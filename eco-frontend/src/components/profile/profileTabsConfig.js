// Window definitions for the profile page. The array order is the swipe order.
export const PROFILE_TABS = [
  { key: 'general',  labelKey: 'profile.tabs.general' },
  { key: 'referral', labelKey: 'profile.tabs.referral' },
  { key: 'medals',   labelKey: 'profile.tabs.medals' },
  { key: 'history',  labelKey: 'history.tabLabel' },
]

// DOM ids shared by the tab bar and the window panels so aria-controls and aria-labelledby match.
export const tabDomId   = (baseId, index) => `${baseId}-tab-${index}`
export const panelDomId = (baseId, index) => `${baseId}-panel-${index}`
