// Simple, reusable card to display a group
export default function GroupCard({
                                      name,
                                      course,
                                      section,
                                      members = [],
                                      status = 'Active',
                                      updatedAt,
                                      onOpen,
                                  }) {
    const initials = (full) =>
        full.split(' ').map(p => p[0]?.toUpperCase()).slice(0,2).join('');

    return (
        <article className="gc-card" aria-label={`Group ${name}`}>
            <header className="gc-header">
                <h3 className="gc-title">{name}</h3>
                <span className={`gc-status ${status.toLowerCase()}`}>{status}</span>
            </header>

            <div className="gc-meta">
                <div className="gc-line"><strong>Course:</strong> {course}</div>
                {section && <div className="gc-line"><strong>Section:</strong> {section}</div>}
                {updatedAt && (
                    <div className="gc-line subtle">
                        Updated {new Date(updatedAt).toLocaleDateString()}
                    </div>
                )}
            </div>

            <div className="gc-members" aria-label="Members">
                {members.slice(0,5).map((m) => (
                    <div key={m.id} className="gc-avatar" title={`${m.name} • ${m.role}`}>
                        {m.avatarUrl
                            ? <img src={m.avatarUrl} alt={m.name} />
                            : <span aria-hidden>{initials(m.name)}</span>}
                    </div>
                ))}
                {members.length > 5 && (
                    <div className="gc-avatar more" title={`${members.length - 5} more`}>
                        +{members.length - 5}
                    </div>
                )}
            </div>

            <footer className="gc-actions">
                <button className="gc-btn" onClick={onOpen}>Open</button>
                <button className="gc-btn ghost">Settings</button>
            </footer>
        </article>
    );
}
