import { useCallback, useMemo, useState } from 'react';

export function useToasts() {
  const [items, setItems] = useState([]);
  const push = useCallback((message, kind = 'info') => {
    const id = Math.random().toString(36).slice(2);
    setItems((xs) => [...xs, { id, message, kind }]);
    setTimeout(() => setItems((xs) => xs.filter((x) => x.id !== id)), 4000);
  }, []);
  return useMemo(() => ({ items, push }), [items, push]);
}

export default function Toasts({ toasts }) {
  return (
    <div className="toasts" role="status" aria-live="polite">
      {toasts.items.map((t) => (
        <div key={t.id} className={`toast ${t.kind}`}>{t.message}</div>
      ))}
    </div>
  );
}
