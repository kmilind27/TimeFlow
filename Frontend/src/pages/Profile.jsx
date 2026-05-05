import React, { useState, useEffect } from 'react'
import { IdCard, User, Mail, Shield, CheckCircle, Calendar, Lock, AlertCircle } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getProfile, requestOtp, verifyOtp, resetPassword } from '../api/authApi'
import Layout from '../components/Layout'
import AutoDismissMessage from '../components/AutoDismissMessage'
import { formatDate } from '../utils/helpers'
import styles from './Profile.module.css'

const Profile = () => {
  const { user } = useAuth()

  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // Password reset state
  const [passwordStep, setPasswordStep] = useState(0) // 0: Hidden, 1: OTP, 2: Password
  const [otp, setOtp] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [passwordData, setPasswordData] = useState({
    newPassword: '',
    confirmPassword: ''
  })
  const [passwordError, setPasswordError] = useState('')
  const [passwordSuccess, setPasswordSuccess] = useState('')
  const [passwordLoading, setPasswordLoading] = useState(false)

  useEffect(() => {
    fetchProfile()
  }, [])

  async function fetchProfile() {
    try {
      const response = await getProfile()
      setProfile(response.data)
    } catch (err) {
      setError('Failed to load profile')
    }
  }

  function handlePasswordChange(e) {
    setPasswordData({ ...passwordData, [e.target.name]: e.target.value })
    setPasswordError('')
    setPasswordSuccess('')
  }

  function validatePassword() {
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setPasswordError('Passwords do not match')
      return false
    }
    if (passwordData.newPassword.length < 8) {
      setPasswordError('Password must be at least 8 characters')
      return false
    }
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/
    if (!regex.test(passwordData.newPassword)) {
      setPasswordError(
        'Password must contain uppercase, lowercase, number and special character'
      )
      return false
    }
    return true
  }

  // Step 1: Request OTP
  async function handleRequestOtp() {
    setPasswordLoading(true)
    setPasswordError('')
    setPasswordSuccess('')

    try {
      await requestOtp({ email: profile?.email || user?.email })
      setPasswordSuccess('OTP sent to your email')
      setPasswordStep(1)
      setTimeout(() => {
        setPasswordSuccess('')
      }, 1500)
    } catch (err) {
      setPasswordError(err.response?.data?.message || 'Failed to send OTP')
    } finally {
      setPasswordLoading(false)
    }
  }

  // Step 2: Verify OTP
  async function handleVerifyOtp(e) {
    e.preventDefault()
    setPasswordLoading(true)
    setPasswordError('')

    try {
      const response = await verifyOtp({ 
        email: profile?.email || user?.email, 
        otp 
      })
      setResetToken(response.data.resetToken)
      setPasswordSuccess('OTP verified successfully')
      setPasswordStep(2)
      setTimeout(() => {
        setPasswordSuccess('')
      }, 1500)
    } catch (err) {
      setPasswordError(err.response?.data?.message || 'Invalid or expired OTP')
    } finally {
      setPasswordLoading(false)
    }
  }

  // Step 3: Reset Password
  async function handlePasswordReset(e) {
    e.preventDefault()
    if (!validatePassword()) return

    setPasswordLoading(true)
    setPasswordError('')

    try {
      await resetPassword({
        resetToken,
        newPassword: passwordData.newPassword,
        confirmPassword: passwordData.confirmPassword
      })

      setPasswordSuccess('Password reset successfully!')
      setPasswordData({ newPassword: '', confirmPassword: '' })
      setOtp('')
      setResetToken('')
      setPasswordStep(0)
    } catch (err) {
      setPasswordError(
        err.response?.data?.message ||
        'Failed to reset password. Please try again.'
      )
    } finally {
      setPasswordLoading(false)
    }
  }

  function handleCancelPasswordReset() {
    setPasswordStep(0)
    setPasswordError('')
    setPasswordSuccess('')
    setPasswordData({ newPassword: '', confirmPassword: '' })
    setOtp('')
    setResetToken('')
  }

  function getRoleBadgeColor(role) {
    if (role === 'ADMIN') return styles.badgeAdmin
    if (role === 'MANAGER') return styles.badgeManager
    return styles.badgeEmployee
  }

  function getStatusBadgeColor(status) {
    return status === 'ACTIVE' ? styles.badgeActive : styles.badgeInactive
  }

  if (error) {
    return (
      <Layout>
        <div className={styles.errorContainer}>
          <p><AlertCircle size={16} /> {error}</p>
          <button onClick={fetchProfile} className={styles.retryBtn}>
            Retry
          </button>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className={styles.pageHeader}>
        <h2 className={styles.pageTitle}>My Profile</h2>
        <p className={styles.pageSubtitle}>
          View and manage your account details
        </p>
      </div>

      <div className={styles.profileGrid}>

        {/* ── Left Card — Avatar + Basic Info ── */}
        <div className={styles.avatarCard}>
          <div className={styles.avatarLarge}>
            {profile?.fullName?.charAt(0).toUpperCase()}
          </div>
          <h3 className={styles.profileName}>{profile?.fullName}</h3>
          <span className={`${styles.badge} ${getRoleBadgeColor(profile?.role)}`}>
            {profile?.role}
          </span>
          <span className={`${styles.badge} ${getStatusBadgeColor(profile?.status)}`}>
            {profile?.status}
          </span>
          <p className={styles.memberSince}>
            Member since {formatDate(profile?.createdAt)}
          </p>
        </div>

        {/* ── Right Side ── */}
        <div className={styles.rightSide}>

          {/* Details Card */}
          <div className={styles.detailsCard}>
            <h3 className={styles.cardTitle}>Account Details</h3>

            <div className={styles.detailsGrid}>

              <div className={styles.detailItem}>
                <span className={styles.detailLabel}>
                  <IdCard size={14} /> Employee Code
                </span>
                <span className={styles.detailValue}>
                  {profile?.employeeCode || '—'}
                </span>
              </div>

              <div className={styles.detailItem}>
                <span className={styles.detailLabel}>
                  <User size={14} /> Full Name
                </span>
                <span className={styles.detailValue}>
                  {profile?.fullName}
                </span>
              </div>

              <div className={styles.detailItem}>
                <span className={styles.detailLabel}>
                  <Mail size={14} /> Email Address
                </span>
                <span className={styles.detailValue}>
                  {profile?.email}
                </span>
              </div>

              <div className={styles.detailItem}>
                <span className={styles.detailLabel}>
                  <Shield size={14} /> Role
                </span>
                <span className={styles.detailValue}>
                  {profile?.role}
                </span>
              </div>

              <div className={styles.detailItem}>
                <span className={styles.detailLabel}>
                  <CheckCircle size={14} /> Status
                </span>
                <span className={styles.detailValue}>
                  {profile?.status}
                </span>
              </div>

              <div className={styles.detailItem}>
                <span className={styles.detailLabel}>
                  <Calendar size={14} /> Member Since
                </span>
                <span className={styles.detailValue}>
                  {formatDate(profile?.createdAt)}
                </span>
              </div>

            </div>
          </div>

          {/* Password Reset Card */}
          <div className={styles.passwordCard}>
            <div className={styles.passwordHeader}>
              <h3 className={styles.cardTitle}><Lock size={16} /> Password</h3>
              {passwordStep === 0 && (
                <button
                  onClick={handleRequestOtp}
                  className={styles.toggleBtn}
                  disabled={passwordLoading}
                >
                  {passwordLoading ? 'Sending...' : 'Reset Password'}
                </button>
              )}
              {passwordStep > 0 && (
                <button
                  onClick={handleCancelPasswordReset}
                  className={styles.toggleBtn}
                >
                  Cancel
                </button>
              )}
            </div>

            {/* Success/Error messages */}
            <AutoDismissMessage
              message={passwordSuccess}
              type="success"
              onDismiss={() => setPasswordSuccess('')}
            />
            <AutoDismissMessage
              message={passwordError}
              type="error"
              onDismiss={() => setPasswordError('')}
            />

            {/* Step 1: OTP Input */}
            {passwordStep === 1 && (
              <form onSubmit={handleVerifyOtp} className={styles.passwordForm}>
                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    Enter OTP sent to {profile?.email}
                  </label>
                  <input
                    type="text"
                    value={otp}
                    onChange={(e) => {
                      setOtp(e.target.value)
                      setPasswordError('')
                    }}
                    placeholder="6-digit OTP"
                    className={styles.input}
                    maxLength="6"
                    required
                  />
                </div>
                <button
                  type="submit"
                  className={styles.resetBtn}
                  disabled={passwordLoading}
                >
                  {passwordLoading ? 'Verifying...' : 'Verify OTP'}
                </button>
                <button
                  type="button"
                  onClick={handleRequestOtp}
                  className={styles.secondaryBtn}
                  disabled={passwordLoading}
                >
                  Resend OTP
                </button>
              </form>
            )}

            {/* Step 2: New Password Input */}
            {passwordStep === 2 && (
              <form onSubmit={handlePasswordReset} className={styles.passwordForm}>
                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    New Password
                  </label>
                  <input
                    type="password"
                    name="newPassword"
                    value={passwordData.newPassword}
                    onChange={handlePasswordChange}
                    placeholder="Enter new password"
                    className={styles.input}
                    required
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    Confirm New Password
                  </label>
                  <input
                    type="password"
                    name="confirmPassword"
                    value={passwordData.confirmPassword}
                    onChange={handlePasswordChange}
                    placeholder="Repeat new password"
                    className={styles.input}
                    required
                  />
                </div>

                <p className={styles.passwordHint}>
                  Must contain uppercase, lowercase,
                  number and special character (@$!%*?&)
                </p>

                <button
                  type="submit"
                  className={styles.resetBtn}
                  disabled={passwordLoading}
                >
                  {passwordLoading
                    ? 'Resetting...'
                    : 'Reset Password'}
                </button>
              </form>
            )}

          </div>

        </div>

      </div>
    </Layout>
  )
}

export default Profile