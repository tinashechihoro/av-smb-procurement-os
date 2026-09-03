import { useEffect, useRef } from 'react';

interface Field {
  label: string;
  value: React.ReactNode;
  full?: boolean;
}

interface Props {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  icon?: string;
  fields: Field[];
  children?: React.ReactNode;
  actions?: React.ReactNode;
}

export default function DetailDrawer({ open, onClose, title, subtitle, icon, fields, children, actions }: Props) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    if (open) document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="modal-wrap" style={{ opacity: 1, pointerEvents: 'auto' }} onClick={onClose}>
      <div
        ref={ref}
        className="modal-card"
        style={{ width: 'min(640px, 96vw)', maxHeight: '90vh' }}
        onClick={e => e.stopPropagation()}
      >
        <div className="modal-head">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {icon && <div className="record-icon" style={{ width: 32, height: 32, fontSize: 14 }}>{icon}</div>}
            <div>
              <div className="eyebrow">{subtitle || 'DETAIL'}</div>
              <h2>{title}</h2>
            </div>
          </div>
          <button className="icon-btn" onClick={onClose} aria-label="Close">×</button>
        </div>
        <div className="modal-body" style={{ maxHeight: 'calc(90vh - 130px)', overflow: 'auto' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '10px 16px' }}>
            {fields.map((f, i) => (
              <div key={i} style={f.full ? { gridColumn: '1 / -1' } : undefined}>
                <div style={{ fontSize: 10, color: '#111', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 3 }}>
                  {f.label}
                </div>
                <div style={{ fontSize: 12, color: '#000', fontWeight: 500 }}>{f.value}</div>
              </div>
            ))}
          </div>
          {children && <div style={{ marginTop: 16 }}>{children}</div>}
          {actions && <div className="modal-actions">{actions}</div>}
        </div>
      </div>
    </div>
  );
}
