import React, { useEffect, useState } from 'react';
import { websocketService, CheckpointUpdate } from '../../services/websocketService';

type Checkpoint = {
  checkpointNumber: number;
  status: 'PENDING' | 'PASS' | 'RETURN';
};

export default function Checkpoints({ groupId }: { groupId: string }) {
  const [checkpoints, setCheckpoints] = useState<Checkpoint[]>([]);

  // 1️⃣ Load initial checkpoint data (REST)
  useEffect(() => {
    fetch(`/api/groups/${groupId}/checkpoints`)
      .then((r) => r.json())
      .then((data) => {
        setCheckpoints(data.checkpoints || []);
      })
      .catch((err) => console.error('Failed loading checkpoints', err));
  }, [groupId]);

  // 2️⃣ Listen for WebSocket updates
  useEffect(() => {
    const onUpdate = (update: CheckpointUpdate) => {
      setCheckpoints((prev) =>
        prev.map((cp) =>
          cp.checkpointNumber === update.checkpointNumber
            ? { ...cp, status: update.status }
            : cp
        )
      );
    };

    // connect + subscribe
    websocketService.init();
    websocketService.addListener(onUpdate);
    websocketService.subscribeToGroup(groupId);

    // cleanup
    return () => {
      websocketService.removeListener(onUpdate);
      websocketService.unsubscribeFromGroup(groupId);
    };
  }, [groupId]);

  return (
    <div className="p-6">
      <h2 className="text-xl font-bold mb-4">
        Group {groupId} – Checkpoints
      </h2>

      <ul className="space-y-2">
        {checkpoints.map((cp) => (
          <li key={cp.checkpointNumber} className="flex items-center gap-2">
            <span className="font-semibold">
              #{cp.checkpointNumber}:
            </span>
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