import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { mockCheckpoints } from '../../mock/checkpoints';
import SignOffModal from '../../components/SignOffModal';
import GroupManagementModal from '../../components/GroupManagementModal';
import Header from '../../components/Header/Header';
import { api } from '../../config/api';
import { websocketService } from '../../services/websocketService';
import './checkpoints.css';

export default function CheckpointPage() {
  // ================================================================
  // 🔧 ROUTER + STATE HOOKS
  // ================================================================
  const { labId, groupId } = useParams();
  const navigate = useNavigate();

  // Core app states
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

  // ================================================================
  // 🧩 EFFECT 1: Fetch lab + group data on mount
  // ================================================================
  useEffect(() => {
    if (!labId) return;
    setLoading(true);

    // Fetch all labs and the groups for the current lab
    Promise.all([
      fetch(api.labs()).then(res => res.json()),
      fetch(api.labGroups(labId)).then(res => {
        if (!res.ok) throw new Error('Failed to fetch groups');
        return res.json();
      })
    ])
      .then(([allLabs, groupsData]) => {
        // Match labId to its data
        const currentLab = allLabs.find(l => l.id === labId);
        setLab(currentLab);

        const groupsArray = Array.isArray(groupsData) ? groupsData : [];
        setGroups(groupsArray);

        // Pre-select group if one is in URL
        if (groupId) {
          const foundGroup = groupsArray.find(g => g.id === groupId);
          setGroup(foundGroup);
          setSelectedGroupId(groupId);
        } else if (groupsArray.length > 0) {
          setSelectedGroupId(groupsArray[0].id);
        }

        // Initialize empty checkpoints for each group
        const initial = {};
        groupsArray.forEach(group => {
          initial[group.id] = {};
        });
        setGroupCheckpoints(initial);
      })
      .catch(err => {
        console.error('Error fetching data:', err);
        setError(err.message);
      })
      .finally(() => setLoading(false));
  }, [labId, groupId]);

  // ================================================================
  // 🧭 EFFECT 2: Keep selected group in sync when list changes
  // ================================================================
  useEffect(() => {
    if (selectedGroupId && groups.length > 0) {
      const foundGroup = groups.find(g => g.id === selectedGroupId);
      setGroup(foundGroup);
    }
  }, [selectedGroupId, groups]);

  // ================================================================
  // 🔌 EFFECT 3: WebSocket setup — real-time updates between tabs
  // ================================================================
  useEffect(() => {
    let isActive = true;
    websocketService.init(); // Initialize WebSocket client

    // Track connection status (Connected / Disconnected / Reconnecting)
    const handleStatusChange = (status) => {
      console.log('[WebSocket STATUS]', status);
      setWsStatus(status);

      // Subscribe to updates only when connected
      if (status === 'CONNECTED' && isActive) {
        console.log('[WebSocket] Subscribing to group:', selectedGroupId || 'Group-1');
        websocketService.subscribeToGroup(selectedGroupId || 'Group-1');
      }
    };

    // Handle incoming WebSocket messages from backend
    const handleUpdate = (update) => {
      console.log('📡 WebSocket update received:', update);

      // 🎓 Case 1: Group fully passed → Update status for all tabs
      if (update.status === 'GROUP_PASSED') {
        console.log('🎓 Group fully passed! Updating status in all tabs.');
        setGroups(prev =>
          prev.map(g =>
            g.groupId === update.groupId ? { ...g, status: 'passed' } : g
          )
        );
        return;
      }

      // ✅ Case 2: Individual checkpoint updates (PASS / RETURN)
      if (update.checkpointNumber !== undefined) {
        setGroupCheckpoints(prev => {
          const updated = { ...prev };
          const gid = update.groupId;
          const checkpointNum = update.checkpointNumber;
          const status = update.status;

          if (!updated[gid]) updated[gid] = {};
          const checkpointId = `cp-${checkpointNum}`;

          console.log('✅ Backend checkpoint number', checkpointNum, '→ Frontend ID:', checkpointId);

          if (status === 'PASS') {
            updated[gid][checkpointId] = {
              completed: true,
              completedAt: new Date().toISOString().split('T')[0],
              completedBy: 'instructor',
            };
          } else if (status === 'RETURN') {
            delete updated[gid][checkpointId];
          }

          return updated;
        });
      }
    };

    // Register WebSocket listeners
    websocketService.addListener(handleUpdate);
    if (websocketService.addStatusListener) {
      websocketService.addStatusListener(handleStatusChange);
    } else {
      websocketService.subscribeToGroup(selectedGroupId || 'Group-1');
      setWsStatus('CONNECTED');
    }

    // Cleanup when leaving the page
    return () => {
      isActive = false;
      websocketService.removeListener(handleUpdate);
      if (websocketService.removeStatusListener) {
        websocketService.removeStatusListener(handleStatusChange);
      }
      websocketService.unsubscribeFromGroup(selectedGroupId || 'Group-1');
    };
  }, [selectedGroupId]);

  // ================================================================
  // 🧠 UI HANDLERS
  // ================================================================

  // Opens modal to pass/return a checkpoint
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

  // Undo a checkpoint (mark as RETURNED)
  const handleUndoCheckpoint = async (checkpoint) => {
    if (!selectedGroup) return;
    const checkpointNum = parseInt(checkpoint.id.split('-')[1]);
    console.log('🔍 Undoing checkpoint:', checkpoint.id, '→ Number:', checkpointNum);

    // Optimistic UI update (instant feedback)
    setGroupCheckpoints(prev => {
      const updated = { ...prev };
      if (updated[selectedGroup.id]) {
        delete updated[selectedGroup.id][checkpoint.id];
      }
      return updated;
    });

    // Notify backend to broadcast RETURN
    try {
      await fetch(`http://localhost:8080/groups/${selectedGroup.id}/return`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ checkpointNumber: checkpointNum })
      });
      console.log(`✅ Undo checkpoint ${checkpointNum} - backend notified`);
    } catch (e) {
      console.error('❌ Error undoing checkpoint:', e);
    }
  };

  // Confirm Sign Off (PASS a checkpoint)
  const handleSignOffConfirm = async () => {
    if (!selectedCheckpoint || !selectedGroup) return;
    const isPassing = signOffStatus === 'pass';
    const checkpointNum = parseInt(selectedCheckpoint.id.split('-')[1]);

    console.log('🔍 Checkpoint:', selectedCheckpoint.id, '→ Number:', checkpointNum);

    // Optimistic local update
    const next = { ...groupCheckpoints };
    if (!next[selectedGroup.id]) next[selectedGroup.id] = {};

    if (isPassing) {
      next[selectedGroup.id][selectedCheckpoint.id] = {
        completed: true,
        completedAt: new Date().toISOString().split('T')[0],
        completedBy: 'instructor',
        notes: signOffNotes
      };
    } else {
      delete next[selectedGroup.id][selectedCheckpoint.id];
    }
    setGroupCheckpoints(next);

    try {
      // Update backend (triggers WebSocket broadcast)
      const endpoint = isPassing ? 'pass' : 'return';
      const response = await fetch(`http://localhost:8080/groups/${selectedGroup.id}/${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ checkpointNumber: checkpointNum, notes: signOffNotes })
      });

      if (!response.ok) throw new Error('Failed to update checkpoint');
      console.log(`✅ Backend API called for checkpoint ${checkpointNum}`);

      // 🎯 If all checkpoints are completed → mark full group as passed
      const allCompleted = mockCheckpoints.every(
        cp => next[selectedGroup.id]?.[cp.id]?.completed
      );

      if (isPassing && allCompleted) {
        try {
          const res = await fetch(`http://localhost:8080/lti/labs/${labId}/groups/${selectedGroup.groupId}/pass`, {
            method: 'POST'
          });

          if (res.ok) {
            setGroups(prev =>
              prev.map(g => g.id === selectedGroup.id ? { ...g, status: 'passed' } : g)
            );
            console.log('🎉 All checkpoints complete - Group marked as PASSED!');
          }
        } catch (e) {
          console.error('Error marking group as passed:', e);
        }
      }
    } catch (e) {
      console.error('❌ Error calling backend API:', e);
    }

    // Close modal
    setShowSignOffModal(false);
    setSelectedCheckpoint(null);
    setSignOffNotes('');
  };

  // ================================================================
  // 🧩 Helper functions
  // ================================================================
  const isCheckpointCompleted = (checkpointId) => {
    if (!selectedGroup) return false;
    return groupCheckpoints[selectedGroup.id]?.[checkpointId]?.completed || false;
  };

  const getCompletedCount = (groupId) => {
    if (!groupCheckpoints[groupId]) return 0;
    return Object.values(groupCheckpoints[groupId]).filter(cp => cp.completed).length;
  };

  const handleEditGroups = () => setShowGroupManagement(true);
  const handleUpdateGroups = (updatedGroups) => {
    setGroups(updatedGroups);
    console.log('Groups updated successfully!', updatedGroups);
  };

  // ================================================================
  // 🌀 UI STATES: Loading / Error
  // ================================================================
  if (loading) {
    return (
      <>
        <Header />
        <main className="checkpoint-shell">
          <div className="center-message">Loading...</div>
        </main>
      </>
    );
  }

  if (error) {
    return (
      <>
        <Header />
        <main className="checkpoint-shell">
          <div className="error-message">
            <div>Error loading data: {error}</div>
            <button onClick={() => navigate('/lab-selector')}>Back to Labs</button>
          </div>
        </main>
      </>
    );
  }

  // ================================================================
  // 🎨 MAIN RENDER — groups and checkpoints
  // ================================================================
  return (
    <>
      <Header />
      <main className="checkpoint-shell">
        {/* ---------- Page Header ---------- */}
        <div className="checkpoint-page-header">
          <div className="checkpoint-nav">
            <button className="breadcrumb-back" onClick={() => navigate('/lab-selector')}>
              <span className="back-arrow">←</span>
              <span>Back to Labs</span>
            </button>
            <span className="breadcrumb-separator">/</span>
            <h1 className="checkpoint-title">{currentLab}</h1>
            {group && <span className="checkpoint-subtitle"> - {group.groupId}</span>}
          </div>

          {/* Connection + Management */}
          <div className="checkpoint-actions">
            <span
              className={`font-semibold ${
                wsStatus === 'CONNECTED'
                  ? 'text-green-600'
                  : wsStatus === 'RECONNECTING'
                  ? 'text-orange-500'
                  : 'text-red-600'
              }`}
            >
              {wsStatus}
            </span>
            <button className="action-btn secondary" onClick={handleEditGroups}>
              ✏️ Manage Groups
            </button>
          </div>
        </div>

        {/* ---------- Groups Panel ---------- */}
        <section className="groups-panel">
          <header className="panel-header">
            <h2 className="panel-title">Groups</h2>
            <span className="groups-count">
              {groups.length} group{groups.length !== 1 ? 's' : ''}
            </span>
          </header>

          <div className="groups-list">
            {groups.map(group => {
              const completedCount = getCompletedCount(group.id);
              const totalCount = mockCheckpoints.length;
              const progressPercent = Math.round((completedCount / totalCount) * 100);
              const isSelected = group.id === selectedGroupId;

              return (
                <div
                  key={group.id}
                  className={`group-card ${isSelected ? 'selected' : ''}`}
                  onClick={() => setSelectedGroupId(group.id)}
                >
                  <div className="group-card-header">
                    <h3 className="group-name">{group.groupId}</h3>
                    <span className={`group-status ${group.status.toLowerCase().replace(' ', '-')}`}>
                      {group.status}
                    </span>
                  </div>

                  <div className="group-card-body">
                    <div className="group-members">
                      <div className="members-list">
                        {group.members.map((member, index) => (
                          <span key={index} className="member-name">
                            {member}{index < group.members.length - 1 ? ', ' : ''}
                          </span>
                        ))}
                      </div>
                      <span className="members-count">
                        {group.members.length} member{group.members.length !== 1 ? 's' : ''}
                      </span>
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
              {selectedGroup ? getCompletedCount(selectedGroup.id) : 0}/{mockCheckpoints.length}
            </span>
          </header>

          <div className="checkpoint-list">
            {mockCheckpoints.map((checkpoint, index) => {
              const isCompleted = isCheckpointCompleted(checkpoint.id);
              return (
                <div key={checkpoint.id} className={`checkpoint-item ${isCompleted ? 'completed' : 'pending'}`}>
                  <div className="checkpoint-indicator">
                    <div className="checkpoint-number">
                      {isCompleted ? '✓' : index + 1}
                    </div>
                  </div>

                  <div className="checkpoint-details">
                    <div className="checkpoint-main">
                      <h3 className="checkpoint-name">{checkpoint.name}</h3>
                      <p className="checkpoint-description">{checkpoint.description}</p>
                    </div>

                    <div className="checkpoint-actions">
                      {!isCompleted ? (
                        <button className="checkpoint-btn pass" onClick={() => handleSignOffClick(checkpoint, 'pass')}>
                          Sign Off
                        </button>
                      ) : (
                        <button className="checkpoint-btn return" onClick={() => handleSignOffClick(checkpoint, 'return')}>
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
          groupsData={groups}
          labId={labId}
          onUpdateGroups={handleUpdateGroups}
        />
      </main>
    </>
  );
}