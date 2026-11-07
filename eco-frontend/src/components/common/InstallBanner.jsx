import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

// Shows an install banner only when the browser offers installation through
// beforeinstallprompt. Browsers that never fire the event (for example Safari on iOS)
// get no banner at all.
//
// The deferred prompt and the dismissal are kept in component state only.
// Nothing is written to localStorage or sessionStorage.
export default function InstallBanner() {
  const { t } = useTranslation()
  const [deferredPrompt, setDeferredPrompt] = useState(null)
  const [dismissed, setDismissed]           = useState(false)

  useEffect(() => {
    // Stop the mini-infobar so the in-app banner is the only install entry point.
    const onBeforeInstall = (event) => {
      event.preventDefault()
      setDeferredPrompt(event)
    }
    // The app is installed, so the offer is no longer relevant.
    const onInstalled = () => setDeferredPrompt(null)

    window.addEventListener('beforeinstallprompt', onBeforeInstall)
    window.addEventListener('appinstalled', onInstalled)
    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstall)
      window.removeEventListener('appinstalled', onInstalled)
    }
  }, [])

  // The prompt can be used only once, so the banner is hidden as soon as the user chooses.
  const handleInstall = async () => {
    const promptEvent = deferredPrompt
    setDeferredPrompt(null)
    try {
      // Called only from this click handler, which is a user gesture.
      await promptEvent.prompt()
    } catch {
      // The browser refused the prompt. The banner stays hidden for this session.
    }
  }

  const handleLater = () => {
    setDismissed(true)
  }

  if (!deferredPrompt || dismissed) return null

  return (
    <div
      role="region"
      aria-label={t('install.title')}
      className="fixed bottom-20 md:bottom-4 left-0 right-0 z-[1140] max-w-md mx-auto px-4"
    >
      <div className="bg-white rounded-2xl shadow-2xl border border-gray-100 p-4">
        <p className="text-sm font-semibold text-gray-800">{t('install.title')}</p>
        <p className="mt-1 text-sm text-gray-600 leading-relaxed">{t('install.body')}</p>
        <div className="mt-3 flex justify-end gap-2">
          <button
            type="button"
            onClick={handleLater}
            className="min-h-[44px] px-4 py-2 text-sm font-medium text-gray-500 rounded-xl hover:bg-gray-100"
          >
            {t('install.later')}
          </button>
          <button
            type="button"
            onClick={handleInstall}
            className="min-h-[44px] px-4 py-2 text-sm font-medium text-white bg-eco-600 rounded-xl hover:bg-eco-700"
          >
            {t('install.button')}
          </button>
        </div>
      </div>
    </div>
  )
}
