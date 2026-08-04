import request from './request'
import { calculateAnalysisLocally, calculateDetectionLocally, calculateStatisticsLocally } from '@/utils/localGasCalculation'

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getProfile = () => request.get('/user/profile')
export const updateProfile = (data) => request.put('/user/profile', data)
export const changePassword = (data) => request.put('/user/password', data)

export const getAdminUsers = (params) => request.get('/admin/users', { params })
export const getAdminUserStatistics = () => request.get('/admin/users/statistics')
export const updateAdminUserStatus = (id, data) => request.put(`/admin/users/${id}/status`, data)
export const addUserToBlacklist = (id, data) => request.put(`/admin/users/${id}/blacklist`, data)
export const removeUserFromBlacklist = (id) => request.delete(`/admin/users/${id}/blacklist`)
export const getUserLoginLogs = (id, params) => request.get(`/admin/users/${id}/login-logs`, { params })

export const getNews = () => request.get('/public/news')
export const calcAnalysis = (data) => calculateAnalysisLocally(data)
export const calcStatistics = (data) => calculateStatisticsLocally(data)
export const calcDetection = (data) => calculateDetectionLocally(data)

export const uploadFile = (formData) => request.post('/files/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const getDataFiles = () => request.get('/files')
export const deleteDataFile = (id) => request.delete(`/files/${id}`)

export const getReports = () => request.get('/reports')
export const exportReport = (data) => request.post('/reports/export', data)

export const submitFeedback = (data) => request.post('/feedback', data)
export const getMyFeedback = () => request.get('/feedback/mine')
export const getAdminFeedback = (params) => request.get('/admin/feedback', { params })
export const getAdminFeedbackStatistics = () => request.get('/admin/feedback/statistics')
export const handleAdminFeedback = (id, data) => request.put(`/admin/feedback/${id}/handle`, data)
