import axiosInstance from './axiosInstance'

// POST /leave/apply
export const applyLeave = (data) =>
  axiosInstance.post('/leave/apply', data)

// GET /leave/my-requests
export const getMyLeaves = () =>
  axiosInstance.get('/leave/my-requests')

// GET /leave/my-balance
export const getMyBalance = () =>
  axiosInstance.get('/leave/my-balance')

// PUT /leave/cancel/{leaveId}
export const cancelLeave = (leaveId) =>
  axiosInstance.put(`/leave/cancel/${leaveId}`)

// GET /leave/manager/pending
export const getPendingLeaves = () =>
  axiosInstance.get('/leave/manager/pending')

// PUT /leave/manager/review/{leaveId}
export const reviewLeave = (leaveId, data) =>
  axiosInstance.put(`/leave/manager/review/${leaveId}`, data)

// GET /leave/holidays?year=2026
export const getHolidays = (year = 2026) =>
  axiosInstance.get(`/leave/holidays?year=${year}`)

// POST /leave/holidays (admin only)
export const addHoliday = (data) =>
  axiosInstance.post('/leave/holidays', data)