import { useTranslation } from 'react-i18next'
import StatCard from './StatCard'
import GrowthStageRow, { ECO_SLED_STAGES, MAX_LEVEL } from './GrowthStageRow'

// First profile window: growth stage, points progress, streaks and core metrics.
export default function GeneralInfoPanel({ profile }) {
  const { t }       = useTranslation()
  const level       = profile?.ecoSledLevel ?? 0
  const progressPct = profile ? Math.min((profile.totalPoints % 500) / 500 * 100, 100) : 0
  const treeStage   = ECO_SLED_STAGES[Math.min(Math.max(level, 0), MAX_LEVEL)]

  return (
    <div className="pb-2">
      <section className="mx-4 bg-white rounded-3xl shadow-sm p-6 text-center">
        <div className="text-8xl mb-2">{treeStage}</div>
        <GrowthStageRow level={level} />
        <div className="w-full bg-gray-100 rounded-full h-2.5 mt-4 mb-1">
          <div className="bg-eco-500 h-2.5 rounded-full transition-all duration-500"
            style={{ width: `${progressPct}%` }} />
        </div>
        <p className="text-eco-700 font-bold text-lg mt-2">
          {t('profile.ecoSledLabel')} {level}
        </p>
        <p className="text-gray-400 text-xs">{profile?.totalPoints ?? 0} {t('profile.pointsTotal')}</p>
      </section>

      <div className="mx-4 mt-4 grid grid-cols-2 gap-3">
        <StatCard icon="🔥" label={t('profile.currentStreakLabel')} value={profile?.currentStreak ?? 0} />
        <StatCard icon="🥇" label={t('profile.longestStreakLabel')} value={profile?.longestStreak ?? 0} />
      </div>

      <div className="mx-4 mt-3 grid grid-cols-2 gap-3">
        <StatCard icon="♻️" label={t('profile.plasticLabel')} value={`${profile?.plasticRecycledKg?.toFixed(1) ?? '0'} ${t('common.kg')}`} />
        <StatCard icon="💧" label={t('profile.waterLabel')}   value={`${profile?.waterSavedLiters ?? 0} ${t('common.liters')}`} />
        <StatCard icon="📍" label={t('profile.locationsLabel')} value={profile?.locationsVisited ?? 0} />
        <StatCard icon="🧾" label={t('profile.checksLabel')}  value={profile?.ecoChecks ?? 0} />
      </div>
    </div>
  )
}
