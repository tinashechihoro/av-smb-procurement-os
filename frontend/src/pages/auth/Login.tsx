import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/services';
import { useAuthStore } from '../../store/authStore';
import toast from 'react-hot-toast';

const DEV = import.meta.env.DEV;

export default function Login() {
  const [email, setEmail] = useState(DEV ? 'admin@avmotors.com' : '');
  const [password, setPassword] = useState(DEV ? 'Admin@123' : '');
  const [otpCode, setOtpCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [otpLoading, setOtpLoading] = useState(false);
  const [step, setStep] = useState<'credentials' | 'otp'>('credentials');
  const [pendingUserId, setPendingUserId] = useState<string | null>(null);
  const [pendingFullName, setPendingFullName] = useState<string | null>(null);
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const handleCredentialsSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await authApi.login(email, password.trim());
      // Accounts without MFA are authenticated outright and get their tokens here.
      if (!data.requiresOtp && data.user?.accessToken) {
        setAuth(data.user);
        toast.success(`Welcome, ${data.user.fullName}`);
        navigate('/');
        return;
      }
      // The server nests the identity under `user`; it used to be read only
      // from the top level, so this branch never matched and every login fell
      // through to "Unexpected login response".
      const userId = data.userId || data.user?.userId;
      if (data.requiresOtp && userId) {
        setPendingUserId(userId);
        setPendingFullName(data.fullName || data.user?.fullName || null);
        setStep('otp');
        toast.success(data.message || 'OTP sent to your registered phone');
      } else {
        toast.error(data.error || 'Unexpected login response');
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleOtpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pendingUserId) {
      toast.error('Session expired. Please login again.');
      setStep('credentials');
      return;
    }
    setOtpLoading(true);
    try {
      const { data } = await authApi.verifyLogin(pendingUserId, otpCode.trim());
      if (data.success && data.user) {
        setAuth(data.user);
        toast.success(`Welcome, ${data.user.fullName}`);
        navigate('/');
      } else {
        toast.error(data.error || 'Invalid OTP code');
      }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'OTP verification failed');
    } finally {
      setOtpLoading(false);
    }
  };

  if (step === 'otp') {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24, background: 'linear-gradient(180deg, #f8faff 0, #f5f7fb 300px, #f5f7fb 100%)' }}>
        <div style={{ width: 'min(440px, 100%)', background: '#fff', border: '1px solid #e1e6ee', borderRadius: 18, padding: '36px 32px', boxShadow: '0 12px 34px rgba(15,34,68,.065)' }}>
          <div style={{ textAlign: 'center', marginBottom: 24 }}>
            <div style={{ width: 56, height: 56, borderRadius: 14, background: 'linear-gradient(135deg, #2f7cff, #0844ae)', display: 'grid', placeItems: 'center', margin: '0 auto 14px', fontSize: 24, fontWeight: 900, color: '#fff', boxShadow: '0 7px 18px rgba(18,97,230,.18)' }}>🔒</div>
            <h1 style={{ fontSize: 22, fontWeight: 760, letterSpacing: '-.03em', margin: '0 0 4px', color: '#000' }}>Enter Verification Code</h1>
            <p style={{ fontSize: 12, color: '#555', margin: '4px 0 0' }}>{pendingFullName}</p>
            <p style={{ fontSize: 11, color: '#888', margin: '8px 0 0' }}>A 6-digit OTP has been sent to your phone</p>
          </div>
          <form onSubmit={handleOtpSubmit}>
            <div className="form-field" style={{ marginBottom: 14 }}>
              <label>OTP Code</label>
              <input
                type="text"
                value={otpCode}
                onChange={e => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="000000"
                maxLength={6}
                required
                autoFocus
                style={{ textAlign: 'center', fontSize: 20, letterSpacing: 8, fontFamily: 'monospace' }}
              />
            </div>
            <button className="primary-btn" type="submit" disabled={otpLoading || otpCode.length !== 6} style={{ width: '100%', padding: 12, fontSize: 13, marginTop: 8 }}>
              {otpLoading ? 'Verifying...' : 'Verify & Login'}
            </button>
            <button type="button" onClick={() => { setStep('credentials'); setOtpCode(''); setPendingUserId(null); }} style={{ width: '100%', padding: 8, marginTop: 8, background: 'none', border: 'none', color: '#666', cursor: 'pointer', fontSize: 12 }}>
              ← Back to login
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24, background: 'linear-gradient(180deg, #f8faff 0, #f5f7fb 300px, #f5f7fb 100%)' }}>
      <div style={{ width: 'min(440px, 100%)', background: '#fff', border: '1px solid #e1e6ee', borderRadius: 18, padding: '36px 32px', boxShadow: '0 12px 34px rgba(15,34,68,.065)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{ width: 56, height: 56, borderRadius: 14, background: 'linear-gradient(135deg, #2f7cff, #0844ae)', display: 'grid', placeItems: 'center', margin: '0 auto 14px', fontSize: 24, fontWeight: 900, color: '#fff', boxShadow: '0 7px 18px rgba(18,97,230,.18)' }}>A</div>
          <h1 style={{ fontSize: 22, fontWeight: 760, letterSpacing: '-.03em', margin: '0 0 4px', color: '#000' }}>AV × SMB Procurement OS</h1>
          <p style={{ fontSize: 12, color: '#111', margin: 0 }}>Enterprise procurement and spare parts management</p>
        </div>

        <form onSubmit={handleCredentialsSubmit}>
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

        <div style={{ marginTop: 14, padding: 12, background: '#f4fbf8', borderRadius: 11, border: '1px solid #d8eee6', display: 'flex', gap: 10, alignItems: 'center' }}>
          <div style={{ width: 28, height: 28, borderRadius: 8, background: 'rgba(11,154,113,.10)', color: '#0b6e52', display: 'grid', placeItems: 'center', fontWeight: 900, fontSize: 13, flexShrink: 0 }}>✓</div>
          <div style={{ fontSize: 11, color: '#000' }}>
            <strong>SMS OTP + Strict RBAC</strong>
            <span style={{ display: 'block', color: '#111', marginTop: 2 }}>Organisation-scoped · OTP verified · Audit logged</span>
          </div>
        </div>
      </div>
    </div>
  );
}
