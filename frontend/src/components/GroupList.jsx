import GroupCard from './GroupCard';
import { groups } from '../mock/groups';

export default function GroupList() {
    function openGroup(id) {
        // replace with router navigate or Canvas LTI deep link
        alert(`Open group ${id}`);
    }

    return (
        <main className="gc-grid-wrap">
            <h2 className="gc-page-title">Groups</h2>

            <div className="gc-grid">
                {groups.map((g) => (
                    <GroupCard
                        key={g.id}
                        {...g}
                        onOpen={() => openGroup(g.id)}
                    />
                ))}
            </div>
        </main>
    );
}
