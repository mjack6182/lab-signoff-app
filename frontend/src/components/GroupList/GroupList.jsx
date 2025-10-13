import React, { useEffect, useState } from "react";

export default function GroupList() {
  const [groups, setGroups] = useState([]);

useEffect(() => {
  fetch("http://localhost:8080/groups")
    .then((res) => {
      if (!res.ok) throw new Error("Failed to fetch groups");
      return res.json();
    })
    .then((data) => Array.isArray(data) ? setGroups(data) : setGroups([]))
    .catch((err) => {
      console.error("Error fetching groups:", err);
      setGroups([]);
    });
}, []);

  return (
    <main className="gc-grid-wrap">
      <h2 className="gc-page-title">Groups</h2>
      <div className="gc-grid">
        {groups.map((g) => (
          <div key={g.id} className="card">
            <h3>{g.groupId}</h3>
            <p>Status: {g.status}</p>
            <p>Members: {g.members.join(", ")}</p>
          </div>
        ))}
      </div>
    </main>
  );
}
