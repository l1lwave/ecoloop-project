import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/common/ProtectedRoute'
import BottomNav from './components/layout/BottomNav'
import CookieConsent from './components/legal/CookieConsent'
import DailyCheckin  from './components/common/DailyCheckin'
import ReferralClaim from './components/common/ReferralClaim'
import InstallBanner from './components/common/InstallBanner'

import LoginPage      from './pages/LoginPage'
import RegisterPage   from './pages/RegisterPage'
import HomePage       from './pages/HomePage'
import MapPage        from './pages/MapPage'
import ScanPage       from './pages/ScanPage'
import ProfilePage    from './pages/ProfilePage'
import PrivacyPolicy  from './pages/legal/PrivacyPolicy'
import TermsOfService from './pages/legal/TermsOfService'
import CookiePolicy   from './pages/legal/CookiePolicy'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        {/* Rendered before the routes so the md+ top bar sits above the page content in the flow. */}
        <BottomNav />
        <Routes>
          <Route path="/login"    element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* legal pages are public — reachable whether or not the user is logged in */}
          <Route path="/privacy" element={<PrivacyPolicy />} />
          <Route path="/terms"   element={<TermsOfService />} />
          <Route path="/cookies" element={<CookiePolicy />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/"        element={<HomePage />} />
            <Route path="/map"     element={<MapPage />} />
            <Route path="/scan"    element={<ScanPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <CookieConsent />
        <DailyCheckin />
        <ReferralClaim />
        <InstallBanner />
      </AuthProvider>
    </BrowserRouter>
  )
}
