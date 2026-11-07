import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import LanguageSwitcher from '../components/layout/LanguageSwitcher'

export default function LoginPage() {
  const { signIn }  = useAuth()
  const navigate    = useNavigate()
  const { t }       = useTranslation()
  const [form, setForm]       = useState({ email: '', password: '' })
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await signIn(form.email, form.password)
      navigate('/')
    } catch {
      setError(t('auth.invalidCredentials'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-eco-50 flex items-center justify-center p-4">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-sm p-6 sm:p-8">
        <div className="flex justify-end mb-4">
          <LanguageSwitcher />
        </div>
        <div className="text-center mb-8">
          <div className="text-5xl mb-2">♻️</div>
          <h1 className="text-2xl font-bold text-eco-800">{t('auth.loginTitle')}</h1>
          <p className="text-gray-500 text-sm mt-1">{t('auth.loginSubtitle')}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('auth.email')}</label>
            <input
              type="email" required value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-eco-400"
              placeholder="jouw@email.nl"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t('auth.password')}</label>
            <input
              type="password" required value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-eco-400"
              placeholder="••••••••"
            />
          </div>
          {error && <p className="text-red-500 text-sm">{error}</p>}
          <button type="submit" disabled={loading}
            className="w-full bg-eco-600 text-white rounded-xl py-3 font-semibold text-sm hover:bg-eco-700 transition-colors disabled:opacity-50">
            {loading ? t('common.loading') : t('auth.loginBtn')}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          {t('auth.toRegister')}{' '}
          <Link to="/register" className="inline-flex min-h-[44px] items-center text-eco-600 font-semibold">{t('auth.toRegisterLink')}</Link>
        </p>

        <p className="text-center text-xs text-gray-400 mt-4">
          <Link to="/privacy" className="inline-flex min-h-[44px] items-center underline">{t('auth.privacyLink')}</Link>
          {' · '}
          <Link to="/terms" className="inline-flex min-h-[44px] items-center underline">{t('auth.termsLink')}</Link>
        </p>
      </div>
    </div>
  )
}
