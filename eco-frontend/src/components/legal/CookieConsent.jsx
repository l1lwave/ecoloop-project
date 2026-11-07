import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'

/**
 * EU-style cookie/local-storage consent banner.
 *
 * Honesty note: this app currently sets NO cookies and uses NO analytics —
 * see CookiePolicy.jsx. Everything in localStorage today (auth token,
 * language) is "strictly necessary" under the ePrivacy Directive and is
 * exempt from the opt-in requirement, so there is nothing to meaningfully
 * "reject" today. This component still implements a real opt-in toggle for
 * a future "Analytics" category (off by default, disabled, clearly marked
 * "not currently in use") so that if/when non-essential tracking is ever
 * added, the opt-in mechanism required by law is already wired up — flip
 * ANALYTICS_AVAILABLE to true and connect onSave's analytics branch once
 * (and only once) an actual analytics integration exists.
 */

const STORAGE_KEY = 'eco_cookie_consent'
const ANALYTICS_AVAILABLE = false // flip to true only once a real analytics integration exists

function readConsent() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function writeConsent(prefs) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...prefs, decidedAt: new Date().toISOString() }))
  } catch {
    // localStorage unavailable (private mode / disabled) — banner will just reappear next visit
  }
}

export default function CookieConsent() {
  const [visible, setVisible]   = useState(false)
  const [expanded, setExpanded] = useState(false)
  const [analytics, setAnalytics] = useState(false)

  useEffect(() => {
    setVisible(!readConsent())
  }, [])

  const acceptAll = () => {
    writeConsent({ necessary: true, analytics: ANALYTICS_AVAILABLE })
    setVisible(false)
  }

  const saveChoices = () => {
    writeConsent({ necessary: true, analytics: ANALYTICS_AVAILABLE ? analytics : false })
    setVisible(false)
  }

  if (!visible) return null

  return (
    <div className="fixed bottom-20 md:bottom-0 left-0 right-0 z-[1150] max-w-md mx-auto px-4 pb-4">
      <div className="bg-white rounded-2xl shadow-2xl border border-gray-100 p-4">
        <p className="text-sm text-gray-700 leading-relaxed">
          🍪 We use only strictly-necessary browser storage to keep you logged in and remember your
          language — no advertising or tracking cookies. See our{' '}
          <Link to="/cookies" className="text-eco-600 underline font-medium">Cookie Policy</Link>.
        </p>

        {expanded && (
          <div className="mt-3 space-y-2 border-t border-gray-100 pt-3">
            <PreferenceRow
              label="Strictly necessary"
              description="Login session & language preference. Always on — required for the app to work."
              checked
              disabled
            />
            <PreferenceRow
              label="Analytics (optional)"
              description={ANALYTICS_AVAILABLE
                ? 'Helps us understand feature usage. Off unless you turn it on.'
                : 'Not currently in use — there is nothing to opt into yet.'}
              checked={analytics}
              disabled={!ANALYTICS_AVAILABLE}
              onChange={setAnalytics}
            />
          </div>
        )}

        <div className="flex items-center gap-2 mt-3">
          <button
            onClick={() => setExpanded(v => !v)}
            className="flex min-h-[44px] items-center text-xs text-gray-500 underline flex-shrink-0"
          >
            {expanded ? 'Hide details' : 'Manage preferences'}
          </button>
          <div className="flex-1" />
          {expanded && (
            <button
              onClick={saveChoices}
              className="min-h-[44px] text-xs font-semibold text-eco-700 border border-eco-300 rounded-full px-3 py-1.5"
            >
              Save choices
            </button>
          )}
          <button
            onClick={acceptAll}
            className="min-h-[44px] text-xs font-semibold text-white bg-eco-600 rounded-full px-4 py-1.5"
          >
            Got it
          </button>
        </div>
      </div>
    </div>
  )
}

function PreferenceRow({ label, description, checked, disabled, onChange }) {
  return (
    <label className={`flex items-start gap-3 ${disabled && onChange === undefined ? '' : ''}`}>
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(e) => onChange?.(e.target.checked)}
        className="mt-0.5 accent-eco-600 disabled:opacity-40"
      />
      <span>
        <span className="block text-sm font-semibold text-gray-800">{label}</span>
        <span className="block text-sm text-gray-400">{description}</span>
      </span>
    </label>
  )
}
