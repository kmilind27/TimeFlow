import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Layout from '../../components/Layout'
import AutoDismissMessage from '../../components/AutoDismissMessage'
import { getMyTimesheets, submitTimesheet } from '../../api/timesheetApi'
import { getWeekStart, getWeekLabel, getStatusColor } from '../../utils/helpers'
import { Plus, Clipboard, Inbox, Calendar, Clock, MessageSquare } from 'lucide-react'
import styles from './Timesheet.module.css'

const TimesheetDashboard = () => {
  const navigate = useNavigate()

  const [timesheets, setTimesheets] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(null)
  const [successMsg, setSuccessMsg] = useState('')

  useEffect(() => {
    fetchTimesheets()
  }, [])

  async function fetchTimesheets() {
    try {
      
      setError('')
      const response = await getMyTimesheets()
      setTimesheets(response.data)
    } catch (err) {
      setError('Failed to load timesheets')
    } finally {
      
    }
  }

  async function handleSubmit(weekStart) {
    setSubmitting(weekStart)
    setSuccessMsg('')
    setError('')
    try {
      await submitTimesheet(weekStart)
      setSuccessMsg('Timesheet submitted successfully!')
      fetchTimesheets()
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Failed to submit timesheet'
      )
    } finally {
      setSubmitting(null)
    }
  }

  function getCurrentWeekStart() {
    return getWeekStart(new Date())
  }

  function hasCurrentWeekTimesheet() {
    const currentWeek = getCurrentWeekStart()
    return timesheets.some(t => t.weekStart === currentWeek)
  }

  

  return (
    <Layout>

      {/* ── Page Header ── */}
      <div className={styles.pageHeader}>
        <div>
          <h2 className={styles.pageTitle}>My Timesheets</h2>
          <p className={styles.pageSubtitle}>
            Track and manage your weekly hours
          </p>
        </div>
        <button
          className={styles.primaryBtn}
          onClick={() => navigate('/timesheet/log-entry')}
        >
          <Plus size={18} /> Log Hours
        </button>
      </div>

      {/* ── Messages ── */}
      <AutoDismissMessage
        message={error}
        type="error"
        onDismiss={() => setError('')}
      />
      <AutoDismissMessage
        message={successMsg}
        type="success"
        onDismiss={() => setSuccessMsg('')}
      />

      {/* ── Current Week Quick Card ── */}
      {!hasCurrentWeekTimesheet() && (
        <div className={styles.quickCard}>
          <div className={styles.quickCardLeft}>
            <span className={styles.quickCardIcon}><Clipboard size={32} /></span>
            <div>
              <p className={styles.quickCardTitle}>
                Current Week
              </p>
              <p className={styles.quickCardSubtitle}>
                {getWeekLabel(getCurrentWeekStart())}
              </p>
            </div>
          </div>
          <button
            className={styles.primaryBtn}
            onClick={() => navigate('/timesheet/log-entry')}
          >
            Start Logging
          </button>
        </div>
      )}

      {/* ── Timesheets List ── */}
      {timesheets.length === 0 ? (
        <div className={styles.emptyState}>
          <span className={styles.emptyIcon}><Inbox size={48} /></span>
          <h3>No timesheets yet</h3>
          <p>Start by logging your hours for this week</p>
          <button
            className={styles.primaryBtn}
            onClick={() => navigate('/timesheet/log-entry')}
          >
            Log Hours
          </button>
        </div>
      ) : (
        <div className={styles.timesheetList}>
          {timesheets.map((timesheet) => (
            <div key={timesheet.id} className={styles.timesheetCard}>

              {/* Card Header */}
              <div className={styles.cardHeader}>
                <div>
                  <p className={styles.weekLabel}>
                    <Calendar size={16} /> {getWeekLabel(timesheet.weekStart)}
                  </p>
                  <p className={styles.hoursLabel}>
                    <Clock size={16} /> {timesheet.totalHours} hours logged
                  </p>
                </div>
                <span
                  className={styles.statusBadge}
                  style={{ background: getStatusColor(timesheet.status) }}
                >
                  {timesheet.status}
                </span>
              </div>

              {/* Entries Preview */}
              {timesheet.entries && timesheet.entries.length > 0 && (
                <div className={styles.entriesPreview}>
                  {timesheet.entries.slice(0, 3).map((entry) => (
                    <div key={entry.id} className={styles.entryRow}>
                      <span className={styles.entryProject}>
                        {entry.projectName}
                      </span>
                      <span className={styles.entryDate}>
                        {entry.workDate}
                      </span>
                      <span className={styles.entryHours}>
                        {entry.hoursLogged}h
                      </span>
                    </div>
                  ))}
                  {timesheet.entries.length > 3 && (
                    <p className={styles.moreEntries}>
                      +{timesheet.entries.length - 3} more entries
                    </p>
                  )}
                </div>
              )}

              {/* Review Comment */}
              {timesheet.reviewComment && (
                <div className={styles.reviewComment}>
                  <MessageSquare size={16} /> <strong>Manager comment:</strong>{' '}
                  {timesheet.reviewComment}
                </div>
              )}

              {/* Card Actions */}
              <div className={styles.cardActions}>
                <button
                  className={styles.secondaryBtn}
                  onClick={() =>
                    navigate('/timesheet/log-entry', {
                      state: { weekStart: timesheet.weekStart }
                    })
                  }
                >
                  View Details
                </button>

                {timesheet.status === 'DRAFT' && (
                  <button
                    className={styles.primaryBtn}
                    onClick={() => handleSubmit(timesheet.weekStart)}
                    disabled={submitting === timesheet.weekStart}
                  >
                    {submitting === timesheet.weekStart
                      ? 'Submitting...'
                      : 'Submit for Approval'}
                  </button>
                )}

                {timesheet.status === 'REJECTED' && (
                  <button
                    className={styles.primaryBtn}
                    onClick={() =>
                      navigate('/timesheet/log-entry', {
                        state: { weekStart: timesheet.weekStart }
                      })
                    }
                  >
                    Update & Resubmit
                  </button>
                )}
              </div>

            </div>
          ))}
        </div>
      )}

    </Layout>
  )
}

export default TimesheetDashboard