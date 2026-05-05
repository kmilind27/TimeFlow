import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, CheckCircle, X, Clock, Ban, FileText, MessageSquare, Inbox, AlertCircle } from 'lucide-react'
import Layout from '../../components/Layout'
import AutoDismissMessage from '../../components/AutoDismissMessage'
import { getMyLeaves, cancelLeave } from '../../api/leaveApi'
import {
  getLeaveTypeLabel,
  getStatusColor,
  formatDisplayDate
} from '../../utils/helpers'
import styles from './Leave.module.css'
import ConfirmModal from '../../components/ConfirmModal'

const LeaveHistory = () => {
  const navigate = useNavigate()

  const [leaves, setLeaves] = useState([])
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [cancelling, setCancelling] = useState(null)

  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    leaveId: null
  })

  useEffect(() => {
    fetchLeaves()
  }, [])

  async function fetchLeaves() {
    try {
      const response = await getMyLeaves()
      setLeaves(response.data)
    } catch (err) {
      setError('Failed to load leave history')
    }
  }

   function handleCancelClick(leaveId) {
    setConfirmModal({ isOpen: true, leaveId })
  }

  async function handleConfirmCancel() {
    const leaveId = confirmModal.leaveId
    setConfirmModal({ isOpen: false, leaveId: null })

    setCancelling(leaveId)
    setError('')
    setSuccess('')

    try {
      await cancelLeave(leaveId)
      setSuccess('Leave cancelled successfully')
      fetchLeaves()
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Failed to cancel leave'
      )
    } finally {
      setCancelling(null)
    }
  }


  function getStatusIcon(status) {
    switch (status) {
      case 'APPROVED':   return <CheckCircle size={20} />
      case 'REJECTED':   return <X size={20} />
      case 'SUBMITTED':  return <Clock size={20} />
      case 'CANCELLED':  return <Ban size={20} />
      default:           return <FileText size={20} />
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
      <ConfirmModal
        isOpen={confirmModal.isOpen}
        title="Cancel Leave Request"
        message="Are you sure you want to cancel this leave request? This action cannot be undone."
        confirmText="Yes, Cancel Leave"
        cancelText="Keep Request"
        danger={true}
        onConfirm={handleConfirmCancel}
        onCancel={() =>
          setConfirmModal({ isOpen: false, leaveId: null })
        }
      />

      <div className={styles.pageHeader}>
        <div>
          <h2 className={styles.pageTitle}>Leave History</h2>
          <p className={styles.pageSubtitle}>
            All your leave requests
          </p>
        </div>
        <button
          className={styles.primaryBtn}
          onClick={() => navigate('/leave/apply')}
        >
          <Plus size={16} /> Apply Leave
        </button>
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

      {leaves.length === 0 ? (
        <div className={styles.emptyState}>
          <span className={styles.emptyIcon}><Inbox size={48} /></span>
          <h3>No leave requests yet</h3>
          <p>Apply for leave when you need time off</p>
          <button
            className={styles.primaryBtn}
            onClick={() => navigate('/leave/apply')}
          >
            Apply Now
          </button>
        </div>
      ) : (
        <div className={styles.leaveList}>
          {leaves.map((leave) => (
            <div key={leave.id} className={styles.leaveCard}>

              <div className={styles.leaveCardHeader}>
                <div className={styles.leaveTypeSection}>
                  <span className={styles.leaveTypeIcon}>
                    {getStatusIcon(leave.status)}
                  </span>
                  <div>
                    <p className={styles.leaveType}>
                      {getLeaveTypeLabel(leave.leaveType)}
                    </p>
                    <p className={styles.leaveDates}>
                      {formatDisplayDate(leave.fromDate)} →{' '}
                      {formatDisplayDate(leave.toDate)}
                    </p>
                  </div>
                </div>

                <div className={styles.leaveCardRight}>
                  <span
                    className={styles.statusBadge}
                    style={{ background: getStatusColor(leave.status) }}
                  >
                    {leave.status}
                  </span>
                  <span className={styles.leaveDaysCount}>
                    {leave.totalDays} day{leave.totalDays !== 1 ? 's' : ''}
                  </span>
                </div>
              </div>

              {/* Reason */}
              {leave.reason && (
                <div className={styles.leaveReason}>
                  <FileText size={16} /> {leave.reason}
                </div>
              )}

              {/* Manager Comment */}
              {leave.managerComment &&
                leave.managerComment !== 'No review comment yet' && (
                  <div className={styles.managerComment}>
                    <MessageSquare size={16} /> <strong>Manager:</strong> {leave.managerComment}
                  </div>
                )}

              {/* Cancel Button — only for SUBMITTED */}
              {leave.status === 'SUBMITTED' && (
                <button
                  className={styles.cancelBtn}
                  onClick={() => handleCancelClick(leave.id)}
                  disabled={cancelling === leave.id}
                >
                  {cancelling === leave.id
                    ? 'Cancelling...'
                    : 'Cancel Request'}
                </button>
              )}

            </div>
          ))}
        </div>
      )}

    </Layout>
  )
}

export default LeaveHistory