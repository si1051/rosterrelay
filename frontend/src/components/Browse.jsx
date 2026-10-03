import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import ShiftCard from './ShiftCard.jsx';

export default function Browse({ notify }) {
  const [shifts, setShifts] = useState(null);
  const [q, setQ] = useState('');
  const load = useCallback(() => api.browse().then(setShifts).catch((e) => notify(e.message, 'error')), [notify]);
  useEffect(() => { load(); }, [load]);

  const signUp = async (s) => {
    try {
      const r = await api.signUp(s.id);
      notify(r.status === 'WAITLISTED' ? `Shift is full. You're #${r.waitlistPosition} on the waitlist and will be moved in automatically.` : "You're confirmed!");
      load();
    } catch (e) { notify(e.message, 'error'); }
  };

  if (!shifts) return <p className="muted">Loading…</p>;
  const shown = shifts.filter((s) => `${s.title} ${s.organization} ${s.location}`.toLowerCase().includes(q.toLowerCase()));
  return (
    <div className="list">
      <input className="search" placeholder="Search shifts, organizations, places…" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search shifts" />
      {shown.length === 0 && <div className="card empty-state"><h3>No upcoming shifts</h3></div>}
      {shown.map((s) => (
        <ShiftCard key={s.id} shift={s}>
          {s.myStatus === 'CONFIRMED' && <span className="pill st-confirmed">You're in</span>}
          {s.myStatus === 'WAITLISTED' && <span className="pill st-waitlisted">Waitlist #{s.myWaitlistPosition}</span>}
          {!['CONFIRMED', 'WAITLISTED'].includes(s.myStatus) && (
            <button className={s.confirmed >= s.capacity ? 'btn ghost' : 'btn primary'} onClick={() => signUp(s)}>
              {s.confirmed >= s.capacity ? 'Join waitlist' : 'Sign up'}
            </button>
          )}
          {s.confirmed < s.capacity && <span className="small muted">{s.capacity - s.confirmed} spots left</span>}
        </ShiftCard>
      ))}
    </div>
  );
}
