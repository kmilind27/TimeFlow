import { NavLink, useNavigate, Link } from 'react-router-dom'
import { Clock, FileText, Plus, FilePlus, Calendar, Wallet, BarChart3, Inbox, Users, PartyPopper, LogOut, Menu } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useState } from 'react'
import styles from './Sidebar.module.css'

function Sidebar() {
  const { user, logoutUser, isEmployee, isManager, isAdmin } = useAuth()
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)

  const handleLogout = () => {
    logoutUser()
    navigate('/login')
  }

  const closeSidebar = () => {
    setIsOpen(false)
  }

  return (
    <>
      {/* Mobile Top Bar */}
      <div className={styles.mobileTopBar}>
        <button
          className={styles.hamburger}
          onClick={() => setIsOpen(!isOpen)}
          aria-label="Toggle menu"
        >
          <Menu size={24} />
        </button>
        <span className={styles.logoText}>
          <Clock size={20} /> TimeFlow
        </span>
      </div>

      {/* Overlay - Mobile Only */}
      {isOpen && (
        <div
          className={styles.overlay}
          onClick={closeSidebar}
        />
      )}

      {/* Sidebar */}
      <aside className={`${styles.sidebar} ${isOpen ? styles.sidebarOpen : ''}`}>

        {/* Logo */}
        <div className={styles.logo}>
          <Clock size={28} className={styles.logoIcon} />
          <span className={styles.logoText}>TimeFlow</span>
        </div>
        <p className={styles.logoDescription}>Timesheet and Leave Management</p>

        {/* User Info */}
        <Link to="/profile" className={styles.userInfoLink} onClick={closeSidebar}>
          <div className={styles.userInfo}>
            <div className={styles.avatar}>
              {user?.fullName?.charAt(0).toUpperCase()}
            </div>
            <div className={styles.userDetails}>
              <span className={styles.userName}>{user?.fullName}</span>
              <span className={styles.userRole}>{user?.role}</span>
            </div>
          </div>
        </Link>

        {/* Navigation */}
        <nav className={styles.nav}>

          {/* Timesheet Section (EMPLOYEE only) */}
          {isEmployee() && (
            <div className={styles.navSection}>
              <span className={styles.navSectionTitle}>Timesheet</span>

              <NavLink
                to="/timesheet/dashboard"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><FileText size={18} /></span>
                My Timesheets
              </NavLink>

              <NavLink
                to="/timesheet/log-entry"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><Plus size={18} /></span>
                Log Hours
              </NavLink>

            </div>
          )}

          {/* Leave Section (EMPLOYEE only) */}
          {isEmployee() && (
            <div className={styles.navSection}>
              <span className={styles.navSectionTitle}>Leave</span>

              <NavLink
                to="/leave/apply"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><FilePlus size={18} /></span>
                Apply Leave
              </NavLink>

              <NavLink
                to="/leave/history"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><Calendar size={18} /></span>
                Leave History
              </NavLink>

              <NavLink
                to="/leave/balance"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><Wallet size={18} /></span>
                Leave Balance
              </NavLink>

            </div>
          )}

          {/* Manager Section (MANAGER only) */}
          {isManager() && (
            <div className={styles.navSection}>

              <NavLink
                to="/manager/dashboard"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><BarChart3 size={18} /></span>
                Dashboard
              </NavLink>

              <NavLink
                to="/manager/pending-timesheets"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><Clock size={18} /></span>
                Pending Timesheets
              </NavLink>

              <NavLink
                to="/manager/pending-leaves"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><Inbox size={18} /></span>
                Pending Leaves
              </NavLink>

              <NavLink
                to="/manager/team"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><Users size={18} /></span>
                My Team
              </NavLink>

            </div>
          )}

          {/* Admin Section (ADMIN only) */}
          {isAdmin() && (
            <div className={styles.navSection}>

              <NavLink
                to="/admin/dashboard"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><BarChart3 size={18} /></span>
                Dashboard
              </NavLink>

              <NavLink
                to="/admin/users"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><Users size={18} /></span>
                User Management
              </NavLink>

              <NavLink
                to="/admin/holidays"
                className={({ isActive }) =>
                  isActive ? `${styles.navItem} ${styles.active}` : styles.navItem
                }
                onClick={closeSidebar}
              >
                <span className={styles.navIcon}><PartyPopper size={18} /></span>
                Holidays
              </NavLink>

            </div>
          )}

        </nav>

        {/* Logout */}
        <button
          onClick={handleLogout}
          className={styles.logoutBtn}
        >
          <LogOut size={18} className={styles.logoutIcon} />
          Logout
        </button>

      </aside>
    </>
  )
}

export default Sidebar
