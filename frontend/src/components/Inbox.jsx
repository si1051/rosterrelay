import { useEffect, useState } from 'react';
import { api } from '../api.js';

const ICON = { PROMOTED_FROM_WAITLIST: '🎉', REMINDER: '⏰', SHIFT_CANCELLED: '⚠️', INVITE: '🙋', SPOT_OPENED: '✨' };

export default function Inbox({ onRead }) {
  const [inbox, setInbox] = useState(null);
  useEffect(() => {
    api.inbox().then((i) => { setInbox(i); if (i.unread > 0) api.markRead().then(onRead).catch(() => {}); });
  }, [onRead]);
  if (!inbox) return <p className="muted">Loading…</p>;
  if (inbox.items.length === 0) return <div className="card empty-state"><h3>No messages</h3></div>;
  return (
    <ul className="inbox list">
      {inbox.items.map((n) => (
        <li key={n.id} className={n.read ? 'card note' : 'card note unread'}>
          <span className="note-icon" aria-hidden>{ICON[n.type]}</span>
          <div><div>{n.message}</div><div className="muted small">{new Date(n.createdAt).toLocaleString()}</div></div>
        </li>
      ))}
    </ul>
  );
}
