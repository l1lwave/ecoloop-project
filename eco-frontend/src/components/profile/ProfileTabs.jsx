import { useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { PROFILE_TABS, tabDomId, panelDomId } from './profileTabsConfig'

// Segmented tab bar for the profile windows.
// Arrow keys, Home and End move between tabs and select the focused one.
export default function ProfileTabs({ baseId, activeIndex, onChange }) {
  const { t }    = useTranslation()
  const tabRefs  = useRef([])
  const lastIndex = PROFILE_TABS.length - 1

  const handleKeyDown = (event, index) => {
    let next = null
    if (event.key === 'ArrowRight')  next = index === lastIndex ? 0 : index + 1
    else if (event.key === 'ArrowLeft') next = index === 0 ? lastIndex : index - 1
    else if (event.key === 'Home') next = 0
    else if (event.key === 'End')  next = lastIndex
    if (next === null) return

    event.preventDefault()
    onChange(next)
    tabRefs.current[next]?.focus()
  }

  return (
    <div
      role="tablist"
      aria-label={t('profile.tabListLabel')}
      className="mx-4 mt-4 flex gap-1 rounded-2xl bg-white p-1 shadow-sm"
    >
      {PROFILE_TABS.map((tab, index) => {
        const selected = index === activeIndex
        return (
          <button
            key={tab.key}
            ref={(el) => { tabRefs.current[index] = el }}
            id={tabDomId(baseId, index)}
            type="button"
            role="tab"
            aria-selected={selected}
            aria-controls={panelDomId(baseId, index)}
            tabIndex={selected ? 0 : -1}
            onClick={() => onChange(index)}
            onKeyDown={(event) => handleKeyDown(event, index)}
            className={`min-h-[44px] min-w-0 flex-1 truncate rounded-xl px-2 py-2 text-xs font-semibold transition-colors sm:text-sm ${
              selected ? 'bg-eco-600 text-white' : 'text-gray-600 hover:bg-eco-50'
            }`}
          >
            {t(tab.labelKey)}
          </button>
        )
      })}
    </div>
  )
}
