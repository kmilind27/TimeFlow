//Format date: 2026-03-16 → Mon, 16 Mar 2026
export const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-IN', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  })
}

// Get week start (Monday) from any date
export const getWeekStart = (date = new Date()) => {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  d.setDate(diff)
  return d.toISOString().split('T')[0]  // returns YYYY-MM-DD
}

// Format: YYYY-MM-DD → DD/MM/YYYY
export const formatDisplayDate = (dateStr) => {
  if (!dateStr) return ''
  const [year, month, day] = dateStr.split('-')
  return `${day}/${month}/${year}`
}

// Get token from localStorage
export const getToken = () => localStorage.getItem('token')

// Decode JWT payload (without library)
export const decodeToken = (token) => {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

// Check if token is expired
export const isTokenExpired = (token) => {
  const decoded = decodeToken(token)
  if (!decoded) return true
  return decoded.exp * 1000 < Date.now()
}

// Get current week label: "27 Apr - 1 May 2024" (Mon-Fri only)
export const getWeekLabel = (weekStart) => {
  if (!weekStart) return ''
  const start = new Date(weekStart)
  const end = new Date(weekStart)
  end.setDate(end.getDate() + 4) // Friday is 4 days after Monday
  return `${start.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' })} - 
          ${end.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}`
}

// Status badge color
export const getStatusColor = (status) => {
  switch (status) {
    case 'APPROVED': return '#4caf50'
    case 'SUBMITTED': return '#2196f3'
    case 'REJECTED': return '#f44336'
    case 'DRAFT': return '#9e9e9e'
    case 'CANCELLED': return '#ff9800'
    default: return '#9e9e9e'
  }
}

// Leave type label
export const getLeaveTypeLabel = (type) => {
  switch (type) {
    case 'CASUAL': return 'Casual Leave'
    case 'SICK': return 'Sick Leave'
    case 'EARNED': return 'Earned Leave'
    case 'COMP_OFF': return 'Comp Off'
    default: return type
  }
}