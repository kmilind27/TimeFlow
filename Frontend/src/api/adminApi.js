import axiosInstance from './axiosInstance'

//GET /admin/dashboard
export const getDashboard = () =>
  axiosInstance.get('/admin/dashboard')

// GET /admin/dashboard/employee-summary
export const getEmployeeSummary = () =>
  axiosInstance.get('/admin/dashboard/employee-summary')

//GET /admin/users
export const getAdminUsers = () =>
  axiosInstance.get('/admin/users')

// GET /admin/users/{id}
export const getAdminUserById = (id) =>
  axiosInstance.get(`/admin/users/${id}`)

// DELETE /admin/users/{id}
export const deleteAdminUser = (id) =>
  axiosInstance.delete(`/admin/users/${id}`)

// GET /admin/master/policies
export const getPolicies = () =>
  axiosInstance.get('/admin/master/policies')

// POST /admin/master/holidays
export const addAdminHoliday = (data) =>
  axiosInstance.post('/admin/master/holidays', data)

// GET /admin/reports/timesheet-compliance
export const getTimesheetCompliance = () =>
  axiosInstance.get('/admin/reports/timesheet-compliance')

// GET /admin/reports/leave-consumption
export const getLeaveConsumption = () =>
  axiosInstance.get('/admin/reports/leave-consumption')

//GET /admin/config/public
export const getPublicConfig = () =>
  axiosInstance.get('/admin/config/public')