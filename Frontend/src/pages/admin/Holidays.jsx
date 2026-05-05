import React, { useState, useEffect } from 'react'
import { X, Plus, Calendar, AlertCircle, CheckCircle } from 'lucide-react'
import Layout from '../../components/Layout'
import { getHolidays, addHoliday } from '../../api/leaveApi'
import { formatDisplayDate } from '../../utils/helpers'
import styles from './Admin.module.css'

const Holidays = () => {
  const [holidays, setHolidays] = useState([])
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [showForm, setShowForm] = useState(false)
  const [formData, setFormData] = useState({
    holidayDate: '',
    holidayName: '',
    holidayType: ''
  })
  const [formLoading, setFormLoading] = useState(false)
  const [formError, setFormError] = useState('')

  const currentYear = new Date().getFullYear()

  useEffect(() => {
    fetchHolidays()
  }, [])

  async function fetchHolidays() {
    try {
      setError('')
      const response = await getHolidays(currentYear)
      setHolidays(response.data)
    } catch (err) {
      setError('Failed to load holidays')
    }
  }

  function handleChange(e) {
    setFormData({ ...formData, [e.target.name]: e.target.value })
    setFormError('')
  }

  function validateForm() {
    if (!formData.holidayDate) {
      setFormError('Please select a date')
      return false
    }
    if (!formData.holidayName) {
      setFormError('Please enter holiday name')
      return false
    }
    if (!formData.holidayType) {
      setFormError('Please select holiday type')
      return false
    }
    return true
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!validateForm()) return

    setFormLoading(true)
    setFormError('')
    setSuccess('')

    try {
      await addHoliday(formData)
      setSuccess('Holiday added successfully!')
      setFormData({ holidayDate: '', holidayName: '', holidayType: '' })
      setShowForm(false)
      fetchHolidays()
    } catch (err) {
      setFormError(
        err.response?.data?.message ||
        'Failed to add holiday. Please try again.'
      )
    } finally {
      setFormLoading(false)
    }
  }

  function getMonthName(dateStr) {
    const date = new Date(dateStr)
    return date.toLocaleDateString('en-IN', { month: 'short' })
  }

  function getDay(dateStr) {
    const date = new Date(dateStr)
    return date.toLocaleDateString('en-IN', { day: '2-digit' })
  }

  function getWeekday(dateStr) {
    const date = new Date(dateStr)
    return date.toLocaleDateString('en-IN', { weekday: 'long' })
  }

  function isPastHoliday(dateStr) {
    const holidayDate = new Date(dateStr)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return holidayDate < today
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
          <h2 className={styles.pageTitle}>Holidays {currentYear}</h2>
          <p className={styles.pageSubtitle}>
            Manage company holidays
          </p>
        </div>
        <button
          className={styles.refreshBtn}
          onClick={() => setShowForm(!showForm)}
        >
          {showForm ? <><X size={16} /> Cancel</> : <><Plus size={16} /> Add Holiday</>}
        </button>
      </div>

      {error && (
        <div className={styles.errorBox}><AlertCircle size={16} /> {error}</div>
      )}
      {success && (
        <div className={styles.successBox}><CheckCircle size={16} /> {success}</div>
      )}

      {/* ── Add Holiday Form ── */}
      {showForm && (
        <div className={styles.formCard}>
          <h3 className={styles.cardTitle}>Add New Holiday</h3>

          {formError && (
            <div className={styles.errorBox}><AlertCircle size={16} /> {formError}</div>
          )}

          <form onSubmit={handleSubmit} className={styles.holidayForm}>
            <div className={styles.formRow}>
              <div className={styles.formGroup}>
                <label className={styles.label}>Holiday Date</label>
                <input
                  type="date"
                  name="holidayDate"
                  value={formData.holidayDate}
                  onChange={handleChange}
                  className={styles.input}
                  required
                />
              </div>

              <div className={styles.formGroup}>
                <label className={styles.label}>Holiday Name</label>
                <input
                  type="text"
                  name="holidayName"
                  value={formData.holidayName}
                  onChange={handleChange}
                  placeholder="e.g. Independence Day"
                  className={styles.input}
                  required
                />
              </div>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>Holiday Type</label>
              <select
                name="holidayType"
                value={formData.holidayType}
                onChange={handleChange}
                className={styles.select}
                required
              >
                <option value="">Select type</option>
                <option value="National">National</option>
                <option value="Festival">Festival</option>
                <option value="Company">Company</option>
                <option value="Optional">Optional</option>
              </select>
            </div>

            <button
              type="submit"
              className={styles.primaryBtn}
              disabled={formLoading}
            >
              {formLoading ? 'Adding...' : 'Add Holiday'}
            </button>
          </form>
        </div>
      )}

      {/* ── Holidays List ── */}
      <div className={styles.holidayGrid}>
        {holidays.length === 0 ? (
          <div className={styles.emptyState}>
            <span className={styles.emptyIcon}><Calendar size={48} /></span>
            <h3>No holidays added yet</h3>
            <p>Add holidays for {currentYear}</p>
          </div>
        ) : (
          holidays.map((holiday, index) => (
            <div 
              key={index} 
              className={`${styles.holidayCard} ${isPastHoliday(holiday.holidayDate) ? styles.pastHoliday : ''}`}
            >
              <div className={styles.holidayDateBox}>
                <span className={styles.holidayDay}>
                  {getDay(holiday.holidayDate)}
                </span>
                <span className={styles.holidayMonth}>
                  {getMonthName(holiday.holidayDate)}
                </span>
              </div>
              <div className={styles.holidayInfo}>
                <span className={styles.holidayName}>
                  {holiday.holidayName}
                </span>
                <span className={styles.holidayWeekday}>
                  {getWeekday(holiday.holidayDate)}
                </span>
              </div>
            </div>
          ))
        )}
      </div>

    </Layout>
  )
}

export default Holidays
