import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'

// Auth pages
import Login from './pages/auth/Login'
import Signup from './pages/auth/Signup'
import ForgotPassword from './pages/auth/ForgotPassword'

// Protected route
import ProtectedRoute from './components/ProtectedRoute'

// Timesheet pages
import TimesheetDashboard from './pages/timesheet/TimesheetDashboard'
import LogEntry from './pages/timesheet/LogEntry'

// Leave pages
import LeaveApply from './pages/leave/LeaveApply'
import LeaveHistory from './pages/leave/LeaveHistory'
import LeaveBalance from './pages/leave/LeaveBalance'

// Manager pages
import ManagerDashboard from './pages/manager/ManagerDashboard'
import PendingTimesheets from './pages/manager/PendingTimesheets'
import PendingLeaves from './pages/manager/PendingLeaves'
import Team from './pages/manager/Team'

// Admin pages
import AdminDashboard from './pages/admin/Dashboard'
import UserManagement from './pages/admin/UserManagement'
import Holidays from './pages/admin/Holidays'
import NotFound from './pages/NotFound'

//Profile page
import Profile from './pages/Profile'

const App = () => {
  const { isAuthenticated, user } = useAuth()

  // Role based default redirect helper
  const getHomeRoute = () => {
    if (user?.role === 'ADMIN') return '/admin/dashboard'
    if (user?.role === 'MANAGER') return '/manager/dashboard'
    return '/timesheet/dashboard'
  }

  const router = createBrowserRouter([
    //------------Public Routes-----------------
    {
      path: '/login',
      element: isAuthenticated
        ? <Navigate to={getHomeRoute()} replace />
        : <Login />
    },
    {
      path: '/signup',
      element: <Signup />
    },
    {
      path: '/forgot-password',
      element: <ForgotPassword />
    },
    {
      path: '/timesheet/dashboard',
      element: (
        <ProtectedRoute>
          <TimesheetDashboard />
        </ProtectedRoute>
      )
    },
    {
      path: '/timesheet/log-entry',
      element: (
        <ProtectedRoute>
          <LogEntry />
        </ProtectedRoute>
      )
    },
    {
      path: '/leave/apply',
      element: (
        <ProtectedRoute>
          <LeaveApply />
        </ProtectedRoute>
      )
    },
    {
      path: '/leave/history',
      element: (
        <ProtectedRoute>
          <LeaveHistory />
        </ProtectedRoute>
      )
    },
    {
      path: '/leave/balance',
      element: (
        <ProtectedRoute>
          <LeaveBalance />
        </ProtectedRoute>
      )
    },

    //------------Manager/Admin Routes-----------------
    {
      path: '/manager/dashboard',
      element: (
        <ProtectedRoute allowedRoles={['MANAGER', 'ADMIN']}>
          <ManagerDashboard />
        </ProtectedRoute>
      )
    },
    {
      path: '/manager/pending-timesheets',
      element: (
        <ProtectedRoute allowedRoles={['MANAGER', 'ADMIN']}>
          <PendingTimesheets />
        </ProtectedRoute>
      )
    },
    {
      path: '/manager/pending-leaves',
      element: (
        <ProtectedRoute allowedRoles={['MANAGER', 'ADMIN']}>
          <PendingLeaves />
        </ProtectedRoute>
      )
    },
    {
      path: '/manager/team',
      element: (
        <ProtectedRoute allowedRoles={['MANAGER', 'ADMIN']}>
          <Team />
        </ProtectedRoute>
      )
    },

    //------------Admin Only Routes-----------------
    {
      path: '/admin/dashboard',
      element: (
        <ProtectedRoute allowedRoles={['ADMIN']}>
          <AdminDashboard />
        </ProtectedRoute>
      )
    },
    {
      path: '/admin/users',
      element: (
        <ProtectedRoute allowedRoles={['ADMIN']}>
          <UserManagement />
        </ProtectedRoute>
      )
    },
    {
      path: '/admin/holidays',
      element: (
        <ProtectedRoute allowedRoles={['ADMIN']}>
          <Holidays />
        </ProtectedRoute>
      )
    },
    {
      path: '/profile',
      element: (
        <ProtectedRoute>
          <Profile />
        </ProtectedRoute>
      )
    },

    //-------------Default & 404 Handler-------------
    {
      path: '/',
      element: <Navigate to="/login" replace />
    },
    {
      path: '*',
      element: <NotFound />
    }
  ])

  return (
    <RouterProvider router={router} />
  )
}

export default App