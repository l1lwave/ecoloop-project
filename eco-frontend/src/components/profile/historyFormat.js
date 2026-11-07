// Pure formatting helpers for the History window. No React imports, so they stay easy to test.

// Missing, null or non-numeric API values count as 0, so the UI never renders NaN.
export const toNumber = (value) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

export const formatEur = (value) => toNumber(value).toFixed(2)
export const formatKg  = (value) => toNumber(value).toFixed(2)

// The app uses the i18n code "ua" for Ukrainian, but Intl needs the BCP 47 tag "uk".
export const toIntlLocale = (lang) => (lang === 'ua' ? 'uk' : lang)

// Localized short month name, e.g. "jan" or "Jan". Month is 1-12.
export const monthLabel = (year, month, lang) =>
  new Intl.DateTimeFormat(toIntlLocale(lang), { month: 'short' })
    .format(new Date(year, month - 1, 1))

// Localized date and time for a scan. The API sends a local date-time without a zone.
// Returns an empty string when the value cannot be parsed.
export const formatScannedAt = (value, lang) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat(toIntlLocale(lang), { dateStyle: 'medium', timeStyle: 'short' })
    .format(date)
}

// Always returns 12 points, one per month. Months missing from the API response become zeros.
export const buildMonthlySeries = (year, months, lang) => {
  const byMonth = new Map()
  if (Array.isArray(months)) {
    months.forEach((entry) => byMonth.set(Number(entry?.month), entry))
  }
  return Array.from({ length: 12 }, (_, index) => {
    const month = index + 1
    const entry = byMonth.get(month)
    return {
      label:   monthLabel(year, month, lang),
      items:   toNumber(entry?.itemsReturned),
      deposit: toNumber(entry?.depositEur),
    }
  })
}
