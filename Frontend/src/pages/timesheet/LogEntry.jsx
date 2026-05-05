import React, { useState, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import Layout from '../../components/Layout'
import AutoDismissMessage from '../../components/AutoDismissMessage'
import {
  getProjects,
  logEntry,
  getWeeklyTimesheet,
  submitTimesheet
} from '../../api/timesheetApi'
import { getWeekStart, getWeekLabel, getStatusColor } from '../../utils/helpers'
import { ArrowLeft, Plus, BarChart3, Lock, CheckCircle, Clock, X } from 'lucide-react'
import styles from './Timesheet.module.css'

const LogEntry = () => {
  const location = useLocation()
  const navigate = useNavigate()

  // ✅ If coming from dashboard with a specific week
  const initialWeek = location.state?.weekStart || getWeekStart(new Date())

  const [selectedWeek, setSelectedWeek] = useState(initialWeek)
  const [projects, setProjects] = useState([])
  const [timesheet, setTimesheet] = useState(null)
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  const [formData, setFormData] = useState({
    projectId: '',
    workDate: '',
    hoursLogged: '',
    taskSummary: ''
  })
  const [formError, setFormError] = useState('')
  const [formLoading, setFormLoading] = useState(false)

  useEffect(() => {
    fetchProjects()
  }, [])

  useEffect(() => {
    fetchWeeklyTimesheet()
  }, [selectedWeek])

  async function fetchProjects() {
    try {
      const response = await getProjects()
      setProjects(response.data)
    } catch (err) {
      setError('Failed to load projects')
    }
  }

  async function fetchWeeklyTimesheet() {
    try {
      
      setError('')
      const response = await getWeeklyTimesheet(selectedWeek)
      setTimesheet(response.data)
    } catch (err) {
      // ✅ 404 means no timesheet yet for this week — that's fine
      if (err.response?.status === 404) {
        setTimesheet(null)
      } else {
        setError('Failed to load timesheet')
      }
    } finally {
      
    }
  }

  function handleFormChange(e) {
    setFormData({ ...formData, [e.target.name]: e.target.value })
    setFormError('')
  }

  function validateForm() {
    if (!formData.projectId) {
      setFormError('Please select a project')
      return false
    }
    if (!formData.workDate) {
      setFormError('Please select a work date')
      return false
    }
    if (!formData.hoursLogged) {
      setFormError('Please enter hours logged')
      return false
    }
    const hours = parseFloat(formData.hoursLogged)
    if (hours < 0.5 || hours > 12) {
      setFormError('Hours must be between 0.5 and 12')
      return false
    }
    // ✅ Cannot log future dates
    if (formData.workDate > new Date().toISOString().split('T')[0]) {
      setFormError('Cannot log hours for future dates')
      return false
    }
    return true
  }

  async function handleLogEntry(e) {
    e.preventDefault()
    if (!validateForm()) return

    setFormLoading(true)
    setFormError('')
    setSuccessMsg('')

    try {
      await logEntry({
        projectId: parseInt(formData.projectId),
        workDate: formData.workDate,
        hoursLogged: parseFloat(formData.hoursLogged),
        taskSummary: formData.taskSummary
      })

      setSuccessMsg('Hours logged successfully!')
      setFormData({
        projectId: '',
        workDate: '',
        hoursLogged: '',
        taskSummary: ''
      })
      fetchWeeklyTimesheet()

    } catch (err) {
      setFormError(
        err.response?.data?.message ||
        'Failed to log entry. Please try again.'
      )
    } finally {
      setFormLoading(false)
    }
  }

  async function handleSubmitTimesheet() {
    setSubmitting(true)
    setError('')
    setSuccessMsg('')
    try {
      await submitTimesheet(selectedWeek)
      setSuccessMsg('Timesheet submitted for approval!')
      fetchWeeklyTimesheet()
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Failed to submit timesheet'
      )
    } finally {
      setSubmitting(false)
    }
  }

  function getWeekDates() {
    const start = new Date(selectedWeek)
    const dates = []
    for (let i = 0; i < 7; i++) {
      const date = new Date(start)
      date.setDate(start.getDate() + i)
      const dayOfWeek = date.getDay()
      // Skip Saturday (6) and Sunday (0)
      if (dayOfWeek !== 0 && dayOfWeek !== 6) {
        dates.push(date.toISOString().split('T')[0])
      }
    }
    return dates
  }

  function getPreviousWeek() {
    const date = new Date(selectedWeek)
    date.setDate(date.getDate() - 7)
    return getWeekStart(date)
  }

  function getNextWeek() {
    const date = new Date(selectedWeek)
    date.setDate(date.getDate() + 7)
    return getWeekStart(date)
  }

  function isCurrentWeek() {
    return selectedWeek === getWeekStart(new Date())
  }

  function isPastWeek() {
    return selectedWeek < getWeekStart(new Date())
  }

  const canEdit = (!timesheet ||
    timesheet.status === 'DRAFT' ||
    timesheet.status === 'REJECTED') &&
    !isPastWeek()

  return (
    <Layout>

      {/* ── Page Header ── */}
      <div className={styles.pageHeader}>
        <div>
          <h2 className={styles.pageTitle}>Log Hours</h2>
          <p className={styles.pageSubtitle}>
            Record your daily work hours
          </p>
        </div>
        <button
          className={styles.secondaryBtn}
          onClick={() => navigate('/timesheet/dashboard')}
        >
          <ArrowLeft size={18} /> My Timesheets
        </button>
      </div>

      {/* ── Week Navigator ── */}
      <div className={styles.weekNavigator}>
        <button
          className={styles.weekNavBtn}
          onClick={() => setSelectedWeek(getPreviousWeek())}
        >
          ← Prev
        </button>
        <div className={styles.weekInfo}>
          <span className={styles.weekLabel}>
            {getWeekLabel(selectedWeek)}
          </span>
          {isCurrentWeek() && (
            <span className={styles.currentWeekBadge}>
              Current Week
            </span>
          )}
        </div>
        <button
          className={styles.weekNavBtn}
          onClick={() => setSelectedWeek(getNextWeek())}
          disabled={isCurrentWeek()}
        >
          Next →
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

      <div className={styles.logEntryGrid}>

        {/* ── Left — Log Entry Form ── */}
        {canEdit && (
          <div className={styles.formCard}>
            <h3 className={styles.cardTitle}><Plus size={18} /> Add Entry</h3>

            <AutoDismissMessage
              message={formError}
              type="error"
              onDismiss={() => setFormError('')}
            />

            <form onSubmit={handleLogEntry} className={styles.form}>

              <div className={styles.formGroup}>
                <label className={styles.label}>Project</label>
                <select
                  name="projectId"
                  value={formData.projectId}
                  onChange={handleFormChange}
                  className={styles.select}
                >
                  <option value="">Select project</option>
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.projectName}
                    </option>
                  ))}
                </select>
              </div>

              <div className={styles.formGroup}>
                <label className={styles.label}>Work Date</label>
                <select
                  name="workDate"
                  value={formData.workDate}
                  onChange={handleFormChange}
                  className={styles.select}
                >
                  <option value="">Select date</option>
                  {getWeekDates().map((date) => (
                    <option
                      key={date}
                      value={date}
                      disabled={
                        date > new Date().toISOString().split('T')[0]
                      }
                    >
                      {new Date(date).toLocaleDateString('en-IN', {
                        weekday: 'short',
                        day: '2-digit',
                        month: 'short'
                      })}
                      {date > new Date().toISOString().split('T')[0]
                        ? ' (future)'
                        : ''}
                    </option>
                  ))}
                </select>
              </div>

              <div className={styles.formGroup}>
                <label className={styles.label}>
                  Hours Logged (0.5 - 12)
                </label>
                <input
                  type="number"
                  name="hoursLogged"
                  value={formData.hoursLogged}
                  onChange={handleFormChange}
                  placeholder="e.g. 8"
                  min="0.5"
                  max="12"
                  step="0.5"
                  className={styles.input}
                />
              </div>

              <div className={styles.formGroup}>
                <label className={styles.label}>
                  Task Summary (optional)
                </label>
                <textarea
                  name="taskSummary"
                  value={formData.taskSummary}
                  onChange={handleFormChange}
                  placeholder="What did you work on?"
                  className={styles.textarea}
                  rows={3}
                />
              </div>

              <button
                type="submit"
                className={styles.primaryBtn}
                disabled={formLoading}
              >
                {formLoading ? 'Logging...' : 'Log Hours'}
              </button>

            </form>
          </div>
        )}

        {/* ── Past Week Message ── */}
        {isPastWeek() && !canEdit && (
          <div className={styles.formCard}>
            <div className={styles.statusMessage}>
              <Lock size={16} /> Cannot log hours for past weeks. You can only view past timesheets.
            </div>
          </div>
        )}

        {/* ── Right — Weekly Summary ── */}
        <div className={styles.summaryCard}>
          <div className={styles.summaryHeader}>
            <h3 className={styles.cardTitle}><BarChart3 size={18} /> Weekly Summary</h3>
            {timesheet && (
              <span
                className={styles.statusBadge}
                style={{ background: getStatusColor(timesheet.status) }}
              >
                {timesheet.status}
              </span>
            )}
          </div>

          {/* Total Hours */}
          <div className={styles.totalHours}>
            <span className={styles.totalHoursNumber}>
              {timesheet?.totalHours || 0}
            </span>
            <span className={styles.totalHoursLabel}>
              hours this week
            </span>
          </div>

          {/* Entries List */}
          {loading ? (
            <div className={styles.loadingContainer}>
              <div className={styles.spinner} />
            </div>
          ) : timesheet?.entries && timesheet.entries.length > 0 ? (
            <div className={styles.entriesList}>
              {timesheet.entries.map((entry) => (
                <div key={entry.id} className={styles.entryItem}>
                  <div className={styles.entryItemLeft}>
                    <span className={styles.entryProject}>
                      {entry.projectName}
                    </span>
                    <span className={styles.entryDate}>
                      {new Date(entry.workDate).toLocaleDateString(
                        'en-IN',
                        { weekday: 'short', day: '2-digit', month: 'short' }
                      )}
                    </span>
                    {entry.taskSummary && (
                      <span className={styles.entryTask}>
                        {entry.taskSummary}
                      </span>
                    )}
                  </div>
                  <span className={styles.entryHoursBig}>
                    {entry.hoursLogged}h
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div className={styles.emptyEntries}>
              <p>No entries for this week yet</p>
            </div>
          )}

          {/* Submit Button */}
          {timesheet?.status === 'DRAFT' &&
            timesheet?.entries?.length > 0 && (
              <button
                className={styles.submitTimesheetBtn}
                onClick={handleSubmitTimesheet}
                disabled={submitting}
              >
                {submitting
                  ? 'Submitting...'
                  : <><CheckCircle size={18} /> Submit for Approval</>}
              </button>
            )}

          {/* Status Messages */}
          {timesheet?.status === 'SUBMITTED' && (
            <div className={styles.statusMessage}>
              <Clock size={16} /> Awaiting manager approval
            </div>
          )}
          {timesheet?.status === 'APPROVED' && (
            <div className={styles.statusMessageSuccess}>
              <CheckCircle size={16} /> Approved by manager
            </div>
          )}
          {timesheet?.status === 'REJECTED' && (
            <div className={styles.statusMessageError}>
              <X size={16} /> Rejected — {timesheet.reviewComment}
            </div>
          )}

        </div>
      </div>

    </Layout>
  )
}

export default LogEntry