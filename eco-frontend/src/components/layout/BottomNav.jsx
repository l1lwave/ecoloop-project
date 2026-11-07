import { NavLink, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../context/AuthContext'

export default function BottomNav() {
  const { user }    = useAuth()
  const { pathname } = useLocation()
  const { t }       = useTranslation()

  const LEGAL_PATHS = ['/login', '/register', '/privacy', '/terms', '/cookies']
  if (!user || LEGAL_PATHS.includes(pathname)) return null

  const NAV_ITEMS = [
    { to: '/',        label: t('nav.home'),    icon: HomeIcon    },
    { to: '/map',     label: t('nav.map'),     icon: MapIcon     },
    { to: '/scan',    label: t('nav.scan'),    icon: ScanIcon    },
    { to: '/profile', label: t('nav.profile'), icon: ProfileIcon },
  ]

  // Phones: fixed bottom bar. md and up: the same links in a sticky top bar (App renders this before the routes).
  return (
    <nav className="fixed bottom-0 left-0 right-0 md:sticky md:top-0 md:bottom-auto md:left-auto md:right-auto md:max-w-none bg-white border-t md:border-t-0 md:border-b border-gray-200 flex justify-around md:justify-center md:gap-8 items-center md:items-stretch h-16 md:h-14 z-[1100] max-w-md mx-auto">
      {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/'}
          className={({ isActive }) =>
            `flex min-h-[44px] min-w-[44px] flex-col items-center gap-0.5 px-4 py-2 text-xs transition-colors md:flex-row md:gap-2 md:border-b-2 md:px-2 ${
              isActive ? 'text-eco-600 md:border-eco-600' : 'text-gray-400 md:border-transparent'
            }`
          }
        >
          {({ isActive }) => (
            <>
              <Icon active={isActive} />
              <span className={isActive ? 'font-semibold' : ''}>{label}</span>
              {/* The underline indicator is used only by the phone bar; on md the bottom border does this job. */}
              {isActive && <span className="w-5 h-0.5 bg-eco-600 rounded-full md:hidden" />}
            </>
          )}
        </NavLink>
      ))}
    </nav>
  )
}

function HomeIcon({ active }) {
  return (
    <svg className={`w-6 h-6 ${active ? 'text-eco-600' : 'text-gray-400'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={active ? 2.5 : 1.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 12l8.954-8.955a1.126 1.126 0 011.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
    </svg>
  )
}
function MapIcon({ active }) {
  return (
    <svg className={`w-6 h-6 ${active ? 'text-eco-600' : 'text-gray-400'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={active ? 2.5 : 1.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 6.75V15m6-6v8.25m.503 3.498l4.875-2.437c.381-.19.622-.58.622-1.006V4.82c0-.836-.88-1.38-1.628-1.006l-3.869 1.934c-.317.159-.69.159-1.006 0L9.503 3.252a1.125 1.125 0 00-1.006 0L3.622 5.689C3.24 5.88 3 6.27 3 6.695V19.18c0 .836.88 1.38 1.628 1.006l3.869-1.934c.317-.159.69-.159 1.006 0l4.994 2.497c.317.158.69.158 1.006 0z" />
    </svg>
  )
}
function ScanIcon({ active }) {
  return (
    <svg className={`w-6 h-6 ${active ? 'text-eco-600' : 'text-gray-400'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={active ? 2.5 : 1.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 4.875c0-.621.504-1.125 1.125-1.125h4.5c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-4.5A1.125 1.125 0 013.75 9.375v-4.5zM3.75 14.625c0-.621.504-1.125 1.125-1.125h4.5c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-4.5a1.125 1.125 0 01-1.125-1.125v-4.5zM13.5 4.875c0-.621.504-1.125 1.125-1.125h4.5c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-4.5A1.125 1.125 0 0113.5 9.375v-4.5z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 6.75h.75v.75h-.75v-.75zM6.75 16.5h.75v.75h-.75v-.75zM16.5 6.75h.75v.75h-.75v-.75zM13.5 13.5h.75v.75h-.75v-.75zM13.5 19.5h.75v.75h-.75v-.75zM19.5 13.5h.75v.75h-.75v-.75zM19.5 19.5h.75v.75h-.75v-.75zM16.5 16.5h.75v.75h-.75v-.75z" />
    </svg>
  )
}
function ProfileIcon({ active }) {
  return (
    <svg className={`w-6 h-6 ${active ? 'text-eco-600' : 'text-gray-400'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={active ? 2.5 : 1.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
    </svg>
  )
}
