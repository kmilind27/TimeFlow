import React, { useState, useEffect } from 'react'
import { CheckCircle, Check, X, Umbrella, Heart, Star, RefreshCw, Calendar as CalendarIcon, FileText, AlertCircle } from 'lucide-react'
import Layout from '../../components/Layout'
import AutoDismissMessage from '../../components/AutoDismissMessage'
import { getPendingLeaves, reviewLeave } from '../../api/leaveApi'
import {
  getLeaveTypeLabel,
  formatDisplayDate
} from '../../utils/helpers'
import styles from './Manager.module.css'

const PendingLeaves = () => {

  const [leaves, setLeaves] = useState([])
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [activeReview, setActiveReview] = useState(null)
  const [reviewData, setReviewData] = useState({
    action: '',
    comment: ''
  })
  const [reviewing, setReviewing] = useState(false)
  const [reviewError, setReviewError] = useState('')

  useEffect(() => {
    fetchPendingLeaves()
  }, [])

  async function fetchPendingLeaves() {
    try {
      setError('')
      const response = await getPendingLeaves()
      setLeaves(response.data)
    } catch (err) {
      setError('Failed to load pending leaves')
    }
  }

  function handleReviewOpen(leaveId) {
    setActiveReview(leaveId)
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
    if (
      reviewData.action === 'REJECTED' &&
      !reviewData.comment.trim()
    ) {
      setReviewError('Comment is required when rejecting')
      return false
    }
    return true
  }

  async function handleReviewSubmit(leaveId) {
    if (!validateReview()) return

    setReviewing(true)
    setReviewError('')
    setSuccess('')
    setError('')

    try {
      await reviewLeave(leaveId, reviewData)
      setSuccess(
        `Leave ${reviewData.action === 'APPROVED'
          ? 'approved' : 'rejected'} successfully!`
      )
      handleReviewClose()
      fetchPendingLeaves()
    } catch (err) {
      setReviewError(
        err.response?.data?.message ||
        'Failed to submit review'
      )
    } finally {
      setReviewing(false)
    }
  }

  function getLeaveTypeIcon(type) {
    switch (type) {
      case 'CASUAL':   return <Umbrella size={20} />
      case 'SICK':     return <Heart size={20} />
      case 'EARNED':   return <Star size={20} />
      case 'COMP_OFF': return <RefreshCw size={20} />
      default:         return <CalendarIcon size={20} />
    }
  }

  if (error) {
    return (
      <Layout>
        <div className={styles.errorBox}><AlertCircle size={16} /> {error}</div>
      </Layout>
    )
  }

  return (
    <Layout>

      {/* ── Page Header ── */}
      <div className={styles.pageHeader}>
        <div>
          <h2 className={styles.pageTitle}>Pending Leaves</h2>
          <p className={styles.pageSubtitle}>
            Review and approve leave requests
          </p>
        </div>
        <div className={styles.countBadge}>
          {leaves.length} pending
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
      {leaves.length === 0 ? (
        <div className={styles.emptyState}>
          <span className={styles.emptyIcon}><CheckCircle size={48} /></span>
          <h3>All caught up!</h3>
          <p>No leave requests pending review</p>
        </div>
      ) : (
        <div className={styles.cardList}>
          {leaves.map((leave) => (
            <div key={leave.id} className={styles.reviewCard}>

              {/* ── Card Header ── */}
              <div className={styles.cardHeader}>
                <div className={styles.employeeInfo}>
                  <div className={styles.employeeAvatar}>
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
                </div>
                <span className={styles.submittedBadge}>
                  SUBMITTED
                </span>
              </div>

              {/* ── Leave Details ── */}
              <div className={styles.leaveDetails}>
                <div className={styles.leaveDetailItem}>
                  <span className={styles.leaveDetailIcon}>
                    {getLeaveTypeIcon(leave.leaveType)}
                  </span>
                  <div>
                    <p className={styles.leaveDetailLabel}>
                      Leave Type
                    </p>
                    <p className={styles.leaveDetailValue}>
                      {getLeaveTypeLabel(leave.leaveType)}
                    </p>
                  </div>
                </div>

                <div className={styles.leaveDetailItem}>
                  <span className={styles.leaveDetailIcon}><CalendarIcon size={20} /></span>
                  <div>
                    <p className={styles.leaveDetailLabel}>Duration</p>
                    <p className={styles.leaveDetailValue}>
                      {formatDisplayDate(leave.fromDate)} →{' '}
                      {formatDisplayDate(leave.toDate)}
                    </p>
                  </div>
                </div>

                <div className={styles.leaveDetailItem}>
                  <span className={styles.leaveDetailIcon}><CalendarIcon size={20} /></span>
                  <div>
                    <p className={styles.leaveDetailLabel}>Days</p>
                    <p className={styles.leaveDetailValue}>
                      {leave.totalDays} day
                      {leave.totalDays !== 1 ? 's' : ''}
                    </p>
                  </div>
                </div>
              </div>

              {/* ── Reason ── */}
              {leave.reason &&
                leave.reason !== 'No reason specified' && (
                  <div className={styles.reasonBox}>
                    <FileText size={16} /> <strong>Reason:</strong> {leave.reason}
                  </div>
                )}

              {/* ── Review Panel ── */}
              {activeReview === leave.id ? (
                <div className={styles.reviewPanel}>

                  <AutoDismissMessage
                    message={reviewError}
                    type="error"
                    onDismiss={() => setReviewError('')}
                  />

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

                  <div className={styles.reviewActions}>
                    <button
                      className={styles.cancelReviewBtn}
                      onClick={handleReviewClose}
                    >
                      Cancel
                    </button>
                    <button
                      className={styles.submitReviewBtn}
                      onClick={() => handleReviewSubmit(leave.id)}
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
                    onClick={() => handleReviewOpen(leave.id)}
                  >
                    Review Request
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

export default PendingLeaves