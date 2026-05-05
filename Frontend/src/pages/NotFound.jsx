import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import styles from './NotFound.module.css'

const NotFound = () => {
  const navigate = useNavigate()
  const { isAuthenticated, user } = useAuth()

  const handleGoHome = () => {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }
    if (user?.role === 'ADMIN') navigate('/admin/dashboard')
    else if (user?.role === 'MANAGER') navigate('/manager/dashboard')
    else navigate('/timesheet/dashboard')
  }

  return (
    <div className={styles.container}>
      <div className={styles.content}>
        <h1 className={styles.errorCode}>404</h1>
        <p className={styles.message}>Page not found</p>
        <button onClick={handleGoHome} className={styles.homeBtn}>
          {isAuthenticated ? 'Go Home' : 'Back to Login'}
        </button>
      </div>
    </div>
  )
}

export default NotFound