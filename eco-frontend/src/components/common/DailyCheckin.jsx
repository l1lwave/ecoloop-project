import { useEffect, useRef } from 'react'
import { useAuth } from '../../context/AuthContext'
import { checkin } from '../../api/rewardApi'

// Records the daily check-in once per signed-in user while the app is open.
// Errors are swallowed on purpose: a failed check-in must never block the app.
// Nothing is logged and nothing is written to storage.
export default function DailyCheckin() {
  const { user }    = useAuth()
  const lastUserRef = useRef(null) // in-memory guard, resets on page reload

  useEffect(() => {
    if (!user?.userId || lastUserRef.current === user.userId) return
    lastUserRef.current = user.userId
    checkin().catch(() => {})
  }, [user])

  return null
}
