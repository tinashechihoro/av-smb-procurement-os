export default function SkipLink() {
  return (
    <a href="#main-content" className="skip-link" style={{
      position: 'absolute', left: '-9999px', top: 'auto', width: '1px', height: '1px', overflow: 'hidden',
      zIndex: 9999, padding: '12px 24px', background: '#0a4fc5', color: '#fff', fontSize: 14,
      fontWeight: 700, textDecoration: 'none', borderRadius: '0 0 8px 0',
    }}
    onFocus={(e) => { e.currentTarget.style.left = '0'; e.currentTarget.style.top = '0'; e.currentTarget.style.width = 'auto'; e.currentTarget.style.height = 'auto'; }}
    onBlur={(e) => { e.currentTarget.style.left = '-9999px'; e.currentTarget.style.top = 'auto'; e.currentTarget.style.width = '1px'; e.currentTarget.style.height = '1px'; }}
    >
      Skip to main content
    </a>
  );
}
