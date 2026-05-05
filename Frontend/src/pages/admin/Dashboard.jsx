import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { RefreshCw, Clock, CheckCircle, Inbox, Users, FileText, Calendar, AlertCircle } from 'lucide-react'
import Layout from '../../components/Layout'
import { getAllUsers } from '../../api/authApi'
import { getPendingTimesheets } from '../../api/timesheetApi'
import { getPendingLeaves } from '../../api/leaveApi'
import { getLeaveTypeLabel, getStatusColor } from '../../utils/helpers'
import styles from './Admin.module.css'

const AdminDashboard = () => {
  const navigate = useNavigate()

  const [dashboard, setDashboard] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchDashboard()
  }, [])

  async function fetchDashboard() {
    try {
      setError('')
      
      // Fetch data from multiple endpoints in parallel
      const [usersRes, timesheetsRes, leavesRes] = await Promise.all([
        getAllUsers(),
        getPendingTimesheets(),
        getPendingLeaves()
      ])

      const allUsers = usersRes.data || []
      const pendingTimesheets = timesheetsRes.data || []
      const pendingLeaves = leavesRes.data || []

      // Calculate stats
      const dashboardData = {
        allEmployees: allUsers,
        pendingTimesheets: pendingTimesheets.length,
        approvedTimesheets: 0, // Not available from current endpoints
        pendingLeaves: pendingLeaves.length,
        recentTimesheets: pendingTimesheets.slice(0, 6),
        recentLeaves: pendingLeaves.slice(0, 6)
      }

      setDashboard(dashboardData)
    } catch (err) {
      setError('Failed to load dashboard data')
    }
  }

  if (error) {
    return (
      <Layout>
        <div className={styles.errorBox}><AlertCircle size={16} /> {error}</div>
        <button
          className={styles.retryBtn}
          onClick={fetchDashboard}
        >
          Retry
        </button>
      </Layout>
    )
  }

  return (
    <Layout>

      {/* ── Page Header ── */}
      <div className={styles.pageHeader}>
        <div>
          <h2 className={styles.pageTitle}>Admin Dashboard</h2>
          <p className={styles.pageSubtitle}>
            System overview and statistics
          </p>
        </div>
        <button
          className={styles.refreshBtn}
          onClick={fetchDashboard}
        >
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      {/* ── Stats Cards ── */}
      <div className={styles.statsGrid}>

        <div className={`${styles.statCard} ${styles.statBlue}`}>
          <div className={styles.statIcon}><Clock size={32} /></div>
          <div className={styles.statInfo}>
            <span className={styles.statNumber}>
              {dashboard?.pendingTimesheets || 0}
            </span>
            <span className={styles.statLabel}>
              Pending Timesheets
            </span>
          </div>
        </div>

        <div className={`${styles.statCard} ${styles.statGreen}`}>
          <div className={styles.statIcon}><CheckCircle size={32} /></div>
          <div className={styles.statInfo}>
            <span className={styles.statNumber}>
              {dashboard?.approvedTimesheets || 0}
            </span>
            <span className={styles.statLabel}>
              Approved Timesheets
            </span>
          </div>
        </div>

        <div className={`${styles.statCard} ${styles.statOrange}`}>
          <div className={styles.statIcon}><Inbox size={32} /></div>
          <div className={styles.statInfo}>
            <span className={styles.statNumber}>
              {dashboard?.pendingLeaves || 0}
            </span>
            <span className={styles.statLabel}>
              Pending Leaves
            </span>
          </div>
        </div>

        <div 
          className={`${styles.statCard} ${styles.statPurple} ${styles.clickableCard}`}
          onClick={() => navigate('/admin/users')}
        >
          <div className={styles.statIcon}><Users size={32} /></div>
          <div className={styles.statInfo}>
            <span className={styles.statNumber}>
              {dashboard?.allEmployees?.length || 0}
            </span>
            <span className={styles.statLabel}>
              Total Employees
            </span>
          </div>
        </div>

      </div>

      {/* ── Bottom Grid ── */}
      <div className={styles.bottomGrid}>

        {/* Recent Timesheets */}
        <div className={styles.tableCard}>
          <h3 className={styles.cardTitle}>
            <FileText size={18} /> Recent Timesheets
          </h3>

          {dashboard?.recentTimesheets?.length === 0 ? (
            <p className={styles.emptyText}>No timesheets yet</p>
          ) : (
            <div className={styles.table}>
              <div className={styles.tableHeader}>
                <span>Employee</span>
                <span>Week</span>
                <span>Hours</span>
                <span>Status</span>
              </div>
              {dashboard?.recentTimesheets?.slice(0, 6).map((ts) => (
                <div key={ts.id} className={styles.tableRow}>
                  <span className={styles.employeeCell}>
                    <div className={styles.miniAvatar}>
                      {ts.employeeName?.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <p className={styles.employeeName}>
                        {ts.employeeName}
                      </p>
                      <p className={styles.employeeEmail}>
                        {ts.employeeEmail}
                      </p>
                    </div>
                  </span>
                  <span className={styles.weekCell}>
                    {ts.weekStart}
                  </span>
                  <span className={styles.hoursCell}>
                    {ts.totalHours}h
                  </span>
                  <span>
                    <span
                      className={styles.statusPill}
                      style={{
                        background: getStatusColor(ts.status)
                      }}
                    >
                      {ts.status}
                    </span>
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent Leaves */}
        <div className={styles.tableCard}>
          <h3 className={styles.cardTitle}>
            <Calendar size={18} /> Recent Leaves
          </h3>

          {dashboard?.recentLeaves?.length === 0 ? (
            <p className={styles.emptyText}>No leaves yet</p>
          ) : (
            <div className={styles.table}>
              <div className={styles.tableHeader}>
                <span>Employee</span>
                <span>Type</span>
                <span>Days</span>
                <span>Status</span>
              </div>
              {dashboard?.recentLeaves?.slice(0, 6).map((leave) => (
                <div key={leave.id} className={styles.tableRow}>
                  <span className={styles.employeeCell}>
                    <div className={styles.miniAvatar}>
                      {leave.employeeName?.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <p className={styles.employeeName}>
                        {leave.employeeName}
                      </p>
                      <p className={styles.employeeEmail}>
                        {leave.employeeEmail}
                      </p>
                    </div>
                  </span>
                  <span className={styles.typeCell}>
                    {getLeaveTypeLabel(leave.leaveType)}
                  </span>
                  <span className={styles.daysCell}>
                    {leave.totalDays}d
                  </span>
                  <span>
                    <span
                      className={styles.statusPill}
                      style={{
                        background: getStatusColor(leave.status)
                      }}
                    >
                      {leave.status}
                    </span>
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>

    </Layout>
  )
}

export default AdminDashboard