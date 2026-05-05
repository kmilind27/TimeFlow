import axiosInstance from './axiosInstance'

//GET /timesheet/projects
export const getProjects = () =>
  axiosInstance.get('/timesheet/projects')

//POST /timesheet/entries
export const logEntry = (data) =>
  axiosInstance.post('/timesheet/entries', data)

//GET /timesheet/weeks/{weekStart}
export const getWeeklyTimesheet = (weekStart) =>
  axiosInstance.get(`/timesheet/weeks/${weekStart}`)

//GET /timesheet/my-timesheets
export const getMyTimesheets = () =>
  axiosInstance.get('/timesheet/my-timesheets')

//POST /timesheet/weeks/{weekStart}/submit
export const submitTimesheet = (weekStart) =>
  axiosInstance.post(`/timesheet/weeks/${weekStart}/submit`)

//GET /timesheet/manager/pending
export const getPendingTimesheets = () =>
  axiosInstance.get('/timesheet/manager/pending')

//PUT /timesheet/manager/review/{id}
export const reviewTimesheet = (id, data) =>
  axiosInstance.put(`/timesheet/manager/review/${id}`, data)