import { useCallback, useEffect, useState } from 'react';
import { api, day, time } from '../api.js';

function Stat({ value, label, hint, tone }) {
  return <div className="card stat"><div className={`stat-value ${tone || ''}`}>{value}</div><div className="stat-label">{label}</div>{hint && <div className="muted small">{hint}</div>}</div>;
}

export default function Dashboard({ notify }) {
  const [d, setD] = useState(null);
  const [invited, setInvited] = useState({});
  const load = useCallback(() => api.dashboard().then(setD).catch((e) => notify(e.message, 'error')), [notify]);
  useEffect(() => { load(); }, [load]);

  const invite = async (shiftId, v) => {
    try { await api.invite(shiftId, v.volunteerId); setInvited({ ...invited, [`${shiftId}-${v.volunteerId}`]: true }); notify(`Invited ${v.name}`); } catch (e) { notify(e.message, 'error'); }
  };

  if (!d) return <p className="muted">Loading…</p>;
  return (
    <div className="analytics">
      <div className="stats">
        <Stat value={d.upcomingShifts} label="Upcoming shifts" />
        <Stat value={d.upcomingFillRate == null ? '—' : `${d.upcomingFillRate}%`} label="Seats filled" tone={d.upcomingFillRate >= 80 ? 'good' : d.upcomingFillRate < 50 ? 'bad' : ''} />
        <Stat value={d.noShowRate == null ? '—' : `${d.noShowRate}%`} label="No-show rate" hint="of confirmed volunteers" tone={d.noShowRate > 15 ? 'bad' : 'good'} />
        <Stat value={d.lateCancelRate == null ? '—' : `${d.lateCancelRate}%`} label="Late cancellations" hint="within 24 h of start" />
      </div>
      <div className="stats">
        <Stat value={d.volunteerHours} label="Volunteer hours" hint="credited after check-in" />
        <Stat value={d.uniqueVolunteers} label="Volunteers" />
        <Stat value={d.waitlistPromotions} label="Auto-filled from waitlist" hint="spots rescued without a phone call" />
      </div>
      <section className="card">
        <h3>Shifts at risk in the next 72 hours</h3>
        {d.atRisk.length === 0 && <p className="muted">Every shift in the next 3 days is at least 60% staffed. 🎉</p>}
        {d.atRisk.map((r) => (
          <div key={r.shiftId} className="risk">
            <div className="item-head">
              <div><strong>{r.title}</strong><div className="muted small">{day(r.startsAt)} · {time(r.startsAt)}</div></div>
              <div className="risk-gap"><strong className="bad">{r.gap}</strong><small>needed</small></div>
            </div>
            <div className="fillbar"><div className="track"><div className="fill low" style={{ width: `${r.fillPercent}%` }} /></div>
              <span className="small muted">{r.confirmed}/{r.capacity} confirmed</span></div>
            {r.suggestions.length > 0 && (
              <div className="suggest">
                <span className="small muted">Free and reliable past volunteers:</span>
                {r.suggestions.map((v) => (
                  <span key={v.volunteerId} className="chip">
                    {v.name} <small>{v.reliability}% · {v.shiftsWithYou} shifts</small>
                    <button className="btn link" disabled={invited[`${r.shiftId}-${v.volunteerId}`]} onClick={() => invite(r.shiftId, v)}>
                      {invited[`${r.shiftId}-${v.volunteerId}`] ? 'Invited' : 'Invite'}
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
      </section>
    </div>
  );
}
