import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import SignOffModal from '../../components/SignOffModal';
import GroupManagementModal from '../../components/GroupManagementModal';
import Header from '../../components/Header/Header';
import { api } from '../../config/api';
import { websocketService } from '../../services/websocketService';
import './checkpoints.css';

/**
 * CheckpointPage (fixed real-time updates)
 *
 * Key fixes:
 * - WebSocket subscription is established once on mount (stable)
 * - We no longer repeatedly subscribe/unsubscribe when selectedGroup changes
 * - Incoming updates are merged into groupCheckpoints with safe state updates
 * - Prevent duplicate checkpoint entries when optimistic update and WS update both occur
 * - Normalizes checkpoint id formats so UI logic is robust
 * - Status logging is concise and developer friendly
 *
 * Important: UI layout and classes were not changed
 */

export default function CheckpointPage() {
  const { labId, groupId } = useParams();
  const navigate = useNavigate();

  const [groups, setGroups] = useState([]);
  const [selectedGroupId, setSelectedGroupId] = useState(groupId || null);
  const [showSignOffModal, setShowSignOffModal] = useState(false);
  const [selectedCheckpoint, setSelectedCheckpoint] = useState(null);
  const [signOffNotes, setSignOffNotes] = useState('');
  const [signOffStatus, setSignOffStatus] = useState('pass');
  const [showGroupManagement, setShowGroupManagement] = useState(false);
  const [group, setGroup] = useState(null);
  const [lab, setLab] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [wsStatus, setWsStatus] = useState('DISCONNECTED');
  const [groupCheckpoints, setGroupCheckpoints] = useState({});

  const selectedGroup = groups.find(g => g.id === selectedGroupId);
  const currentLab = lab?.courseId || 'Lab';

  // ---------- Helper utilities for checkpoint id normalization ----------
  // Acceptable checkpoint id shapes: "cp-1", "1", "checkpoint-1" etc
  // All internal storage keys use "cp-<num>" format
  const normalizeToCpKey = (checkpointId) => {
    if (checkpointId == null) return null;
    // if already cp-<num>
    const cpMatch = String(checkpointId).match(/^cp-(\d+)$/i);
    if (cpMatch) return `cp-${parseInt(cpMatch[1], 10)}`;

    // if numeric string like "1"
    const numMatch = String(checkpointId).match(/^(\d+)$/);
    if (numMatch) return `cp-${parseInt(numMatch[1], 10)}`;

    // if something like "checkpoint-1" or "checkpoint_1"
    const otherMatch = String(checkpointId).match(/(\d+)$/);
    if (otherMatch) return `cp-${parseInt(otherMatch[1], 10)}`;

    // fallback to the raw id as key
    return String(checkpointId);
  };

  const checkpointIdToNumber = (checkpointId) => {
    if (checkpointId == null) return null;
    const numMatch = String(checkpointId).match(/(\d+)$/);
    return numMatch ? parseInt(numMatch[1], 10) : null;
  };

  // Get checkpoints from lab (stored in MongoDB) or use empty array as fallback
  const checkpoints = lab?.checkpoints || [];

  // Transform checkpoints to match the expected format with IDs
  const formattedCheckpoints = checkpoints.map(cp => ({
    id: `cp-${cp.number}`,
    name: cp.name,
    description: cp.description,
    points: cp.points,
    order: cp.number,
    number: cp.number
  }));

  // ================================================================
  // 🧩 EFFECT 1: Fetch lab + group data on mount
  // ================================================================
  useEffect(() => {
    if (!labId) return;
    setLoading(true);

    Promise.all([
      fetch(api.labs()).then(res => res.json()),
      fetch(api.labGroups(labId)).then(async res => {
        if (!res.ok) {
          const errorText = await res.text();
          throw new Error(`Failed to fetch groups: ${res.status} - ${errorText}`);
        }
        return res.json();
      })
    ])
      .then(([allLabs, groupsData]) => {
        const currentLab = allLabs.find(l => l.id === labId);
        setLab(currentLab);

        const groupsArray = Array.isArray(groupsData) ? groupsData : [];
        setGroups(groupsArray);

        // select group if provided, otherwise pick first
        if (groupId) {
          const foundGroup = groupsArray.find(g => g.id === groupId);
          setGroup(foundGroup);
          setSelectedGroupId(groupId);
        } else if (groupsArray.length > 0) {
          setSelectedGroupId(groupsArray[0].id);
        }

        // initialize checkpoints state for each group
        const initial = {};
        groupsArray.forEach(g => {
          // If backend provides checkpointProgress array, hydrate it
          if (Array.isArray(g.checkpointProgress) && g.checkpointProgress.length > 0) {
            initial[g.id] = {};
            g.checkpointProgress.forEach(cp => {
              const cpNum = cp.checkpointNumber || cp.getCheckpointNumber?.();
              const cpId = normalizeToCpKey(cpNum || cp.checkpointNumber || cp.getCheckpointNumber?.());
              const statusVal = cp.status ? (typeof cp.status === 'string' ? cp.status : cp.status.name || cp.status) : null;
              if (statusVal === 'PASS' || statusVal === 'SIGNED_OFF') {
                initial[g.id][cpId] = {
                  completed: true,
                  completedAt: cp.timestamp || new Date().toISOString(),
                  completedBy: cp.signedOffByName || cp.signedOffBy || 'TA'
                };
              }
            });
          } else {
            initial[g.id] = {};
          }
        });

        setGroupCheckpoints(initial);
      })
      .catch(err => {
        console.error('Error loading lab/groups:', err);
        setError(err.message || String(err));
      })
      .finally(() => setLoading(false));
  }, [labId, groupId]);

  // -------------------------
  // EFFECT 2: Keep selected group in sync when groups list or selection changes
  // -------------------------
  useEffect(() => {
    if (selectedGroupId && groups.length > 0) {
      const found = groups.find(g => g.id === selectedGroupId);
      setGroup(found);
    }
  }, [selectedGroupId, groups]);

  // -------------------------
  // EFFECT 3: WebSocket setup ONCE for app lifecycle
  // - subscribe once to /topic/group-updates
  // - add listener that updates local state
  // - remove listeners on unmount but intentionally keep subscription live while dev server runs
  // -------------------------
  useEffect(() => {
    websocketService.init();

    const topic = '/topic/group-updates';

    const statusHandler = (status) => {
      console.log(`WebSocket: ${status}`);
      setWsStatus(status);
      if (status === 'CONNECTED') {
        websocketService.subscribe(topic);
      }
    };

    const updateHandler = (update) => {
      if (!update || !update.groupId) return;

      if (update.checkpointNumber != null) {
        console.log(`WS: group ${update.groupId} cp ${update.checkpointNumber} -> ${update.status}`);
      } else if (update.status === 'GROUP_PASSED') {
        console.log(`WS: group ${update.groupId} -> SIGNED OFF`);
      } else {
        console.log('WS update:', update);
      }

      setGroupCheckpoints(prev => {
        const next = { ...prev };

        if (!next[update.groupId]) next[update.groupId] = {};

        const cpNum = update.checkpointNumber;
        if (cpNum != null) {
          const cpKey = normalizeToCpKey(cpNum);
          const existing = next[update.groupId][cpKey];

          const normalized = String(update.status).toUpperCase();

          if (normalized === 'PASS' || normalized === 'SIGNED_OFF') {
            if (existing && existing.completed) {
              // already marked completed. still update metadata in case timestamp or name changed
              next[update.groupId] = {
                ...next[update.groupId],
                [cpKey]: {
                  ...existing,
                  completedAt: update.timestamp || existing.completedAt,
                  completedBy: update.signedOffByName || update.signedOffBy || existing.completedBy
                }
              };
            } else {
              next[update.groupId] = {
                ...next[update.groupId],
                [cpKey]: {
                  completed: true,
                  completedAt: update.timestamp || new Date().toISOString(),
                  completedBy: update.signedOffByName || update.signedOffBy || 'TA'
                }
              };
            }
          } else if (normalized === 'RETURN') {
            const { [cpKey]: _removed, ...rest } = next[update.groupId];
            next[update.groupId] = rest;
          }
        }

        if (update.status === 'GROUP_PASSED') {
          setGroups(prevGroups => prevGroups.map(g => g.id === update.groupId ? { ...g, status: 'passed' } : g));
        }

        return next;
      });
    };

    websocketService.addStatusListener(statusHandler);
    websocketService.addListener(updateHandler);

    if (wsStatus === 'CONNECTED') {
      websocketService.subscribe(topic);
    }

    return () => {
      websocketService.removeListener(updateHandler);
      websocketService.removeStatusListener(statusHandler);
    };
    // run once on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // -------------------------
  // UI Handlers
  // -------------------------
  const handleSignOffClick = (checkpoint, status) => {
    if (status === 'return') {
      handleUndoCheckpoint(checkpoint);
    } else {
      setSelectedCheckpoint(checkpoint);
      setSignOffStatus(status);
      setSignOffNotes('');
      setShowSignOffModal(true);
    }
  };

  const handleUndoCheckpoint = async (checkpoint) => {
    if (!selectedGroup || !lab) return;
    const checkpointNum = checkpointIdToNumber(checkpoint.id);

    if (checkpointNum == null) {
      console.warn('Cannot parse checkpoint number for undo', checkpoint);
      return;
    }

    try {
      const response = await fetch(
        `http://localhost:8080/labs/${lab.id}/groups/${selectedGroup.id}/return`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            checkpointNumber: checkpointNum,
            performedBy: 'instructor1'
          })
        }
      );
      if (!response.ok) {
        const txt = await response.text();
        throw new Error(txt || 'Failed');
      }
      // rely on backend broadcast to update local state
    } catch (e) {
      console.error('Failed undoing checkpoint:', e);
    }
  };

  const handleSignOffConfirm = async () => {
    if (!selectedCheckpoint || !selectedGroup || !lab) return;

    const isPassing = signOffStatus === 'pass';
    const checkpointNum = checkpointIdToNumber(selectedCheckpoint.id);
    if (checkpointNum == null) {
      console.warn('Cannot parse checkpoint number for sign off', selectedCheckpoint);
      setShowSignOffModal(false);
      return;
    }

    try {
      const endpoint = isPassing ? 'pass' : 'return';
      const response = await fetch(
        `http://localhost:8080/labs/${lab.id}/groups/${selectedGroup.id}/${endpoint}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            checkpointNumber: checkpointNum,
            notes: signOffNotes,
            performedBy: 'instructor1'
          })
        }
      );

      if (!response.ok) {
        const txt = await response.text();
        throw new Error(txt || 'Failed to update checkpoint');
      }

      // rely on backend broadcast to update local state
    } catch (e) {
      console.error('Error calling backend API:', e);
    } finally {
      setShowSignOffModal(false);
      setSelectedCheckpoint(null);
      setSignOffNotes('');
    }
  };

  // -------------------------
  // helpers
  // -------------------------
  const isCheckpointCompleted = (checkpointId) => {
    if (!selectedGroup) return false;
    const cpKey = normalizeToCpKey(checkpointId);
    return !!groupCheckpoints[selectedGroup.id]?.[cpKey]?.completed;
  };

  const getCompletedCount = (groupId) => {
    if (!groupCheckpoints[groupId]) return 0;
    return Object.values(groupCheckpoints[groupId]).filter(cp => cp.completed).length;
  };

  const handleEditGroups = () => setShowGroupManagement(true);

  const handleUpdateGroups = async () => {
    // Refetch groups after update
    try {
      const response = await fetch(api.labGroups(labId));
      if (response.ok) {
        const groupsData = await response.json();
        const groupsArray = Array.isArray(groupsData) ? groupsData : [];
        setGroups(groupsArray);
        console.log('Groups refreshed successfully!', groupsArray);
      }
    } catch (err) {
      console.error('Failed to refresh groups:', err);
    }
  };

  // -------------------------
  // render
  // -------------------------
  console.log('🌀 Rendering UI with groupCheckpoints:', groupCheckpoints);

  if (loading)
    return (
      <>
        <Header />
        <main className="checkpoint-shell">
          <div className="center-message">Loading...</div>
        </main>
      </>
    );

  if (error)
    return (
      <>
        <Header />
        <main className="checkpoint-shell">
          <div className="error-message">
            <div>Error loading data: {error}</div>
            <button onClick={() => navigate('/class-selector')}>Back to Classes</button>
          </div>
        </main>
      </>
    );

  return (
    <>
      <Header />
      <main className="checkpoint-shell">
        {/* ---------- Page Header ---------- */}
        <div className="checkpoint-page-header">
          <div className="checkpoint-nav">
            <button className="breadcrumb-back" onClick={() => navigate('/class-selector')}>
              <span className="back-arrow">←</span>
              <span>Back to Classes</span>
            </button>
            <span className="breadcrumb-separator">/</span>
            <h1 className="checkpoint-title">{currentLab}</h1>
            {group && <span className="checkpoint-subtitle"> - {group.groupId}</span>}
          </div>

          <div className="checkpoint-actions">
            <span
              className={`font-semibold ${
                wsStatus === 'CONNECTED' ? 'text-green-600' :
                wsStatus === 'RECONNECTING' ? 'text-orange-500' :
                'text-red-600'
              }`}
            >
              {wsStatus}
            </span>
            <button className="action-btn secondary" onClick={handleEditGroups}>✏️ Manage Groups</button>
          </div>
        </div>

        {/* ---------- Groups Panel ---------- */}
        <section className="groups-panel">
          <header className="panel-header">
            <h2 className="panel-title">Groups</h2>
            <span className="groups-count">{groups.length} group{groups.length !== 1 ? 's' : ''}</span>
          </header>
          <div className="groups-list">
            {groups.map(g => {
              const completedCount = getCompletedCount(g.id);
              const totalCount = formattedCheckpoints.length;
              const progressPercent = Math.round((completedCount / totalCount) * 100);
              const isSelected = g.id === selectedGroupId;

              return (
                <div key={g.id} className={`group-card ${isSelected ? 'selected' : ''}`} onClick={() => setSelectedGroupId(g.id)}>
                  <div className="group-card-header">
                    <h3 className="group-name">{g.groupId}</h3>
                    <span className={`group-status ${String(g.status || '').toLowerCase().replace(' ', '-')}`}>{g.status}</span>
                  </div>
                  <div className="group-card-body">
                    <div className="group-members">
                      <div className="members-list">
                        {g.members && g.members.map((member, index) => (
                          <span key={index} className="member-name">
                            {typeof member === 'string' ? member : (member.name || member.email || member.userId || 'Unknown')}{index < g.members.length - 1 ? ', ' : ''}
                          </span>
                        ))}
                      </div>
                      <span className="members-count">{g.members?.length || 0} member{g.members?.length !== 1 ? 's' : ''}</span>
                    </div>
                    <div className="group-progress">
                      <div className="progress-header">
                        <span className="progress-text">Checkpoints</span>
                        <span className="progress-value">{completedCount}/{totalCount}</span>
                      </div>
                      <div className="progress-bar">
                        <div className="progress-fill" style={{ width: `${progressPercent}%` }}></div>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        {/* ---------- Checkpoints Panel ---------- */}
        <section className="checkpoint-panel">
          <header className="panel-header">
            <h2 className="panel-title">Checkpoints</h2>
            <span className="checkpoint-count">
              {selectedGroup ? getCompletedCount(selectedGroup.id) : 0}/{formattedCheckpoints.length}
            </span>
          </header>
          <div className="checkpoint-list">
            {formattedCheckpoints.map((checkpoint, index) => {
              const isCompleted = isCheckpointCompleted(checkpoint.id);
              return (
                <div key={checkpoint.id} className={`checkpoint-item ${isCompleted ? 'completed' : 'pending'}`}>
                  <div className="checkpoint-indicator">
                    <div className="checkpoint-number">{isCompleted ? '✓' : index + 1}</div>
                  </div>
                  <div className="checkpoint-details">
                    <div className="checkpoint-main">
                      <h3 className="checkpoint-name">{checkpoint.name}</h3>
                      <p className="checkpoint-description">{checkpoint.description}</p>
                    </div>
                    <div className="checkpoint-actions">
                      {!isCompleted ? (
                        <button className="checkpoint-btn pass" onClick={() => handleSignOffClick(checkpoint, 'pass')}>Sign Off</button>
                      ) : (
                        <button className="checkpoint-btn return" onClick={() => handleSignOffClick(checkpoint, 'return')}>Undo</button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        {/* ---------- Modals ---------- */}
        <SignOffModal
          isOpen={showSignOffModal}
          onClose={() => setShowSignOffModal(false)}
          selectedCheckpoint={selectedCheckpoint}
          signOffStatus={signOffStatus}
          signOffNotes={signOffNotes}
          setSignOffNotes={setSignOffNotes}
          onConfirm={handleSignOffConfirm}
        />

        <GroupManagementModal
          isOpen={showGroupManagement}
          onClose={() => setShowGroupManagement(false)}
          labId={labId}
          classId={lab?.classId}
          labName={lab?.courseId || lab?.name || 'Lab'}
          onUpdateGroups={handleUpdateGroups}
        />
      </main>
    </>
  );
}
