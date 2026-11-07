import { useEffect, useRef } from 'react'
import { useAuth } from '../../context/AuthContext'
import { claimReferral } from '../../api/rewardApi'

// The backend issues 8-character alphanumeric codes. The value comes from the URL,
// so it is untrusted: anything that does not match is dropped before any API call.
const REFERRAL_CODE_PATTERN = /^[A-Za-z0-9]{8}$/

// Reads ?ref= from the URL and claims it once the user is signed in.
// The code is kept in memory only and is never written to localStorage or sessionStorage.
// Errors are swallowed on purpose: a failed claim must never block the app.
export default function ReferralClaim() {
  const { user }   = useAuth()
  const codeRef    = useRef(null)  // code read from the URL on first run
  const readRef    = useRef(false) // guards the URL read across StrictMode re-runs
  const claimedRef = useRef(false) // guarantees a single claim attempt

  useEffect(() => {
    if (!readRef.current) {
      readRef.current = true
      codeRef.current = readReferralParam()
    }
    const code = codeRef.current
    if (!user?.userId || !code || claimedRef.current) return

    claimedRef.current = true
    claimReferral(code)
      .catch(() => {})
      .finally(() => removeReferralParam())
  }, [user])

  return null
}

// Returns the validated code from ?ref=, or null. Invalid values are removed from the URL.
function readReferralParam() {
  const raw = new URLSearchParams(window.location.search).get('ref')
  if (raw === null) return null
  if (REFERRAL_CODE_PATTERN.test(raw)) return raw
  removeReferralParam()
  return null
}

// Removes the ref param without a page reload. Other params and the router state are kept.
function removeReferralParam() {
  const url = new URL(window.location.href)
  if (!url.searchParams.has('ref')) return
  url.searchParams.delete('ref')
  window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`)
}
