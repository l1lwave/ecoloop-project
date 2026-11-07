import { useTranslation } from 'react-i18next'

// Third profile window: grid of all badges from the API.
// Earned badges glow; locked badges are greyed out and labelled as locked.
export default function MedalsPanel({ badges }) {
  const { t }         = useTranslation()
  const unlockedCount = badges.filter((badge) => badge.unlocked).length

  return (
    <section className="mx-4 bg-white rounded-3xl shadow-sm p-6">
      <h2 className="text-base font-bold text-gray-900">{t('profile.badgesTitle')}</h2>
      <p className="text-gray-500 text-sm mt-1">
        {t('profile.medalsCount', { unlocked: unlockedCount, total: badges.length })}
      </p>

      <ul className="mt-4 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
        {badges.map((badge) => (
          <li
            key={badge.id}
            className={`flex flex-col items-center gap-2 rounded-2xl p-3 text-center transition-shadow ${
              badge.unlocked
                ? 'bg-eco-50 shadow-[0_0_14px_rgba(34,197,94,0.45)]'
                : 'bg-gray-50 shadow-sm grayscale opacity-60'
            }`}
          >
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-white text-2xl shadow-sm">
              <span aria-hidden="true">{badge.unlocked ? '🏅' : '🔒'}</span>
            </div>
            <p className="text-xs font-semibold leading-tight text-gray-900">
              {t(`badges.${badge.id}.name`, { defaultValue: badge.name })}
            </p>
            <p className="line-clamp-3 text-[11px] leading-snug text-gray-500">
              {t(`badges.${badge.id}.description`, { defaultValue: badge.description })}
            </p>
            <p className={`text-[11px] font-semibold ${badge.unlocked ? 'text-eco-700' : 'text-gray-500'}`}>
              {badge.unlocked ? t('profile.statusUnlocked') : t('profile.statusLocked')}
            </p>
          </li>
        ))}
      </ul>
    </section>
  )
}
