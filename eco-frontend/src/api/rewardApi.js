import api from './axiosConfig'

// userId is never sent here — the backend derives it from the caller's own
// verified auth token (via the gateway), never from client-supplied data.
export const deposit      = (data)   => api.post('/rewards/deposit', data)
export const getProfile   = ()       => api.get('/rewards/profile')
export const checkin     = ()       => api.post('/rewards/checkin')
export const leaderboard  = (limit)  => api.get('/rewards/leaderboard', { params: { limit } })
export const getReferralInfo = ()     => api.get('/rewards/referrals/me')
export const claimReferral   = (code) => api.post('/rewards/referrals/claim', { code })
// Monthly return counts and deposit values for one calendar year of the logged-in user.
export const getReturnSummary = (year) => api.get('/rewards/returns/summary', { params: { year } })
