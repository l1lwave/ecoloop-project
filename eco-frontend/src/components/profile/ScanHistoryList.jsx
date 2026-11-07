import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getScanHistory } from '../../api/scanApi'
import { formatEur, formatScannedAt, toNumber } from './historyFormat'

const PAGE_SIZE = 20

// Chronological list of scanned products (newest first), loaded 20 at a time.
export default function ScanHistoryList() {
  const { t, i18n }           = useTranslation()
  const [items, setItems]     = useState([])
  const [nextPage, setNextPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState(false)

  // Loads one page. Page 0 replaces the list; later pages are appended.
  const loadPage = async (pageToLoad) => {
    setLoading(true)
    setError(false)
    try {
      const { data } = await getScanHistory(pageToLoad, PAGE_SIZE)
      const incoming = Array.isArray(data?.items) ? data.items : []
      setItems((prev) => (pageToLoad === 0 ? incoming : [...prev, ...incoming]))
      setNextPage(pageToLoad + 1)
      setHasMore(data?.hasMore === true)
    } catch {
      // Do not log the error: it may contain request details about the user's scans.
      setError(true)
    } finally {
      setLoading(false)
    }
  }

  // Load the first page once on mount. loadPage only uses state setters, so it needs no dependency.
  useEffect(() => {
    loadPage(0)
  }, [])

  return (
    <section className="mx-4 bg-white rounded-3xl shadow-sm p-6">
      <h2 className="text-base font-bold text-gray-900">{t('history.scanTitle')}</h2>

      {error && (
        <div className="mt-3 flex flex-col items-start gap-3">
          <p className="text-sm text-red-500">{t('history.scanError')}</p>
          <button
            type="button"
            onClick={() => loadPage(nextPage)}
            className="min-h-[44px] rounded-xl bg-eco-600 px-4 py-2 text-sm font-semibold text-white"
          >
            {t('common.tryAgain')}
          </button>
        </div>
      )}

      {!error && !loading && items.length === 0 && (
        <p className="mt-3 text-sm text-gray-500">{t('history.scanEmpty')}</p>
      )}

      {items.length > 0 && (
        <ul className="mt-4 divide-y divide-gray-100">
          {items.map((item, index) => {
            const depositValue = toNumber(item.depositValue)
            return (
              <li key={`${item.scannedAt}-${index}`} className="flex items-start justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-gray-900 break-words">{item.productName}</p>
                  <p className="mt-0.5 text-xs text-gray-500 break-words">
                    {item.materialLabel && <span>{item.materialLabel} · </span>}
                    <span className={item.recyclable ? 'text-eco-600' : 'text-red-500'}>
                      {item.recyclable ? t('history.recyclable') : t('history.notRecyclable')}
                    </span>
                  </p>
                  <time className="text-[11px] text-gray-400" dateTime={item.scannedAt}>
                    {formatScannedAt(item.scannedAt, i18n.language)}
                  </time>
                </div>
                <p className={`shrink-0 text-right text-sm font-semibold ${
                  depositValue > 0 ? 'text-eco-600' : 'text-gray-400'
                }`}>
                  {depositValue > 0
                    ? t('history.depositAmount', { amount: formatEur(depositValue) })
                    : t('history.noDeposit')}
                </p>
              </li>
            )
          })}
        </ul>
      )}

      {loading && <p className="mt-3 text-sm text-gray-500">{t('common.loading')}</p>}

      {!loading && hasMore && (
        <button
          type="button"
          onClick={() => loadPage(nextPage)}
          className="mt-4 min-h-[44px] w-full rounded-xl bg-eco-50 py-2 text-sm font-semibold text-eco-700 hover:bg-eco-100"
        >
          {t('history.loadMore')}
        </button>
      )}
    </section>
  )
}
