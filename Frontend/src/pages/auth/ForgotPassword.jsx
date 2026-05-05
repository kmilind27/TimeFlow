import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { AlertCircle, CheckCircle, ArrowLeft, Clock } from 'lucide-react'
import { requestOtp, verifyOtp, resetPassword } from '../../api/authApi'
import styles from './Auth.module.css'

function ForgotPassword() {
  const navigate = useNavigate()
  const [step, setStep] = useState(1) // 1: Email, 2: OTP, 3: Password
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [formData, setFormData] = useState({
    newPassword: '',
    confirmPassword: ''
  })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value })
    setError('')
  }

  const validatePassword = () => {
    if (formData.newPassword !== formData.confirmPassword) {
      setError('Passwords do not match')
      return false
    }
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/
    if (!passwordRegex.test(formData.newPassword)) {
      setError('Password must contain uppercase, lowercase, number and special character')
      return false
    }
    return true
  }

  // Step 1: Request OTP
  const handleRequestOtp = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      await requestOtp({ email })
      setSuccess('OTP sent to your registered email')
      setStep(2)
      setTimeout(() => {
        setSuccess('')
      }, 1500)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP. Check your email.')
    } finally {
      setLoading(false)
    }
  }

  // Step 2: Verify OTP
  const handleVerifyOtp = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const response = await verifyOtp({ email, otp })
      setResetToken(response.data.resetToken)
      setSuccess('OTP verified successfully')
      setStep(3)
      setTimeout(() => {
        setSuccess('')
      }, 1500)
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid or expired OTP')
    } finally {
      setLoading(false)
    }
  }

  // Step 3: Reset Password
  const handleResetPassword = async (e) => {
    e.preventDefault()
    if (!validatePassword()) return

    setLoading(true)
    setError('')

    try {
      await resetPassword({
        resetToken,
        newPassword: formData.newPassword,
        confirmPassword: formData.confirmPassword
      })
      setSuccess('Password reset successfully! Redirecting to login...')
      setTimeout(() => navigate('/login'), 2000)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to reset password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.authContainer}>
      <div className={styles.authCard}>
        <div className={styles.authHeader}>
          <h1 className={styles.logo}>
            <Clock size={32} strokeWidth={2.5} /> TimeFlow
          </h1>
          <p className={styles.subtitle}>
            {step === 1 && 'Enter your email to receive OTP'}
            {step === 2 && 'Enter the OTP sent to your email'}
            {step === 3 && 'Create your new password'}
          </p>
        </div>

        {error && <div className={styles.errorBox}><AlertCircle size={16} /> {error}</div>}
        {success && <div className={styles.successBox}><CheckCircle size={16} /> {success}</div>}

        {/* Step 1: Email */}
        {step === 1 && (
          <form onSubmit={handleRequestOtp} className={styles.form}>
            <div className={styles.formGroup}>
              <label className={styles.label}>Email Address</label>
              <input
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value)
                  setError('')
                }}
                placeholder="your registered email"
                className={styles.input}
                required
              />
            </div>
            <button type="submit" className={styles.submitBtn} disabled={loading}>
              {loading ? 'Sending...' : 'Send OTP'}
            </button>
          </form>
        )}

        {/* Step 2: OTP */}
        {step === 2 && (
          <form onSubmit={handleVerifyOtp} className={styles.form}>
            <div className={styles.formGroup}>
              <label className={styles.label}>Enter OTP</label>
              <input
                type="text"
                value={otp}
                onChange={(e) => {
                  setOtp(e.target.value)
                  setError('')
                }}
                placeholder="6-digit OTP"
                className={styles.input}
                maxLength="6"
                required
              />
            </div>
            <button type="submit" className={styles.submitBtn} disabled={loading}>
              {loading ? 'Verifying...' : 'Verify OTP'}
            </button>
            <button
              type="button"
              onClick={() => setStep(1)}
              className={styles.secondaryBtn}
              style={{ marginTop: '10px' }}
            >
              Resend OTP
            </button>
          </form>
        )}

        {/* Step 3: New Password */}
        {step === 3 && (
          <form onSubmit={handleResetPassword} className={styles.form}>
            <div className={styles.formGroup}>
              <label className={styles.label}>New Password</label>
              <input
                type="password"
                name="newPassword"
                value={formData.newPassword}
                onChange={handleChange}
                placeholder="New password"
                className={styles.input}
                required
              />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.label}>Confirm New Password</label>
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="Repeat new password"
                className={styles.input}
                required
              />
            </div>
            <div className={styles.passwordHint}>
              Password must contain uppercase, lowercase, number and special character (@$!%*?&)
            </div>
            <button type="submit" className={styles.submitBtn} disabled={loading}>
              {loading ? 'Updating...' : 'Reset Password'}
            </button>
          </form>
        )}

        <div className={styles.authLinks}>
          <Link to="/login" className={styles.link}>
            <ArrowLeft size={16} /> Back to Login
          </Link>
        </div>
      </div>
    </div>
  )
}

export default ForgotPassword