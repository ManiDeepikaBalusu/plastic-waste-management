import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { authApi } from '../../api/client';
import { Lock, Eye, EyeOff, CheckCircle2, AlertCircle, ArrowRight, ShieldCheck, RefreshCw } from 'lucide-react';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [validating, setValidating] = useState(true);
  const [tokenValid, setTokenValid] = useState(false);
  const [maskedEmail, setMaskedEmail] = useState('');
  const [tokenError, setTokenError] = useState('');

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');
  const [resetSuccess, setResetSuccess] = useState(false);

  useEffect(() => {
    async function validateToken() {
      if (!token || !token.trim()) {
        setTokenValid(false);
        setTokenError('No password reset token was provided in the link. Please request a new reset link.');
        setValidating(false);
        return;
      }

      try {
        setValidating(true);
        const res = await authApi.validateResetToken(token.trim());
        if (res && res.valid) {
          setTokenValid(true);
          setMaskedEmail(res.emailMasked || '');
        } else {
          setTokenValid(false);
          setTokenError(res?.message || 'This password reset link is invalid, expired, or has already been used.');
        }
      } catch (err) {
        setTokenValid(false);
        setTokenError(err.message || 'Unable to validate reset token. Please request a new link.');
      } finally {
        setValidating(false);
      }
    }

    validateToken();
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!newPassword || newPassword.length < 8) {
      setFormError('Password must be at least 8 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setFormError('Passwords do not match. Please ensure both fields are identical.');
      return;
    }

    try {
      setSubmitting(true);
      await authApi.resetPassword({
        token: token.trim(),
        newPassword
      });
      setResetSuccess(true);
    } catch (err) {
      setFormError(err.message || 'Failed to reset password. The link may have expired or been used.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="main-content" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '70vh' }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '440px', padding: '2.5rem 2rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div className="brand-icon" style={{ margin: '0 auto 1rem', width: '48px', height: '48px', fontSize: '1.5rem' }}>
            🔒
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '0.5rem' }}>Reset Password</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Set a new secure password for your EcoClean account
          </p>
        </div>

        {validating && (
          <div style={{ textAlign: 'center', padding: '2rem 0', color: 'var(--text-dim)' }}>
            <RefreshCw size={28} className="animate-spin" style={{ margin: '0 auto 1rem', display: 'block', color: 'var(--primary)' }} />
            <p style={{ fontSize: '0.9rem' }}>Validating your security token...</p>
          </div>
        )}

        {!validating && !tokenValid && !resetSuccess && (
          <div style={{ textAlign: 'center' }}>
            <div style={{
              background: 'rgba(244, 63, 94, 0.12)',
              border: '1px solid rgba(244, 63, 94, 0.35)',
              color: '#f43f5e',
              padding: '1.25rem',
              borderRadius: 'var(--radius-sm)',
              marginBottom: '1.5rem',
              textAlign: 'left'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                <AlertCircle size={20} />
                Invalid or Expired Link
              </div>
              <p style={{ fontSize: '0.88rem', margin: 0, lineHeight: 1.5, color: 'var(--text-main)' }}>
                {tokenError || 'This password reset link is invalid, expired, or has already been used.'}
              </p>
            </div>

            <Link
              to="/forgot-password"
              className="service-btn"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                width: '100%',
                padding: '0.8rem',
                fontWeight: 700,
                textDecoration: 'none',
                background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))',
                color: '#000'
              }}
            >
              Request a New Reset Link <ArrowRight size={16} />
            </Link>

            <div style={{ marginTop: '1.25rem' }}>
              <Link to="/login" style={{ color: 'var(--text-dim)', fontSize: '0.88rem', textDecoration: 'none' }}>
                Return to Sign In
              </Link>
            </div>
          </div>
        )}

        {!validating && tokenValid && resetSuccess && (
          <div style={{ textAlign: 'center' }}>
            <div style={{
              background: 'rgba(16, 185, 129, 0.12)',
              border: '1px solid rgba(16, 185, 129, 0.35)',
              color: '#34d399',
              padding: '1.5rem 1.25rem',
              borderRadius: 'var(--radius-sm)',
              marginBottom: '1.5rem',
              textAlign: 'left'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                <CheckCircle2 size={22} />
                Password Reset Successful!
              </div>
              <p style={{ fontSize: '0.88rem', margin: 0, lineHeight: 1.5, color: 'var(--text-main)' }}>
                Your account password has been securely updated and previous sessions have been invalidated. You can now sign in using your new credentials.
              </p>
            </div>

            <Link
              to="/login"
              className="service-btn"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                width: '100%',
                padding: '0.8rem',
                fontWeight: 800,
                textDecoration: 'none',
                background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))',
                color: '#000'
              }}
            >
              Proceed to Sign In <ArrowRight size={16} />
            </Link>
          </div>
        )}

        {!validating && tokenValid && !resetSuccess && (
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {maskedEmail && (
              <div style={{
                background: 'rgba(56, 189, 248, 0.08)',
                border: '1px solid rgba(56, 189, 248, 0.25)',
                color: 'var(--accent-cyan)',
                padding: '0.65rem 0.85rem',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.82rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem'
              }}>
                <ShieldCheck size={16} />
                <span>Resetting password for: <strong>{maskedEmail}</strong></span>
              </div>
            )}

            {formError && (
              <div style={{
                background: 'rgba(244, 63, 94, 0.12)',
                border: '1px solid rgba(244, 63, 94, 0.35)',
                color: '#f43f5e',
                padding: '0.85rem 1rem',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.88rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem'
              }}>
                <AlertCircle size={18} />
                <span>{formError}</span>
              </div>
            )}

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                New Password
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-dim)' }} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="At least 8 characters"
                  style={{
                    width: '100%',
                    background: 'rgba(255, 255, 255, 0.04)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    padding: '0.75rem 2.5rem 0.75rem 2.5rem',
                    color: 'var(--text-main)',
                    fontSize: '0.95rem'
                  }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-dim)',
                    cursor: 'pointer',
                    padding: 0
                  }}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                Confirm New Password
              </label>
              <div style={{ position: 'relative' }}>
                <Lock size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-dim)' }} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Re-enter new password"
                  style={{
                    width: '100%',
                    background: 'rgba(255, 255, 255, 0.04)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    padding: '0.75rem 1rem 0.75rem 2.5rem',
                    color: 'var(--text-main)',
                    fontSize: '0.95rem'
                  }}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="service-btn"
              style={{
                background: 'linear-gradient(135deg, var(--primary), var(--accent-cyan))',
                color: '#000',
                fontWeight: 800,
                padding: '0.8rem',
                fontSize: '1rem',
                marginTop: '0.5rem',
                boxShadow: '0 0 15px var(--primary-glow)',
                cursor: submitting ? 'not-allowed' : 'pointer',
                opacity: submitting ? 0.7 : 1
              }}
            >
              {submitting ? 'Updating Password...' : 'Save New Password'}
              {!submitting && <ArrowRight size={16} />}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
