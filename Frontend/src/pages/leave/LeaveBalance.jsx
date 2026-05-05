import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Umbrella, Heart, Star, RefreshCw, Calendar as CalendarIcon, PartyPopper, AlertCircle } from 'lucide-react'
import Layout from '../../components/Layout'
import AutoDismissMessage from '../../components/AutoDismissMessage'
import { getMyBalance, getHolidays } from '../../api/leaveApi'
import { getLeaveTypeLabel } from '../../utils/helpers'
import styles from './Leave.module.css'

const LeaveBalance = () => {
  const navigate = useNavigate()

  const [balances, setBalances] = useState([])
  const [holidays, setHolidays] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    fetchData()
  }, [])

  async function fetchData() {
    try {
      const [balanceRes, holidayRes] = await Promise.all([
        getMyBalance(),
        getHolidays(new Date().getFullYear())
      ])
      setBalances(balanceRes.data)
      setHolidays(holidayRes.data)
    } catch (err) {
      setError('Failed to load leave data')
    }
  }

  function getBalanceColor(type) {
    switch (type) {
      case 'CASUAL':  return '#1a237e'
      case 'SICK':    return '#c62828'
      case 'EARNED':  return '#2e7d32'
      case 'COMP_OFF': return '#e65100'
      default:        return '#555'
    }
  }

  function getBalanceIcon(type) {
    switch (type) {
      case 'CASUAL':  return <Umbrella size={24} />
      case 'SICK':    return <Heart size={24} />
      case 'EARNED':  return <Star size={24} />
      case 'COMP_OFF': return <RefreshCw size={24} />
      default:        return <CalendarIcon size={24} />
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
          <h2 className={styles.pageTitle}>Leave Balance</h2>
          <p className={styles.pageSubtitle}>
            Your available leave days for {new Date().getFullYear()}
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

      {/* ── Balance Cards ── */}
      <div className={styles.balanceGrid}>
        {balances.map((balance) => (
          <div key={balance.id} className={styles.balanceCard}>

            <div className={styles.balanceCardTop}>
              <span className={styles.balanceIcon}>
                {getBalanceIcon(balance.leaveType)}
              </span>
              <span
                className={styles.balanceType}
                style={{ color: getBalanceColor(balance.leaveType) }}
              >
                {getLeaveTypeLabel(balance.leaveType)}
              </span>
            </div>

            {/* Progress Bar */}
            <div className={styles.progressSection}>
              <div className={styles.progressBar}>
                <div
                  className={styles.progressFill}
                  style={{
                    width: `${(balance.usedDays / balance.totalDays) * 100}%`,
                    background: getBalanceColor(balance.leaveType)
                  }}
                />
              </div>
              <div className={styles.progressLabels}>
                <span>{balance.usedDays} used</span>
                <span>{balance.totalDays} total</span>
              </div>
            </div>

            {/* Remaining */}
            <div className={styles.remainingSection}>
              <span
                className={styles.remainingNumber}
                style={{ color: getBalanceColor(balance.leaveType) }}
              >
                {balance.remainingDays}
              </span>
              <span className={styles.remainingLabel}>
                days remaining
              </span>
            </div>

          </div>
        ))}
      </div>

      {/* ── Upcoming Holidays ── */}
      <div className={styles.sectionHeader}>
        <h3 className={styles.sectionTitle}><PartyPopper size={20} /> Upcoming Holidays</h3>
      </div>

      <div className={styles.holidayList}>
        {holidays
          .filter(h => new Date(h.holidayDate) >= new Date())
          .slice(0, 6)
          .map((holiday, index) => (
            <div key={index} className={styles.holidayCard}>
              <div className={styles.holidayDate}>
                <span className={styles.holidayDay}>
                  {new Date(holiday.holidayDate)
                    .toLocaleDateString('en-IN', { day: '2-digit' })}
                </span>
                <span className={styles.holidayMonth}>
                  {new Date(holiday.holidayDate)
                    .toLocaleDateString('en-IN', { month: 'short' })}
                </span>
              </div>
              <div className={styles.holidayInfo}>
                <span className={styles.holidayName}>
                  {holiday.holidayName || holiday.description}
                </span>
                <span className={styles.holidayWeekday}>
                  {new Date(holiday.holidayDate)
                    .toLocaleDateString('en-IN', { weekday: 'long' })}
                </span>
              </div>
            </div>
          ))}

        {holidays.filter(h =>
          new Date(h.holidayDate) >= new Date()
        ).length === 0 && (
          <div className={styles.emptyHolidays}>
            No upcoming holidays this year
          </div>
        )}
      </div>

    </Layout>
  )
}

export default LeaveBalance