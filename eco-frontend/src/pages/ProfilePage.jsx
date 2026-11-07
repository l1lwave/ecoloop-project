import { useEffect, useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getProfile } from '../api/rewardApi'
import LanguageSwitcher from '../components/layout/LanguageSwitcher'
import LegalAttribution from '../components/legal/LegalAttribution'
import InviteFriendCard from '../components/common/InviteFriendCard'
import ProfileTabs from '../components/profile/ProfileTabs'
import SwipeableWindows from '../components/profile/SwipeableWindows'
import GeneralInfoPanel from '../components/profile/GeneralInfoPanel'
import MedalsPanel from '../components/profile/MedalsPanel'
import HistoryPanel from '../components/profile/HistoryPanel'

// Profile is split into swipeable windows: general info, referral, medals, history.
export default function ProfilePage() {
  const { user }   = useAuth()
  const { t }      = useTranslation()
  const baseId     = useId()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [activeIndex, setActiveIndex] = useState(0)

  useEffect(() => {
    if (user?.userId) {
      getProfile()
        .then((r) => setProfile(r.data))
        .catch(console.error)
        .finally(() => setLoading(false))
    }
  }, [user])

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center bg-eco-50">
      <div className="w-8 h-8 border-2 border-eco-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )

  return (
    <div className="min-h-screen bg-eco-50 pb-24 max-w-md mx-auto overflow-x-hidden">
      <div className="bg-white px-4 pt-10 pb-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold text-gray-900">{t('profile.title')}</h1>
          <LanguageSwitcher />
        </div>
      </div>

      <ProfileTabs baseId={baseId} activeIndex={activeIndex} onChange={setActiveIndex} />

      <SwipeableWindows baseId={baseId} activeIndex={activeIndex} onChange={setActiveIndex}>
        <GeneralInfoPanel profile={profile} />
        <InviteFriendCard />
        <MedalsPanel badges={profile?.badges ?? []} />
        <HistoryPanel />
      </SwipeableWindows>

      <LegalAttribution variant="full" />
    </div>
  )
}
