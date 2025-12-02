// React + Router
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

// Modals + UI
import SignOffModal from '../../components/SignOffModal';
import GroupManagementModal from '../../components/GroupManagementModal';
import Header from '../../components/Header/Header';

// API + Auth + WebSocket
import { api } from '../../config/api';
import { useAuth } from '../../contexts/AuthContext';
import { websocketService } from '../../services/websocketService';

// Styles
import './checkpoints.css';

/**
 * CheckpointPage (Instructor View)
 *
 * This page shows:
 * - All groups in a lab
 * - Checkpoint progress per group
 * - Ability to sign off or undo checkpoints
 * - Real-time updates through WebSocket
 * - CSV grade exporting for teachers
 *
 * Comments explain logic without being too verbose.
 */

export default function CheckpointPage() {
  // URL params
  const { labId, groupId } = useParams();
  const navigate = useNavigate();

  // Auth helpers
  const { isTeacher } = useAuth();

  // UI + state management
  const [groups, setGroups] = useState([]);
  const [selectedGroupId, setSelectedGroupId] = useState(groupId || null);

  // Modals
  const [showSignOffModal, setShowSignOffModal] = useState(false);
  const [selectedCheckpoint, setSelectedCheckpoint] = useState(null);
  const [signOffNotes, setSignOffNotes] = useState('');
  const [signOffStatus, setSignOffStatus] = useState('pass');
  const [showGroupManagement, setShowGroupManagement] = useState(false);

  // Lab + group data
  const [group, setGroup] = useState(null);
  const [lab, setLab] = useState(null);

  // Load / error states
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // WebSocket status
  const [wsEnabled, setWsEnabled] = useState(false);
  const [wsStatus, setWsStatus] = useState('OFF');

  // Checkpoint state per-group
  const [groupCheckpoints, setGroupCheckpoints] = useState({});

  // Export CSV UI states
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState(null);

  // Teacher permissions
  const canExportGrades = typeof isTeacher === 'function' ? isTeacher() : false;

  // Selected group convenience pointer
  const selectedGroup = groups.find(g => g.id === selectedGroupId);

  // Lab display title
  const currentLab = lab?.courseId || 'Lab';

  // WebSocket display helpers
  const wsDisplayStatus = wsEnabled ? wsStatus : 'OFF';
  const wsStatusClass = !wsEnabled
    ? 'text-gray-600'
    : wsStatus === 'CONNECTED'
    ? 'text-green-600'
    : wsStatus === 'RECONNECTING'
    ? 'text-orange-500'
    : 'text-red-600';

  // -----------------------------------------------------------
  // Helper: make sure checkpoint IDs use the format "cp-1"
  // -----------------------------------------------------------
  const normalizeToCpKey = (checkpointId) => {
    if (!checkpointId) return null;

    const cpMatch = String(checkpointId).match(/^cp-(\d+)$/i);
    if (cpMatch) return `cp-${parseInt(cpMatch[1], 10)}`;

    const numMatch = String(checkpointId).match(/^(\d+)$/);
    if (numMatch) return `cp-${parseInt(numMatch[1], 10)}`;

    const otherMatch = String(checkpointId).match(/(\d+)$/);
    if (otherMatch) return `cp-${parseInt(otherMatch[1], 10)}`;

    return String(checkpointId);
  };

  // Extract checkpoint number for backend calls
  const checkpointIdToNumber = (checkpointId) => {
    if (!checkpointId) return null;
    const match = String(checkpointId).match(/(\d+)$/);
    return match ? parseInt(match[1], 10) : null;
  };

  // Lab checkpoints
  const checkpoints = lab?.checkpoints || [];

  // Convert into consistent format for UI
  const formattedCheckpoints = checkpoints.map(cp => ({
    id: `cp-${cp.number}`,
    name: cp.name,
    description: cp.description,
    points: cp.points,
    order: cp.number,
    number: cp.number
  }));

  // ===================================================================
  // EFFECT 1 — Load lab + groups on initial mount
  // ===================================================================
  useEffect(() => {
    if (!labId) return;

    setLoading(true);

    Promise.all([
      fetch(api.labs()).then(res => res.json()),       // fetch lab list
      fetch(api.labGroups(labId)).then(async res => {  // fetch lab groups
        if (!res.ok) {
          const text = await res.text();
          throw new Error(`Failed to fetch groups: ${res.status} - ${text}`);
        }
        return res.json();
      })
    ])
      .then(async ([allLabs, groupsData]) => {
        // Try to find lab from cached list
        let currentLab = allLabs.find(l => l.id === labId);

        // If missing checkpoint definitions, fetch lab detail
        if (!currentLab || !currentLab.checkpoints?.length) {
          try {
            const detailRes = await fetch(api.labDetail(labId));
            if (detailRes.ok) currentLab = await detailRes.json();
          } catch (e) {
            console.warn('Failed fallback lab detail fetch', e);
          }
        }

        setLab(currentLab);

        const groupsArray = Array.isArray(groupsData) ? groupsData : [];
        setGroups(groupsArray);

        // Choose selected group: URL > first group
        if (groupId) {
          setSelectedGroupId(groupId);
          setGroup(groupsArray.find(g => g.id === groupId));
        } else if (groupsArray.length > 0) {
          setSelectedGroupId(groupsArray[0].id);
        }

        // Build initial checkpoint progress per group
        const initial = {};
        groupsArray.forEach(g => {
          initial[g.id] = {};

          if (Array.isArray(g.checkpointProgress)) {
            g.checkpointProgress.forEach(cp => {
              const cpId = normalizeToCpKey(cp.checkpointNumber);
              const status = (cp.status?.name || cp.status || '').toUpperCase();

              if (status === 'PASS' || status === 'SIGNED_OFF') {
                initial[g.id][cpId] = {
                  completed: true,
                  completedAt: cp.timestamp || new Date().toISOString(),
                  completedBy: cp.signedOffByName || cp.signedOffBy || 'TA'
                };
              }
            });
          }
        });

        setGroupCheckpoints(initial);
      })
      .catch(err => {
        console.error('Error loading data:', err);
        setError(err.message || 'Failed to load lab/group data');
      })
      .finally(() => setLoading(false));
  }, [labId, groupId]);

  // ===================================================================
  // EFFECT 2 — Keep group object updated when selected ID changes
  // ===================================================================
  useEffect(() => {
    if (selectedGroupId && groups.length > 0) {
      setGroup(groups.find(g => g.id === selectedGroupId));
    }
  }, [selectedGroupId, groups]);

  // Reset export error when switching labs
  useEffect(() => {
    setExportError(null);
    setExporting(false);
  }, [labId]);

  // ===================================================================
  // EFFECT 3 — WebSocket setup (opt-in via Start/Stop)
  // ===================================================================
  useEffect(() => {
    const topic = '/topic/group-updates';

    if (!wsEnabled) {
      setWsStatus('OFF');
      return;
    }

    setWsStatus('CONNECTING');
    websocketService.init(); // open connection

    // Track status for UI
    const statusHandler = (status) => {
      console.log(`WebSocket: ${status}`);
      setWsStatus(status);

      if (status === 'CONNECTED') websocketService.subscribe(topic);
    };

    // Handle incoming updates
    const updateHandler = (update) => {
      if (!update || !update.groupId) return;

      // Basic logging
      if (update.checkpointNumber != null) {
        console.log(`WS Update → Group ${update.groupId}, CP ${update.checkpointNumber}: ${update.status}`);
      }

      // Update local state
      setGroupCheckpoints(prev => {
        const next = { ...prev };
        if (!next[update.groupId]) next[update.groupId] = {};

        const status = String(update.status).toUpperCase();

        if (update.checkpointNumber != null) {
          const cpKey = normalizeToCpKey(update.checkpointNumber);

          if (status === 'PASS' || status === 'SIGNED_OFF') {
            next[update.groupId][cpKey] = {
              completed: true,
              completedAt: update.timestamp || new Date().toISOString(),
              completedBy: update.signedOffByName || update.signedOffBy || 'TA'
            };
          }

          if (status === 'RETURN') {
            const { [cpKey]: _removed, ...rest } = next[update.groupId];
            next[update.groupId] = rest;
          }
        }

        // Entire group passed
        if (status === 'GROUP_PASSED') {
          setGroups(gs =>
            gs.map(g => g.id === update.groupId ? { ...g, status: 'passed' } : g)
          );
        }

        return next;
      });
    };

    // Add websocket listeners
    websocketService.addStatusListener(statusHandler);
    websocketService.addListener(updateHandler);

    // Cleanup on unmount or when toggling off
    return () => {
      websocketService.removeListener(updateHandler);
      websocketService.removeStatusListener(statusHandler);
      websocketService.unsubscribe(topic);
      websocketService.disconnect();
    };
  }, [wsEnabled]);

  // -----------------------------------------------------------
  // Open sign-off modal or undo a checkpoint
  // -----------------------------------------------------------
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

  // Undo checkpoint completion
  const handleUndoCheckpoint = async (checkpoint) => {
    if (!selectedGroup || !lab) return;

    const cpNum = checkpointIdToNumber(checkpoint.id);
    if (cpNum == null) return;

    try {
      const res = await fetch(
        api.labGroupCheckpoint(lab.id, selectedGroup.id, 'return'),
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ checkpointNumber: cpNum, performedBy: 'instructor1' })
        }
      );

      if (!res.ok) throw new Error(await res.text());
    } catch (e) {
      console.error('Undo failed:', e);
    }
  };

  // Confirm sign-off or return
  const handleSignOffConfirm = async () => {
    if (!selectedCheckpoint || !selectedGroup || !lab) return;

    const cpNum = checkpointIdToNumber(selectedCheckpoint.id);
    if (cpNum == null) return;

    const endpoint = signOffStatus === 'pass' ? 'pass' : 'return';

    try {
      const response = await fetch(
        api.labGroupCheckpoint(lab.id, selectedGroup.id, endpoint),
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            checkpointNumber: cpNum,
            notes: signOffNotes,
            performedBy: 'instructor1'
          })
        }
      );

      if (!response.ok) throw new Error(await response.text());
    } catch (e) {
      console.error('Backend error:', e);
    } finally {
      // Close modal
      setShowSignOffModal(false);
      setSelectedCheckpoint(null);
      setSignOffNotes('');
    }
  };

  // -----------------------------------------------------------
  // Helper: is a checkpoint complete?
  // -----------------------------------------------------------
  const isCheckpointCompleted = (checkpointId) => {
    const cpKey = normalizeToCpKey(checkpointId);
    return !!groupCheckpoints[selectedGroup?.id]?.[cpKey];
  };

  // Completed checkpoint count per group
  const getCompletedCount = (groupId) => {
    return Object.values(groupCheckpoints[groupId] || {}).filter(cp => cp.completed).length;
  };

  // Open group management modal
  const handleEditGroups = () => setShowGroupManagement(true);

  // Refresh group list after export
  const handleUpdateGroups = async () => {
    try {
      const res = await fetch(api.labGroups(labId));
      if (res.ok) setGroups(await res.json());
    } catch (err) {
      console.error('Failed to reload groups:', err);
    }
  };

  // CSV exporting logic
  const handleExportCsv = async () => {
    if (!labId) return;

    setExportError(null);
    setExporting(true);

    try {
      const res = await fetch(api.labGradesCsv(labId), { credentials: 'include' });
      if (!res.ok) throw new Error(await res.text());

      const blob = await res.blob();
      const url = URL.createObjectURL(blob);

      // download link
      const link = document.createElement('a');
      link.href = url;
      link.download = `lab_${labId}_grades.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      setExportError(e.message || 'Export failed');
    } finally {
      setExporting(false);
    }
  };

  // -----------------------------------------------------------
  // Rendering states
  // -----------------------------------------------------------
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

  // -----------------------------------------------------------
  // MAIN UI
  // -----------------------------------------------------------
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

            {/* lab name */}
            <h1 className="checkpoint-title">{currentLab}</h1>

            {/* group name next to title */}
            {group && <span className="checkpoint-subtitle"> - {group.groupId}</span>}
          </div>

          <div className="checkpoint-actions">
            {/* WebSocket connection state */}
            <span
              className={`font-semibold ${wsStatusClass}`}
            >
              {wsDisplayStatus}
            </span>

            {/* Toggle WebSocket usage */}
            <button
              className="action-btn secondary"
              onClick={() => setWsEnabled(prev => !prev)}
            >
              {wsEnabled ? 'Stop Live Updates' : 'Start Live Updates'}
            </button>

            {/* CSV exporting button (teachers only) */}
            {canExportGrades && (
              <button className="action-btn primary" onClick={handleExportCsv} disabled={exporting}>
                {exporting ? 'Exporting...' : 'Export Grades (CSV)'}
              </button>
            )}

            {/* Manage groups */}
            <button className="action-btn secondary" onClick={handleEditGroups}>
              ✏️ Manage Groups
            </button>
          </div>
        </div>

        {exportError && <div className="export-error-banner">{exportError}</div>}

        {/* ---------- GROUPS PANEL ---------- */}
        <section className="groups-panel">
          <header className="panel-header">
            <h2 className="panel-title">Groups</h2>
            <span className="groups-count">{groups.length} group{groups.length !== 1 ? 's' : ''}</span>
          </header>

          {/* list of groups */}
          <div className="groups-list">
            {groups.map(g => {
              const completed = getCompletedCount(g.id);
              const total = formattedCheckpoints.length;
              const percent = Math.round((completed / total) * 100);
              const isSelected = g.id === selectedGroupId;

              return (
                <div
                  key={g.id}
                  className={`group-card ${isSelected ? 'selected' : ''}`}
                  onClick={() => setSelectedGroupId(g.id)}
                >
                  <div className="group-card-header">
                    <h3 className="group-name">{g.groupId}</h3>
                    <span className={`group-status ${String(g.status || '').toLowerCase()}`}>{g.status}</span>
                  </div>

                  <div className="group-card-body">
                    {/* Member names */}
                    <div className="group-members">
                      <div className="members-list">
                        {g.members?.map((m, index) => (
                          <span key={index} className="member-name">
                            {typeof m === 'string' ? m : m.name || m.email || m.userId || 'Unknown'}
                            {index < g.members.length - 1 ? ', ' : ''}
                          </span>
                        ))}
                      </div>
                      <span className="members-count">
                        {g.members?.length || 0} member{g.members?.length !== 1 ? 's' : ''}
                      </span>
                    </div>

                    {/* Progress bar */}
                    <div className="group-progress">
                      <div className="progress-header">
                        <span className="progress-text">Checkpoints</span>
                        <span className="progress-value">{completed}/{total}</span>
                      </div>
                      <div className="progress-bar">
                        <div className="progress-fill" style={{ width: `${percent}%` }}></div>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        {/* ---------- CHECKPOINTS PANEL ---------- */}
        <section className="checkpoint-panel">
          <header className="panel-header">
            <h2 className="panel-title">Checkpoints</h2>
            <span className="checkpoint-count">
              {selectedGroup ? getCompletedCount(selectedGroup.id) : 0}/{formattedCheckpoints.length}
            </span>
          </header>

          <div className="checkpoint-list">
            {formattedCheckpoints.map((cp, index) => {
              const isComplete = isCheckpointCompleted(cp.id);

              return (
                <div key={cp.id} className={`checkpoint-item ${isComplete ? 'completed' : 'pending'}`}>
                  <div className="checkpoint-indicator">
                    <div className="checkpoint-number">{isComplete ? '✓' : index + 1}</div>
                  </div>

                  <div className="checkpoint-details">
                    <div className="checkpoint-main">
                      <h3 className="checkpoint-name">{cp.name}</h3>
                      <p className="checkpoint-description">{cp.description}</p>
                    </div>

                    <div className="checkpoint-actions">
                      {!isComplete ? (
                        <button className="checkpoint-btn pass" onClick={() => handleSignOffClick(cp, 'pass')}>
                          Sign Off
                        </button>
                      ) : (
                        <button className="checkpoint-btn return" onClick={() => handleSignOffClick(cp, 'return')}>
                          Undo
                        </button>
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
