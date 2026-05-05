import React, { useState, useEffect } from 'react'
import { CheckCircle, X, Check, Calendar, Clock, ChevronDown, ChevronUp } from 'lucide-react'
import Layout from '../../components/Layout'
import AutoDismissMessage from '../../components/AutoDismissMessage'
import { getPendingTimesheets, reviewTimesheet } from '../../api/timesheetApi'
import { getWeekLabel, formatDisplayDate } from '../../utils/helpers'
import styles from './Manager.module.css'

const PendingTimesheets = () => {

  const [timesheets, setTimesheets] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // ✅ Track which timesheet's review panel is open
  const [activeReview, setActiveReview] = useState(null)
  const [reviewData, setReviewData] = useState({
    action: '',
    comment: ''
  })
  const [reviewing, setReviewing] = useState(false)
  const [reviewError, setReviewError] = useState('')

  // ✅ Track which timesheet's entries are expanded
  const [expandedId, setExpandedId] = useState(null)

  useEffect(() => {
    fetchPendingTimesheets()
  }, [])

  async function fetchPendingTimesheets() {
    try {
      
      setError('')
      const response = await getPendingTimesheets()
      setTimesheets(response.data)
    } catch (err) {
      setError('Failed to load pending timesheets')
    } finally {
      
    }
  }

  function handleReviewOpen(timesheetId) {
    setActiveReview(timesheetId)
    setReviewData({ action: '', comment: '' })
    setReviewError('')
  }

  function handleReviewClose() {
    setActiveReview(null)
    setReviewData({ action: '', comment: '' })
    setReviewError('')
  }

  function handleReviewChange(e) {
    setReviewData({ ...reviewData, [e.target.name]: e.target.value })
    setReviewError('')
  }

  function validateReview() {
    if (!reviewData.action) {
      setReviewError('Please select approve or reject')
      return false
    }
    if (reviewData.action === 'REJECTED' && !reviewData.comment.trim()) {
      setReviewError('Comment is required when rejecting')
      return false
    }
    return true
  }

  async function handleReviewSubmit(timesheetId) {
    if (!validateReview()) return

    setReviewing(true)
    setReviewError('')
    setSuccess('')
    setError('')

    try {
      await reviewTimesheet(timesheetId, reviewData)
      setSuccess(
        `Timesheet ${reviewData.action === 'APPROVED'
          ? 'approved' : 'rejected'} successfully!`
      )
      handleReviewClose()
      fetchPendingTimesheets()
    } catch (err) {
      setReviewError(
        err.response?.data?.message ||
        'Failed to submit review'
      )
    } finally {
      setReviewing(false)
    }
  }

  function toggleExpand(id) {
    setExpandedId(expandedId === id ? null : id)
  }

  

  return (
    <Layout>

      {/* ── Page Header ── */}
      <div className={styles.pageHeader}>
        <div>
          <h2 className={styles.pageTitle}>Pending Timesheets</h2>
          <p className={styles.pageSubtitle}>
            Review and approve submitted timesheets
          </p>
        </div>
        <div className={styles.countBadge}>
          {timesheets.length} pending
        </div>
      </div>

      <AutoDismissMessage
        message={error}
        type="error"
        onDismiss={() => setError('')}
      />
      <AutoDismissMessage
        message={success}
        type="success"
        onDismiss={() => setSuccess('')}
      />

      {/* ── Empty State ── */}
      {timesheets.length === 0 ? (
        <div className={styles.emptyState}>
          <span className={styles.emptyIcon}><CheckCircle size={48} /></span>
          <h3>All caught up!</h3>
          <p>No timesheets pending review</p>
        </div>
      ) : (
        <div className={styles.cardList}>
          {timesheets.map((timesheet) => (
            <div key={timesheet.id} className={styles.reviewCard}>

              {/* ── Card Header ── */}
              <div className={styles.cardHeader}>
                <div className={styles.employeeInfo}>
                  <div className={styles.employeeAvatar}>
                    {timesheet.employeeName?.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <p className={styles.employeeName}>
                      {timesheet.employeeName}
                    </p>
                    <p className={styles.employeeEmail}>
                      {timesheet.employeeEmail}
                    </p>
                  </div>
                </div>
                <div className={styles.cardHeaderRight}>
                  <span className={styles.submittedBadge}>
                    SUBMITTED
                  </span>
                  <span className={styles.totalHours}>
                    <Clock size={14} /> {timesheet.totalHours}h total
                  </span>
                </div>
              </div>

              {/* ── Week Info ── */}
              <div className={styles.weekInfo}>
                <Calendar size={14} /> {getWeekLabel(timesheet.weekStart)}
              </div>

              {/* ── Entries Toggle ── */}
              <button
                className={styles.toggleEntriesBtn}
                onClick={() => toggleExpand(timesheet.id)}
              >
                {expandedId === timesheet.id
                  ? <><ChevronUp size={16} /> Hide entries</>
                  : <><ChevronDown size={16} /> View entries ({timesheet.entries?.length || 0})</>}
              </button>

              {/* ── Entries List ── */}
              {expandedId === timesheet.id && (
                <div className={styles.entriesTable}>
                  <div className={styles.entriesHeader}>
                    <span>Project</span>
                    <span>Date</span>
                    <span>Hours</span>
                    <span>Task</span>
                  </div>
                  {timesheet.entries?.map((entry) => (
                    <div key={entry.id} className={styles.entriesRow}>
                      <span>{entry.projectName}</span>
                      <span>{formatDisplayDate(entry.workDate)}</span>
                      <span className={styles.entryHours}>
                        {entry.hoursLogged}h
                      </span>
                      <span className={styles.entryTask}>
                        {entry.taskSummary || '—'}
                      </span>
                    </div>
                  ))}
                </div>
              )}

              {/* ── Review Panel ── */}
              {activeReview === timesheet.id ? (
                <div className={styles.reviewPanel}>

                  <AutoDismissMessage
                    message={reviewError}
                    type="error"
                    onDismiss={() => setReviewError('')}
                  />

                  {/* Action Buttons */}
                  <div className={styles.actionButtons}>
                    <button
                      className={`${styles.actionBtn} ${
                        reviewData.action === 'APPROVED'
                          ? styles.approveActive
                          : styles.approveBtn
                      }`}
                      onClick={() =>
                        setReviewData({
                          ...reviewData,
                          action: 'APPROVED'
                        })
                      }
                    >
                      <Check size={16} /> Approve
                    </button>
                    <button
                      className={`${styles.actionBtn} ${
                        reviewData.action === 'REJECTED'
                          ? styles.rejectActive
                          : styles.rejectBtn
                      }`}
                      onClick={() =>
                        setReviewData({
                          ...reviewData,
                          action: 'REJECTED'
                        })
                      }
                    >
                      <X size={16} /> Reject
                    </button>
                  </div>

                  {/* Comment */}
                  <textarea
                    name="comment"
                    value={reviewData.comment}
                    onChange={handleReviewChange}
                    placeholder={
                      reviewData.action === 'REJECTED'
                        ? 'Comment is required for rejection...'
                        : 'Add a comment (optional)...'
                    }
                    className={styles.commentBox}
                    rows={3}
                  />

                  {/* Submit / Cancel */}
                  <div className={styles.reviewActions}>
                    <button
                      className={styles.cancelReviewBtn}
                      onClick={handleReviewClose}
                    >
                      Cancel
                    </button>
                    <button
                      className={styles.submitReviewBtn}
                      onClick={() => handleReviewSubmit(timesheet.id)}
                      disabled={reviewing}
                    >
                      {reviewing ? 'Submitting...' : 'Submit Review'}
                    </button>
                  </div>

                </div>
              ) : (
                <div className={styles.cardFooter}>
                  <button
                    className={styles.reviewBtn}
                    onClick={() => handleReviewOpen(timesheet.id)}
                  >
                    Review Timesheet
                  </button>
                </div>
              )}

            </div>
          ))}
        </div>
      )}

    </Layout>
  )
}

export default PendingTimesheets