import { useCallback, useEffect, useState } from 'react';
import { api, getToken, setToken, setUnauthorizedHandler } from './api.js';
import AuthPage from './components/AuthPage.jsx';
import Browse from './components/Browse.jsx';
import MyShifts from './components/MyShifts.jsx';
import Hours from './components/Hours.jsx';
import Inbox from './components/Inbox.jsx';
import Dashboard from './components/Dashboard.jsx';
import ManageShifts from './components/ManageShifts.jsx';
import Toasts, { useToasts } from './components/Toasts.jsx';

const TABS = {
  VOLUNTEER: [['browse', 'Find shifts'], ['mine', 'My shifts'], ['hours', 'Hours'], ['inbox', 'Inbox']],
  COORDINATOR: [['dashboard', 'Dashboard'], ['manage', 'Shifts & roster']],
};

export default function App() {
  const [authed, setAuthed] = useState(Boolean(getToken()));
  const [me, setMe] = useState(null);
  const [tab, setTab] = useState(null);
  const [unread, setUnread] = useState(0);
  const toasts = useToasts();
  const notify = toasts.push;

  const logout = useCallback(() => { setToken(null); setAuthed(false); setMe(null); setTab(null); }, []);
  useEffect(() => { setUnauthorizedHandler(logout); }, [logout]);

  const clearUnread = useCallback(() => setUnread(0), []);
  const refreshUnread = useCallback(() => {
    api.inbox().then((i) => setUnread(i.unread)).catch(() => {});
  }, []);

  useEffect(() => {
    if (!authed) return;
    api.me().then((m) => {
      setMe(m);
      setTab((t) => t || TABS[m.role][0][0]);
      if (m.role === 'VOLUNTEER') refreshUnread();
    }).catch((e) => notify(e.message, 'error'));
  }, [authed, notify, refreshUnread]);

  if (!authed) {
    return (<><AuthPage onAuthed={(t) => { setToken(t); setAuthed(true); }} /><Toasts toasts={toasts} /></>);
  }
  if (!me) return <p className="muted pad">Loading…</p>;

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand"><span className="logo" aria-hidden>◆</span> ShiftMate</div>
        <nav className="tabs" aria-label="Sections">
          {TABS[me.role].map(([id, text]) => (
            <button key={id} className={tab === id ? 'tab active' : 'tab'} onClick={() => setTab(id)}>
              {text}{id === 'inbox' && unread > 0 && <span className="badge">{unread}</span>}
            </button>
          ))}
        </nav>
        <div className="topbar-right">
          <span className="muted hide-sm">{me.role === 'COORDINATOR' ? me.organization : me.name}</span>
          <button className="btn ghost" onClick={logout}>Sign out</button>
        </div>
      </header>
      <main className="content">
        {tab === 'browse' && <Browse notify={notify} />}
        {tab === 'mine' && <MyShifts notify={notify} />}
        {tab === 'hours' && <Hours me={me} notify={notify} onRotated={(p) => setMe({ ...me, calendarPath: p })} />}
        {tab === 'inbox' && <Inbox onRead={clearUnread} />}
        {tab === 'dashboard' && <Dashboard notify={notify} />}
        {tab === 'manage' && <ManageShifts notify={notify} />}
      </main>
      <Toasts toasts={toasts} />
    </div>
  );
}
