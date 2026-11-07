import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getProfile } from '../api/rewardApi'
import LanguageSwitcher from '../components/layout/LanguageSwitcher'

export default function HomePage() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  const { t }    = useTranslation()
  const [profile, setProfile] = useState(null)

  useEffect(() => {
    if (user?.userId) {
      getProfile().then((r) => setProfile(r.data)).catch(console.error)
    }
  }, [user])

  return (
    <div className="min-h-screen bg-eco-50 pb-20 max-w-md mx-auto">
      <div className="bg-white px-4 pt-10 pb-6">
        <div className="flex items-center justify-between mb-3">
          <div className="min-w-0">
            <p className="text-gray-500 text-sm">{t('home.hello')}</p>
            <h1 className="text-xl font-bold text-gray-900 break-words">{user?.name} 👋</h1>
          </div>
          <div className="flex items-center gap-3">
            <div className="bg-eco-100 rounded-full px-4 py-2 flex items-center gap-2">
              <span className="text-eco-700 font-bold">{profile?.totalPoints ?? 0}</span>
              <span className="text-eco-600 text-sm">{t('common.points')}</span>
            </div>
          </div>
        </div>
        <div className="flex items-center justify-between">
          <LanguageSwitcher />
          <button onClick={signOut} className="inline-flex min-h-[44px] items-center px-2 text-sm text-gray-400 hover:text-gray-600">
            {t('common.logout')}
          </button>
        </div>
      </div>

      <div className="flex justify-center py-10">
        <button
          onClick={() => navigate('/scan')}
          className="w-44 h-44 rounded-full bg-eco-600 shadow-2xl shadow-eco-300 flex flex-col items-center justify-center gap-2 active:scale-95 transition-transform"
        >
          <svg className="w-14 h-14 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0zM18.75 10.5h.008v.008h-.008V10.5z" />
          </svg>
          <span className="text-white font-bold text-sm tracking-wide">{t('home.scanBtn')}</span>
        </button>
      </div>

      <div className="px-4 grid grid-cols-2 gap-3">
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xl">🌳</span>
            <span className="text-sm font-semibold text-gray-700">{t('home.weeklyImpact')}</span>
          </div>
          <div className="w-full bg-eco-100 rounded-full h-2 mb-2">
            <div className="bg-eco-500 h-2 rounded-full transition-all"
              style={{ width: `${Math.min((profile?.weeklyPlasticKg ?? 0) / 15 * 100, 100)}%` }} />
          </div>
          <p className="text-sm text-gray-600">
            <span className="font-bold text-eco-700">{profile?.weeklyPlasticKg?.toFixed(1) ?? '0.0'} {t('common.kg')}</span>
            {' '}{t('home.weeklyDesc')}
          </p>
        </div>

        <div className="bg-white rounded-2xl p-4 shadow-sm cursor-pointer" onClick={() => navigate('/map')}>
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xl">🗺️</span>
            <span className="text-sm font-semibold text-gray-700">{t('home.nearest')}</span>
          </div>
          <div className="bg-eco-50 rounded-xl h-16 flex items-center justify-center">
            <p className="text-eco-700 text-xs font-medium text-center">{t('home.nearestLabel')}</p>
          </div>
        </div>
      </div>

      {profile?.badges?.filter(b => b.unlocked).length > 0 && (
        <div className="px-4 mt-4">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">{t('home.recentBadges')}</h2>
          <div className="flex gap-4 overflow-x-auto pb-2">
            {profile.badges.filter(b => b.unlocked).slice(0, 3).map((badge) => (
              <div key={badge.id} className="flex-shrink-0 flex flex-col items-center gap-1 w-20">
                <div className="w-14 h-14 rounded-full bg-eco-100 flex items-center justify-center text-2xl">🏅</div>
                <span className="text-xs text-gray-600 text-center leading-tight">
                  {t(`badges.${badge.id}.name`, { defaultValue: badge.name })}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
