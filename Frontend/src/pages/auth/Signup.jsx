import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { signup } from '../../api/authApi'
import { AlertCircle, CheckCircle, Clock } from 'lucide-react'
import styles from './Auth.module.css'

function Signup() {
  const navigate = useNavigate()

  const [formData, setFormData] = useState({
    employeeCode: '',
    fullName: '',
    email: '',
    password: '',
    confirmPassword: ''
  })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value })
    setError('')
  }

  const validate = () => {
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match')
      return false
    }
    if (formData.password.length < 8) {
      setError('Password must be at least 8 characters')
      return false
    }
    const passwordRegex =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/
    if (!passwordRegex.test(formData.password)) {
      setError(
        'Password must contain uppercase, lowercase, number and special character'
      )
      return false
    } 
    return true
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return

    setLoading(true)
    setError('')

    try {
      // ✅ Don't send confirmPassword to backend
      const { confirmPassword, ...requestData } = formData
      await signup(requestData)

      setSuccess('Account created successfully! Redirecting to login...')
      setTimeout(() => navigate('/login'), 2000)

    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Signup failed. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  function getPasswordRequirements(password) {
    return [
      {
        text: 'At least 8 characters',
        met: password.length >= 8
      },
      {
        text: 'One uppercase letter',
        met: /[A-Z]/.test(password)
      },
      {
        text: 'One lowercase letter',
        met: /[a-z]/.test(password)
      },
      {
        text: 'One number',
        met: /\d/.test(password)
      },
      {
        text: 'One special character (@$!%*?&)',
        met: /[@$!%*?&]/.test(password)
      }
    ]
  }

  return (
    <div className={styles.authContainer}>
      <div className={styles.authCard}>

        <div className={styles.authHeader}>
          <h1 className={styles.logo}>
            <Clock size={32} strokeWidth={2.5} /> TimeFlow
          </h1>
          <p className={styles.subtitle}>Create your account</p>
        </div>

        {error && (
          <div className={styles.errorBox}><AlertCircle size={16} /> {error}</div>
        )}
        {success && (
          <div className={styles.successBox}><CheckCircle size={16} /> {success}</div>
        )}

        <form onSubmit={handleSubmit} className={styles.form}>

          <div className={styles.formRow}>
            <div className={styles.formGroup}>
              <label className={styles.label}>Employee Code</label>
              <input
                type="text"
                name="employeeCode"
                value={formData.employeeCode}
                onChange={handleChange}
                placeholder="EMP001"
                className={styles.input}
                required
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>Full Name</label>
              <input
                type="text"
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                placeholder="John Doe"
                className={styles.input}
                required
              />
            </div>
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Email Address</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="you@company.com"
              className={styles.input}
              required
            />
          </div>

          <div className={styles.formGroup}>
            <label className={styles.label}>Password</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Min 8 chars"
              className={styles.input}
              required
            />
          </div>

          {/* Show requirements only when user starts typing password */}
          {formData.password && (
            <div className={styles.passwordRequirements}>
              {getPasswordRequirements(formData.password).map((req, index) => (
                <div
                  key={index}
                  className={`${styles.requirement} ${
                    req.met ? styles.requirementMet : styles.requirementUnmet
                  }`}
                >
                  <span className={styles.requirementIcon}>
                    {req.met ? '✓' : '○'}
                  </span>
                  {req.text}
                </div>
              ))}
            </div>
          )}

          <div className={styles.formGroup}>
            <label className={styles.label}>Confirm Password</label>
            <input
              type="password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Repeat password"
              className={styles.input}
              required
            />
          </div>

          <button
            type="submit"
            className={styles.submitBtn}
            disabled={loading}
          >
            {loading ? 'Creating account...' : 'Create Account'}
          </button>

        </form>

        <div className={styles.authLinks}>
          <span>Already have an account?</span>
          <Link to="/login" className={styles.link}>Sign in</Link>
        </div>

      </div>
    </div>
  )
}

export default Signup