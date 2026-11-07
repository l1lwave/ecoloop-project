import { useTranslation } from 'react-i18next'

const LANGS = [
  { code: 'nl', flag: '🇳🇱', label: 'NL' },
  { code: 'en', flag: '🇬🇧', label: 'EN' },
  { code: 'ua', flag: '🇺🇦', label: 'UA' },
]

export default function LanguageSwitcher() {
  const { i18n } = useTranslation()

  const change = (code) => {
    i18n.changeLanguage(code)
    localStorage.setItem('eco_lang', code)
  }

  return (
    <div className="flex gap-1">
      {LANGS.map(({ code, flag, label }) => (
        <button
          key={code}
          onClick={() => change(code)}
          title={label}
          className={`flex min-h-[44px] min-w-[44px] items-center gap-1 px-2 py-1 rounded-lg text-xs font-semibold transition-colors ${
            i18n.language === code
              ? 'bg-eco-600 text-white'
              : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
          }`}
        >
          <span>{flag}</span>
          <span>{label}</span>
        </button>
      ))}
    </div>
  )
}
