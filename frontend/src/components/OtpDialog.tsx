import { useState } from 'react';
import { otpApi } from '../api/services';
import toast from 'react-hot-toast';

interface OtpDialogProps {
  purpose: 'APPROVAL' | 'SIGNATURE' | 'MFA';
  title: string;
  message: string;
  onVerified: (otpId: string) => void;
  onCancel: () => void;
}

export default function OtpDialog({ purpose, title, message, onVerified, onCancel }: OtpDialogProps) {
  const [otpCode, setOtpCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [otpId, setOtpId] = useState<string | null>(null);

  const handleSendOtp = async () => {
    setLoading(true);
    try {
      const res = await otpApi.generate(purpose);
      if (res.data.success) {
        setOtpId(res.data.otpId);
        setOtpSent(true);
        toast.success('OTP sent to your phone');
      } else {
        toast.error('Failed to send OTP');
      }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async () => {
    if (!otpId) {
      toast.error('Please request an OTP first');
      return;
    }
    
    setLoading(true);
    try {
      const res = await otpApi.verify(otpId, otpCode.trim());
      if (res.data.success) {
        toast.success('OTP verified');
        onVerified(res.data.otpId);
      } else {
        toast.error('Invalid OTP code');
      }
    } catch (err: any) {
      toast.error(err.response?.data?.error || 'OTP verification failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md mx-4">
        <div className="text-center mb-6">
          <div className="w-14 h-14 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center mx-auto mb-4">
            <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-gray-900">{title}</h2>
          <p className="text-sm text-gray-500 mt-2">{message}</p>
        </div>

        {!otpSent ? (
          <div className="space-y-4">
            <p className="text-sm text-gray-600 text-center">
              An OTP will be sent to your registered phone number for verification.
            </p>
            <button
              onClick={handleSendOtp}
              disabled={loading}
              className="w-full py-3 px-4 bg-gradient-to-r from-blue-600 to-cyan-600 text-white rounded-xl font-semibold hover:from-blue-500 hover:to-cyan-500 disabled:opacity-50"
            >
              {loading ? 'Sending...' : 'Send OTP'}
            </button>
            <button
              onClick={onCancel}
              className="w-full py-2 text-gray-500 hover:text-gray-700 text-sm"
            >
              Cancel
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Enter 6-digit OTP code
              </label>
              <input
                type="text"
                value={otpCode}
                onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                className="w-full px-4 py-3 border border-gray-300 rounded-xl text-center text-2xl tracking-widest font-mono focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="000000"
                maxLength={6}
                autoFocus
              />
            </div>
            <button
              onClick={handleVerify}
              disabled={loading || otpCode.length !== 6}
              className="w-full py-3 px-4 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl font-semibold hover:from-green-500 hover:to-emerald-500 disabled:opacity-50"
            >
              {loading ? 'Verifying...' : 'Verify & Continue'}
            </button>
            <div className="flex gap-2">
              <button
                onClick={() => { setOtpSent(false); setOtpCode(''); }}
                className="flex-1 py-2 text-blue-600 hover:text-blue-700 text-sm font-medium"
              >
                Resend OTP
              </button>
              <button
                onClick={onCancel}
                className="flex-1 py-2 text-gray-500 hover:text-gray-700 text-sm"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
