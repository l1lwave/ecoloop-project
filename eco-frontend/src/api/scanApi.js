import api from './axiosConfig'

export const identify  = (barcode, lang = 'nl') =>
  api.post('/scan/identify', { barcode, lang })

export const materials = () => api.get('/scan/materials')

// Paginated scan history of the logged-in user, newest first. Identity comes from the token only.
export const getScanHistory = (page = 0, size = 20) =>
  api.get('/scan/history', { params: { page, size } })
