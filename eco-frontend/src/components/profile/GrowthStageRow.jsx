import { useTranslation } from 'react-i18next'

// One emoji per growth stage, levels 0 to 10 (11 stages).
export const ECO_SLED_STAGES = ['🌱', '🌿', '🪴', '🌳', '🌲', '🌴', '🌵', '🎋', '🌾', '🌏', '🌸']
export const MAX_LEVEL = ECO_SLED_STAGES.length - 1

// Row of all growth stages with the current level highlighted.
export default function GrowthStageRow({ level }) {
  const { t }   = useTranslation()
  const current = Math.min(Math.max(level, 0), MAX_LEVEL)

  return (
    <div>
      <p className="text-xs text-gray-500 mb-2">{t('profile.growthLabel')}</p>
      <ol className="grid grid-cols-11 gap-0.5">
        {ECO_SLED_STAGES.map((stage, index) => {
          const isCurrent = index === current
          return (
            <li
              key={index}
              aria-current={isCurrent ? 'step' : undefined}
              className={`flex h-8 items-center justify-center rounded-lg text-sm transition-colors sm:text-base ${
                isCurrent ? 'bg-eco-100 ring-2 ring-eco-500' : 'opacity-40'
              }`}
            >
              <span aria-hidden="true">{stage}</span>
            </li>
          )
        })}
      </ol>
    </div>
  )
}
