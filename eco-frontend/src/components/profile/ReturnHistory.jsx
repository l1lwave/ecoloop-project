import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getReturnSummary } from '../../api/rewardApi'
import ReturnChart from './ReturnChart'
import { formatEur, formatKg, toNumber } from './historyFormat'

// Year-by-year return summary: year selector, totals and the monthly chart.
// Money shown here is a deposit value, never a payout.
export default function ReturnHistory() {
  const { t }              = useTranslation()
  const currentYear        = new Date().getFullYear()
  const [year, setYear]    = useState(currentYear)
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]  = useState(false)

  useEffect(() => {
    // Ignore a response for a year the user has already moved away from.
    let cancelled = false
    setLoading(true)
    setError(false)
    getReturnSummary(year)
      .then(({ data }) => { if (!cancelled) setSummary(data) })
      .catch(() => { if (!cancelled) { setSummary(null); setError(true) } })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [year])

  return (
    <section className="mx-4 bg-white rounded-3xl shadow-sm p-6">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-base font-bold text-gray-900">{t('history.returnsTitle')}</h2>
        <div className="flex shrink-0 items-center gap-2">
          <button
            type="button"
            aria-label={t('history.prevYear')}
            onClick={() => setYear((y) => y - 1)}
            className="h-11 w-11 sm:h-8 sm:w-8 rounded-lg bg-gray-100 text-gray-600 hover:bg-gray-200"
          >
            ‹
          </button>
          <span className="min-w-[3rem] text-center text-sm font-semibold text-gray-900">{year}</span>
          <button
            type="button"
            aria-label={t('history.nextYear')}
            disabled={year >= currentYear}
            onClick={() => setYear((y) => Math.min(y + 1, currentYear))}
            className="h-11 w-11 sm:h-8 sm:w-8 rounded-lg bg-gray-100 text-gray-600 hover:bg-gray-200 disabled:cursor-not-allowed disabled:opacity-40"
          >
            ›
          </button>
        </div>
      </div>

      {loading && <p className="mt-3 text-sm text-gray-500">{t('common.loading')}</p>}

      {!loading && error && (
        <p className="mt-3 text-sm text-red-500">{t('history.returnsError')}</p>
      )}

      {!loading && !error && summary && (
        <>
          <dl className="mt-4 grid grid-cols-3 gap-2 text-center">
            <div className="rounded-2xl bg-eco-50 p-2">
              <dt className="text-[11px] text-gray-500">{t('history.totalItems')}</dt>
              <dd className="text-sm font-bold text-gray-900">{toNumber(summary.totalItems)}</dd>
            </div>
            <div className="rounded-2xl bg-eco-50 p-2">
              <dt className="text-[11px] text-gray-500">{t('history.totalDeposit')}</dt>
              <dd className="text-sm font-bold text-gray-900">€{formatEur(summary.totalDepositEur)}</dd>
            </div>
            <div className="rounded-2xl bg-eco-50 p-2">
              <dt className="text-[11px] text-gray-500">{t('history.totalPlastic')}</dt>
              <dd className="text-sm font-bold text-gray-900">
                {formatKg(summary.totalPlasticKg)} {t('common.kg')}
              </dd>
            </div>
          </dl>

          {toNumber(summary.totalItems) === 0 && (
            <p className="mt-3 text-sm text-gray-500">{t('history.returnsEmpty', { year })}</p>
          )}

          <ReturnChart year={year} months={summary.months} />
        </>
      )}

      <p className="mt-3 text-[11px] leading-snug text-gray-500">{t('history.disclaimer')}</p>
    </section>
  )
}
