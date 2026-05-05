import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, user, loading } = useAuth()

  //Wait until auth is restored from localStorage
  if (loading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        fontSize: '16px',
        color: '#666'
      }}>
        Loading...
      </div>
    )
  }

  //Not logged in → redirect to login
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  //Logged in but wrong role → redirect to their home
  if (allowedRoles && !allowedRoles.includes(user?.role)) {
    if (user?.role === 'ADMIN') return <Navigate to="/admin/dashboard" replace />
    if (user?.role === 'MANAGER') return <Navigate to="/manager/pending-timesheets" replace />
    return <Navigate to="/timesheet/dashboard" replace />
  }

  return children
}

export default ProtectedRoute