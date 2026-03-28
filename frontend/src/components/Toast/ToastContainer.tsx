import React from 'react';
import type { Toast } from './ToastContext';
import './Toast.css';

interface Props {
  toasts: Toast[];
  onRemove: (id: string) => void;
}

const ICONS: Record<Toast['type'], string> = {
  success: '✓',
  error: '✕',
  warning: '!',
  info: 'i',
};

export default function ToastContainer({ toasts, onRemove }: Props) {
  if (toasts.length === 0) return null;

  return (
    <div className="toast-container" aria-live="polite">
      {toasts.map(t => (
        <div key={t.id} className={`toast toast--${t.type}`} role="alert">
          <span className={`toast__icon toast__icon--${t.type}`}>{ICONS[t.type]}</span>
          <span className="toast__message">{t.message}</span>
          <button className="toast__close" onClick={() => onRemove(t.id)} aria-label="닫기">×</button>
        </div>
      ))}
    </div>
  );
}
