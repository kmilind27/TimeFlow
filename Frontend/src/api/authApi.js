import axiosInstance from './axiosInstance'

// POST /auth/signup
export const signup = (data) =>
  axiosInstance.post('/auth/signup', data)

//POST /auth/login
export const login = (data) =>
  axiosInstance.post('/auth/login', data)

//POST /auth/forgot-password (Step 1: Request OTP)
export const requestOtp = (data) =>
  axiosInstance.post('/auth/forgot-password', data)

//POST /auth/verify-otp (Step 2: Verify OTP)
export const verifyOtp = (data) =>
  axiosInstance.post('/auth/verify-otp', data)

//POST /auth/reset-password (Step 3: Reset Password)
export const resetPassword = (data) =>
  axiosInstance.post('/auth/reset-password', data)

//GET /auth/profile
export const getProfile = () =>
  axiosInstance.get('/auth/profile')

// GET /auth/users (admin)
export const getAllUsers = () =>
  axiosInstance.get('/auth/users')

// GET /auth/users/{id}
export const getUserById = (id) =>
  axiosInstance.get(`/auth/users/${id}`)

// PATCH /auth/users/{id}/role
export const changeRole = (id, data) =>
  axiosInstance.patch(`/auth/users/${id}/role`, data)

//PATCH /auth/users/{id}/status
export const changeStatus = (id, data) =>
  axiosInstance.patch(`/auth/users/${id}/status`, data)

// PATCH /auth/users/{id}/manager
export const updateManager = (id, data) =>
  axiosInstance.patch(`/auth/users/${id}/manager`, data)

//DELETE /auth/users/{id}
export const deleteUser = (id) =>
  axiosInstance.delete(`/auth/users/${id}`)