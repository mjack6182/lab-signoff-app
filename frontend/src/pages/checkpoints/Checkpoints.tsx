import React, { useEffect, useState } from 'react';
import { websocketService, CheckpointUpdate } from '../../services/websocketService';

type Checkpoint = {
  checkpointNumber: number;
  status: 'PENDING' | 'PASS' | 'RETURN';
};

interface CheckpointsProps {
  groupId: string;
  initialCheckpoints?: Checkpoint[];
}

export default function Checkpoints({ groupId = 'Group-1', initialCheckpoints = [] }: CheckpointsProps) {
  console.log("✅ Checkpoints component is running") 
  const [checkpoints, setCheckpoints] = useState<Checkpoint[]>(initialCheckpoints);
  const [wsStatus, setWsStatus] = useState<'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED'>('DISCONNECTED');

  // 1️⃣ Load initial checkpoint data via REST
  useEffect(() => {
    fetch(`/api/groups/${groupId}/checkpoints`)
      .then((r) => r.json())
      .then((data) => setCheckpoints(data.checkpoints || []))
      .catch((err) => console.error('Failed loading checkpoints', err));
  }, [groupId]);

  // 2️⃣ WebSocket subscription + live updates
  useEffect(() => {
    const onUpdate = (update: CheckpointUpdate) => {
  console.log("📡 WebSocket update received:", update) // add this to see updates
  // REMOVE this line for now: if (update.groupId !== groupId) return;

  setCheckpoints((prev) => {
    const index = prev.findIndex(cp => cp.checkpointNumber === update.checkpointNumber);
    if (index !== -1) {
      const updated = [...prev];
      updated[index] = { ...updated[index], status: update.status };
      return updated;
    } else {
      return [...prev, { checkpointNumber: update.checkpointNumber, status: update.status }];
    }
  })
}

    const onStatusChange = (status: 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED') => {
      setWsStatus(status);
    };

    websocketService.init();
    websocketService.addListener(onUpdate);
    websocketService.subscribeToGroup(groupId);
    websocketService.addStatusListener?.(onStatusChange);

    return () => {
      websocketService.removeListener(onUpdate);
      websocketService.unsubscribeFromGroup(groupId);
      websocketService.removeStatusListener?.(onStatusChange);
    };
  }, [groupId]);

  // Color for connection status
  const statusColor =
    wsStatus === 'CONNECTED' ? 'text-green-600' :
    wsStatus === 'RECONNECTING' ? 'text-orange-500' :
    'text-red-600';

  // 3️⃣ UI
  return (
    <div className="p-6">
      {/* Header with Test Broadcast */}
      <div className="flex justify-between items-center mb-4 flex-wrap gap-4">
        <div className="flex items-center gap-4">
          <h2 className="text-xl font-bold flex-shrink-0">Group {groupId} – Checkpoints</h2>
          <span className={`font-semibold flex-shrink-0 ${statusColor}`}>{wsStatus}</span>
        </div>
        <button
          onClick={() => fetch('http://localhost:8080/ws-test-broadcast').catch(console.error)}
          className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 flex-shrink-0"
        >
          Test Broadcast
        </button>
      </div>

      {/* Checkpoint List */}
      <ul className="space-y-2">
        {checkpoints
          .sort((a, b) => a.checkpointNumber - b.checkpointNumber)
          .map((cp) => (
            <li key={cp.checkpointNumber} className="flex items-center gap-2">
              <span className="font-semibold">#{cp.checkpointNumber}:</span>
              <span
                className={`${
                  cp.status === 'PASS'
                    ? 'text-green-600'
                    : cp.status === 'RETURN'
                    ? 'text-red-600'
                    : 'text-gray-500'
                }`}
              >
                {cp.status}
              </span>
            </li>
          ))}
      </ul>
    </div>
  );
}