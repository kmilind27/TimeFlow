import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Umbrella, Heart, Star, RefreshCw, Wallet } from 'lucide-react'
import Layout from '../../components/Layout'
import AutoDismissMessage from '../../components/AutoDismissMessage'
import { applyLeave, getMyBalance } from '../../api/leaveApi'
import { getLeaveTypeLabel } from '../../utils/helpers'
import styles from './Leave.module.css'

const LeaveApply = () => {
  const navigate = useNavigate()

  const [balances, setBalances] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [formData, setFormData] = useState({
    leaveType: '',
    fromDate: '',
    toDate: '',
    reason: ''
  })

  useEffect(() => {
    fetchBalances()
  }, [])

  async function fetchBalances() {
    try {
      const response = await getMyBalance()
      setBalances(response.data)
    } catch (err) {
      setError('Failed to load leave balances')
    } finally {
      setfalse(false)
    }
  }

  function handleChange(e) {
    setFormData({ ...formData, [e.target.name]: e.target.value })
    setError('')
  }

  function getSelectedBalance() {
    return balances.find(b => b.leaveType === formData.leaveType)
  }

  function validate() {
    if (!formData.leaveType) {
      setError('Please select leave type')
      return false
    }
    if (!formData.fromDate || !formData.toDate) {
      setError('Please select from and to dates')
      return false
    }
    if (formData.fromDate > formData.toDate) {
      setError('From date cannot be after to date')
      return false
    }
    const today = new Date().toISOString().split('T')[0]
    if (formData.fromDate < today) {
      setError('Cannot apply for past dates')
      return false
    }
    const balance = getSelectedBalance()
    if (balance && balance.remainingDays === 0) {
      setError(`No ${getLeaveTypeLabel(formData.leaveType)} days remaining`)
      return false
    }
    return true
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!validate()) return

    setLoading(true)
    setError('')

    try {
      await applyLeave(formData)
      setSuccess('Leave applied successfully!')
      setTimeout(() => navigate('/leave/history'), 1500)
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Failed to apply leave. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  const selectedBalance = getSelectedBalance()
  const today = new Date().toISOString().split('T')[0]

  return (
    <Layout>

      <div className={styles.pageHeader}>
        <div>
          <h2 className={styles.pageTitle}>Apply for Leave</h2>
          <p className={styles.pageSubtitle}>
            Submit a leave request for manager approval
          </p>
        </div>
        <button
          className={styles.secondaryBtn}
          onClick={() => navigate('/leave/history')}
        >
          <ArrowLeft size={16} /> My Requests
        </button>
      </div>

      <div className={styles.applyGrid}>

        {/* ── Form ── */}
        <div className={styles.formCard}>

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

          <form onSubmit={handleSubmit} className={styles.form}>

            <div className={styles.formGroup}>
              <label className={styles.label}>Leave Type</label>
              <select
                name="leaveType"
                value={formData.leaveType}
                onChange={handleChange}
                className={styles.select}
              >
                <option value="">Select leave type</option>
                <option value="CASUAL"><Umbrella size={14} /> Casual Leave</option>
                <option value="SICK"><Heart size={14} /> Sick Leave</option>
                <option value="EARNED"><Star size={14} /> Earned Leave</option>
                <option value="COMP_OFF"><RefreshCw size={14} /> Comp Off</option>
              </select>
            </div>

            {/* Balance indicator */}
            {selectedBalance && (
              <div className={styles.balanceIndicator}>
                <span>Available balance:</span>
                <strong
                  style={{
                    color: selectedBalance.remainingDays === 0
                      ? '#c62828' : '#2e7d32'
                  }}
                >
                  {selectedBalance.remainingDays} days
                </strong>
              </div>
            )}

            <div className={styles.formRow}>
              <div className={styles.formGroup}>
                <label className={styles.label}>From Date</label>
                <input
                  type="date"
                  name="fromDate"
                  value={formData.fromDate}
                  onChange={handleChange}
                  min={today}
                  className={styles.input}
                />
              </div>

              <div className={styles.formGroup}>
                <label className={styles.label}>To Date</label>
                <input
                  type="date"
                  name="toDate"
                  value={formData.toDate}
                  onChange={handleChange}
                  min={formData.fromDate || today}
                  className={styles.input}
                />
              </div>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>Reason</label>
              <textarea
                name="reason"
                value={formData.reason}
                onChange={handleChange}
                placeholder="Briefly describe the reason for leave"
                className={styles.textarea}
                rows={4}
              />
            </div>

            <button
              type="submit"
              className={styles.primaryBtn}
              disabled={loading}
            >
              {loading ? 'Submitting...' : 'Submit Leave Request'}
            </button>

          </form>
        </div>

        {/* ── Balance Summary ── */}
        <div className={styles.balanceSummaryCard}>
          <h3 className={styles.cardTitle}><Wallet size={18} /> Your Balance</h3>

          {false ? (
            <div className={styles.loadingContainer}>
              <div className={styles.spinner} />
            </div>
          ) : (
            <div className={styles.balanceSummaryList}>
              {balances.map((balance) => (
                <div key={balance.id} className={styles.balanceSummaryItem}>
                  <div className={styles.balanceSummaryLeft}>
                    <span className={styles.balanceSummaryType}>
                      {getLeaveTypeLabel(balance.leaveType)}
                    </span>
                    <span className={styles.balanceSummaryUsed}>
                      {balance.usedDays} of {balance.totalDays} used
                    </span>
                  </div>
                  <span
                    className={styles.balanceSummaryRemaining}
                    style={{
                      color: balance.remainingDays === 0
                        ? '#c62828' : '#2e7d32'
                    }}
                  >
                    {balance.remainingDays}d
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

export default LeaveApply