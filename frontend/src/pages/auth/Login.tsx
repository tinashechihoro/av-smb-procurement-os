import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import toast from 'react-hot-toast';

// Dev-server convenience only: the production bundle ships blank fields and
// no credential hints (import.meta.env.DEV is false after `vite build`).
const DEV = import.meta.env.DEV;

export default function Login() {
  const [email, setEmail] = useState(DEV ? 'admin@avmotors.com' : '');
  const [password, setPassword] = useState(DEV ? 'Admin@123' : '');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await authApi.login(email, password);
      setAuth(data);
      toast.success(`Welcome, ${data.fullName}`);
      navigate('/');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24, background: 'linear-gradient(180deg, #f8faff 0, #f5f7fb 300px, #f5f7fb 100%)' }}>
      <div style={{ width: 'min(440px, 100%)', background: '#fff', border: '1px solid #e1e6ee', borderRadius: 18, padding: '36px 32px', boxShadow: '0 12px 34px rgba(15,34,68,.065)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{ width: 56, height: 56, borderRadius: 14, background: 'linear-gradient(135deg, #2f7cff, #0844ae)', display: 'grid', placeItems: 'center', margin: '0 auto 14px', fontSize: 24, fontWeight: 900, color: '#fff', boxShadow: '0 7px 18px rgba(18,97,230,.18)' }}>A</div>
          <h1 style={{ fontSize: 22, fontWeight: 760, letterSpacing: '-.03em', margin: '0 0 4px', color: '#000' }}>AV × SMB Procurement OS</h1>
          <p style={{ fontSize: 12, color: '#111', margin: 0 }}>Enterprise procurement and spare parts management</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-field" style={{ marginBottom: 14 }}>
            <label>Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="admin@avmotors.com" required />
          </div>
          <div className="form-field" style={{ marginBottom: 14 }}>
            <label>Password</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" required />
          </div>
          <button className="primary-btn" type="submit" disabled={loading} style={{ width: '100%', padding: 12, fontSize: 13, marginTop: 8 }}>
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        {DEV && (
          <div style={{ marginTop: 20, padding: 14, background: '#f8faff', borderRadius: 12, border: '1px solid #e3e8f0' }}>
            <div style={{ fontSize: 11, color: '#000', fontWeight: 700, marginBottom: 8 }}>Demo Credentials</div>
            <div style={{ fontSize: 11, color: '#111', lineHeight: 1.6 }}>
              <div><strong>AV Admin:</strong> admin@avmotors.com / Admin@123</div>
              <div><strong>SMB Admin:</strong> admin@smbprocurement.com / Admin@123</div>
            </div>
          </div>
        )}

        <div style={{ marginTop: 14, padding: 12, background: '#f4fbf8', borderRadius: 11, border: '1px solid #d8eee6', display: 'flex', gap: 10, alignItems: 'center' }}>
          <div style={{ width: 28, height: 28, borderRadius: 8, background: 'rgba(11,154,113,.10)', color: '#0b6e52', display: 'grid', placeItems: 'center', fontWeight: 900, fontSize: 13, flexShrink: 0 }}>✓</div>
          <div style={{ fontSize: 11, color: '#000' }}>
            <strong>Strict RBAC</strong>
            <span style={{ display: 'block', color: '#111', marginTop: 2 }}>Organisation-scoped · Deny by default · Audit logged</span>
          </div>
        </div>
      </div>
    </div>
  );
}
