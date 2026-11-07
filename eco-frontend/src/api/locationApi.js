import api from './axiosConfig'

export const getNearby   = (lat, lng, radius, material) =>
  api.get('/locations', { params: { lat, lng, radius, material } })

export const getLocation = (id, lat, lng) =>
  api.get(`/locations/${id}`, { params: { lat, lng } })
