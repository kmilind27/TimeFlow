import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login } from '../../api/authApi'
import { useAuth } from '../../context/AuthContext'
import { AlertCircle, Clock } from 'lucide-react'
import styles from './Auth.module.css'

function Login() {
  const navigate = useNavigate()
  const { loginUser } = useAuth()

  const [formData, setFormData] = useState({
    email: '',
    password: ''
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value })
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      const response = await login(formData)
      // Save token + user info to context + localStorage
      loginUser(response.data)

      // Redirect based on role
      const role = response.data.role
      if (role === 'ADMIN') navigate('/admin/dashboard')
      else if (role === 'MANAGER') navigate('/manager/pending-timesheets')
      else navigate('/timesheet/dashboard')

    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Invalid email or password'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.authContainer}>
      <div className={styles.authCard}>

        {/* Logo / Title */}
        <div className={styles.authHeader}>
          <h1 className={styles.logo}>
            <Clock size={32} strokeWidth={2.5} /> TimeFlow
          </h1>
          <p className={styles.subtitle}>Sign in to your account</p>
        </div>

        {/* Error Message */}
        {error && (
          <div className={styles.errorBox}>
            <AlertCircle size={16} /> {error}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className={styles.form}>

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
              placeholder="Enter your password"
              className={styles.input}
              required
            />
          </div>

          <button
            type="submit"
            className={styles.submitBtn}
            disabled={loading}
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </button>

        </form>

        {/* Links */}
        <div className={styles.authLinks}>
          <Link to="/forgot-password" className={styles.link}>
            Forgot password?
          </Link>
          <span className={styles.divider}>|</span>
          <Link to="/signup" className={styles.link}>
            Create account
          </Link>
        </div>

      </div>
    </div>
  )
}

export default Login