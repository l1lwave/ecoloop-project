import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import QRCode from 'qrcode'
import { getReferralInfo } from '../../api/rewardApi'

const COPY_FEEDBACK_MS = 2000

// Shows the signed-in user's own invite link, QR code, copy button and referral counts.
// Only the user's own code and counts are rendered; no invitee data is available here.
export default function InviteFriendCard() {
  const { t }   = useTranslation()
  const [info, setInfo]           = useState(null)
  const [loadFailed, setLoadFailed] = useState(false)
  const [qrUrl, setQrUrl]         = useState('')
  const [copyState, setCopyState] = useState('idle') // 'idle' | 'copied' | 'failed'
  const timerRef = useRef(null)

  useEffect(() => {
    let active = true
    getReferralInfo()
      .then((r) => { if (active) setInfo(r.data) })
      .catch(() => { if (active) setLoadFailed(true) })
    return () => { active = false }
  }, [])

  const code       = info?.code ?? ''
  const inviteLink = code ? `${window.location.origin}/register?ref=${encodeURIComponent(code)}` : ''

  // The QR code encodes the same link that is shown and copied.
  useEffect(() => {
    if (!inviteLink) return
    let active = true
    QRCode.toDataURL(inviteLink, { margin: 1, width: 320, errorCorrectionLevel: 'M' })
      .then((url) => { if (active) setQrUrl(url) })
      .catch(() => { if (active) setQrUrl('') })
    return () => { active = false }
  }, [inviteLink])

  useEffect(() => () => clearTimeout(timerRef.current), [])

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(inviteLink)
      setCopyState('copied')
    } catch {
      // Clipboard can be unavailable (for example on insecure origins); show a message instead of throwing.
      setCopyState('failed')
    }
    clearTimeout(timerRef.current)
    timerRef.current = setTimeout(() => setCopyState('idle'), COPY_FEEDBACK_MS)
  }

  // Native share sheet; only offered on devices that support the Web Share API.
  const canShare = typeof navigator !== 'undefined' && typeof navigator.share === 'function'

  const handleShare = async () => {
    try {
      await navigator.share({ title: t('referral.shareTitle'), text: t('referral.shareText'), url: inviteLink })
    } catch {
      // User cancelled or sharing failed; nothing to show.
    }
  }

  if (loadFailed) {
    return (
      <section className="mx-4 mt-6 bg-white rounded-3xl shadow-sm p-6">
        <h2 className="text-base font-bold text-gray-900 mb-2">{t('referral.title')}</h2>
        <p className="text-sm text-gray-500">{t('referral.loadError')}</p>
      </section>
    )
  }

  if (!info) return null

  return (
    <section className="mx-4 mt-6 bg-white rounded-3xl shadow-sm p-6">
      <h2 className="text-base font-bold text-gray-900">{t('referral.title')}</h2>
      <p className="text-gray-500 text-sm mt-1">{t('referral.description')}</p>

      <div className="mt-4 w-full max-w-xs mx-auto">
        {qrUrl && (
          <img src={qrUrl} alt={t('referral.qrAlt')} className="w-full h-auto rounded-xl" />
        )}
      </div>

      <p className="text-xs text-gray-500 mt-4">{t('referral.linkLabel')}</p>
      <p className="mt-1 break-all text-sm text-gray-800 bg-gray-50 rounded-xl px-3 py-2">{inviteLink}</p>

      <button
        type="button"
        onClick={handleCopy}
        className="mt-3 w-full bg-eco-600 text-white rounded-xl py-3 font-semibold text-sm hover:bg-eco-700 transition-colors"
      >
        {copyState === 'copied' ? t('referral.copiedBtn') : t('referral.copyBtn')}
      </button>
      {canShare && (
        <button
          type="button"
          onClick={handleShare}
          className="mt-2 w-full border border-eco-600 text-eco-700 rounded-xl py-3 font-semibold text-sm hover:bg-eco-50 transition-colors"
        >
          {t('referral.shareBtn')}
        </button>
      )}
      <div aria-live="polite" className="min-h-[1.25rem]">
        {copyState === 'failed' && (
          <p className="text-red-500 text-xs mt-2">{t('referral.copyFailed')}</p>
        )}
      </div>

      <div className="mt-2 grid grid-cols-2 gap-3">
        <div className="bg-eco-50 rounded-2xl p-3 text-center">
          <p className="font-bold text-gray-900 text-lg">{info.creditedCount ?? 0}</p>
          <p className="text-gray-500 text-xs">{t('referral.creditedLabel')}</p>
        </div>
        <div className="bg-eco-50 rounded-2xl p-3 text-center">
          <p className="font-bold text-gray-900 text-lg">{info.pendingCount ?? 0}</p>
          <p className="text-gray-500 text-xs">{t('referral.pendingLabel')}</p>
        </div>
      </div>

      {info.ambassadorUnlocked && (
        <p className="mt-3 text-center text-sm font-semibold text-eco-700">
          🏅 {t('referral.ambassadorUnlocked')}
        </p>
      )}
    </section>
  )
}
