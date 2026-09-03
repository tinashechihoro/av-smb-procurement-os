import { useState, useEffect } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

const avNav = [
  { to: '/', icon: '⌂', label: 'Command Centre', end: true },
  { to: '/vehicles', icon: '◇', label: 'Vehicles & Jobs' },
  { to: '/repair-jobs', icon: '◈', label: 'Repair Jobs' },
  { to: '/requisitions', icon: '≡', label: 'Requisitions' },
  { to: '/quotations', icon: '◫', label: 'Quotations' },
  { to: '/orders', icon: '▣', label: 'Orders' },
  { to: '/goods-receipts', icon: '✓', label: 'Goods Receipt' },
  { to: '/invoices', icon: '▤', label: 'Invoices' },
  { to: '/reports', icon: '⌁', label: 'Reports' },
  { to: '/audit-trail', icon: '◎', label: 'Audit Trail' },
];

const smbNav = [
  { to: '/', icon: '⌂', label: 'Command Centre', end: true },
  { to: '/requisitions', icon: '◈', label: 'AV Requisitions' },
  { to: '/quotations', icon: '◫', label: 'Quotations' },
  { to: '/orders', icon: '⇄', label: 'Sourcing' },
  { to: '/supplier-pos', icon: '▣', label: 'Supplier Orders' },
  { to: '/inventory', icon: '▦', label: 'Inventory' },
  { to: '/deliveries', icon: '⇢', label: 'Deliveries' },
  { to: '/invoices', icon: '▤', label: 'Sales & Invoices' },
  { to: '/cashbook', icon: '↕', label: 'Cashbook' },
  { to: '/general-ledger', icon: '▥', label: 'General Ledger' },
  { to: '/reports', icon: '⌁', label: 'Reports' },
  { to: '/audit-trail', icon: '◎', label: 'Audit Trail' },
];

export default function Layout() {
  const { user, orgType, switchOrg, logout } = useAuthStore();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);

  const navItems = orgType === 'SUPPLIER' ? smbNav : avNav;
  const orgName = orgType === 'BUYER' ? 'AV Motors' : 'SMB';
  const initials = user?.fullName?.split(' ').map(n => n[0]).join('') || 'U';

  const handleLogout = () => { logout(); navigate('/login'); };

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        document.getElementById('globalSearch')?.focus();
      }
      if (e.key === 'Escape') setProfileOpen(false);
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, []);

  return (
    <div className={`app-shell ${collapsed ? 'sidebar-collapsed' : ''}`}>
      <aside className="sidebar" id="sidebar">
        <div className="brand-row">
          <div className="brand-mark">A</div>
          <div>
            <div className="brand-name">AV × SMB</div>
            <div className="brand-sub">Procurement OS</div>
          </div>
          <button className="icon-btn sidebar-toggle" onClick={() => setCollapsed(!collapsed)} title="Collapse">‹</button>
          <button className="icon-btn sidebar-close" onClick={() => setMobileOpen(false)}>×</button>
        </div>

        <div className="org-switcher">
          <button className={`org-pill ${orgType === 'BUYER' ? 'active' : ''}`} onClick={() => switchOrg('BUYER')}>
            <span className="org-dot av-dot" />
            <span><strong>AV Motors</strong><small>Buyer workspace</small></span>
          </button>
          <button className={`org-pill ${orgType === 'SUPPLIER' ? 'active' : ''}`} onClick={() => switchOrg('SUPPLIER')}>
            <span className="org-dot smb-dot" />
            <span><strong>SMB</strong><small>Supplier workspace</small></span>
          </button>
        </div>

        <div className="sidebar-label">WORKSPACE</div>
        <nav className="nav">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end}
              className={({ isActive }) => `nav-btn ${isActive ? 'active' : ''}`}
              onClick={() => setMobileOpen(false)}>
              <span className="nav-icon">{item.icon}</span>
              <span className="nav-name">{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="security-card">
            <div className="security-icon">✓</div>
            <div><strong>Strict RBAC</strong><span>Deny by default · Org-scoped</span></div>
          </div>
          <div className="profile-mini">
            <div className="avatar">{initials}</div>
            <div className="profile-copy">
              <strong>{user?.fullName || 'User'}</strong>
              <span>{user?.roleName || 'Role'}</span>
            </div>
          </div>
        </div>
      </aside>

      <div className="backdrop" onClick={() => setMobileOpen(false)} />

      <main className="main">
        <header className="topbar">
          <div className="topbar-left">
            <button className="icon-btn mobile-menu" onClick={() => setMobileOpen(true)}>☰</button>
            <div>
              <div className="eyebrow">{orgName?.toUpperCase()} / COMMAND CENTRE</div>
              <h1>Procurement OS</h1>
            </div>
          </div>
          <div className="topbar-actions">
            <label className="global-search">
              <span className="search-icon">⌕</span>
              <input id="globalSearch" placeholder="Search vehicles, jobs, orders…" />
              <kbd>⌘ K</kbd>
            </label>
            <button className="icon-btn notify-btn" title="Notifications">
              <span>♢</span><i>3</i>
            </button>
            <div className={`user-profile ${profileOpen ? 'open' : ''}`}>
              <button className="user-profile-trigger" onClick={() => setProfileOpen(!profileOpen)}>
                <div className="avatar" style={{ width: 34, height: 34, borderRadius: 10, fontSize: 12 }}>{initials}</div>
                <span className="user-profile-copy">
                  <strong>{user?.fullName || 'User'}</strong>
                  <span>{user?.roleName || 'Role'}</span>
                </span>
                <span className="user-profile-chevron">⌄</span>
              </button>
              {profileOpen && (
                <div className="profile-menu">
                  <div className="profile-menu-head">
                    <div className="avatar" style={{ width: 42, height: 42, borderRadius: 12, fontSize: 14 }}>{initials}</div>
                    <div>
                      <strong>{user?.fullName}</strong>
                      <span>{user?.roleName}</span>
                      <span className="profile-org-badge">{orgName}</span>
                    </div>
                  </div>
                  <button className="profile-menu-action" onClick={() => setProfileOpen(false)}>
                    <span className="pm-icon">◉</span>
                    <span><strong>My profile</strong><span>Identity and permissions</span></span>
                  </button>
                  <button className="profile-menu-action" onClick={() => setProfileOpen(false)}>
                    <span className="pm-icon">⌁</span>
                    <span><strong>Effective permissions</strong><span>{user?.permissions?.length || 0} permissions</span></span>
                  </button>
                  <div className="profile-menu-divider" />
                  <button className="profile-menu-action signout" onClick={handleLogout}>
                    <span className="pm-icon">↪</span>
                    <span><strong>Sign out</strong><span>End session</span></span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <section className="content">
          <Outlet />
        </section>
      </main>
    </div>
  );
}
