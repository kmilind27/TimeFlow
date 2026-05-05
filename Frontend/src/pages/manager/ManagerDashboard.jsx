import React, { useState, useEffect } from 'react'
import { RefreshCw, Clock, Inbox, FileText, Calendar, AlertCircle } from 'lucide-react'
import Layout from '../../components/Layout'
import { getPendingTimesheets } from '../../api/timesheetApi'
import { getPendingLeaves } from '../../api/leaveApi'
import { getLeaveTypeLabel, getStatusColor, getWeekLabel } from '../../utils/helpers'
import styles from './Manager.module.css'

const ManagerDashboard = () => {
  const [dashboard, setDashboard] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchDashboard()
  }, [])

  async function fetchDashboard() {
    try {
      setError('')
      
      const [timesheetsRes, leavesRes] = await Promise.all([
        getPendingTimesheets(),
        getPendingLeaves()
      ])

      const pendingTimesheets = timesheetsRes.data || []
      const pendingLeaves = leavesRes.data || []

      const dashboardData = {
        pendingTimesheets: pendingTimesheets.length,
        pendingLeaves: pendingLeaves.length,
        recentTimesheets: pendingTimesheets.slice(0, 5),
        recentLeaves: pendingLeaves.slice(0, 5)
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
          <h2 className={styles.pageTitle}>Manager Dashboard</h2>
          <p className={styles.pageSubtitle}>
            Overview of pending approvals
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

      </div>

      {/* ── Bottom Grid ── */}
      <div className={styles.bottomGrid}>

        {/* Recent Timesheets */}
        <div className={styles.tableCard}>
          <h3 className={styles.cardTitle}>
            <FileText size={18} /> Recent Timesheets
          </h3>

          {dashboard?.recentTimesheets?.length === 0 ? (
            <p className={styles.emptyText}>No pending timesheets</p>
          ) : (
            <div className={styles.table}>
              <div className={styles.tableHeader}>
                <span>Employee</span>
                <span>Week</span>
                <span>Hours</span>
              </div>
              {dashboard?.recentTimesheets?.map((ts) => (
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
                    {getWeekLabel(ts.weekStart)}
                  </span>
                  <span className={styles.hoursCell}>
                    {ts.totalHours}h
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
            <p className={styles.emptyText}>No pending leaves</p>
          ) : (
            <div className={styles.table}>
              <div className={styles.tableHeader}>
                <span>Employee</span>
                <span>Type</span>
                <span>Days</span>
              </div>
              {dashboard?.recentLeaves?.map((leave) => (
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
                </div>
              ))}
            </div>
          )}
        </div>

      </div>

    </Layout>
  )
}

export default ManagerDashboard
