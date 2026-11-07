import { createContext, useContext, useState, useEffect } from 'react'
import { login as apiLogin, register as apiRegister } from '../api/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser]   = useState(() => {
    const stored = localStorage.getItem('eco_user')
    return stored ? JSON.parse(stored) : null
  })

  const signIn = async (email, password) => {
    const { data } = await apiLogin({ email, password })
    localStorage.setItem('eco_token', data.token)
    localStorage.setItem('eco_user', JSON.stringify(data))
    setUser(data)
    return data
  }

  const signUp = async (name, email, password) => {
    const { data } = await apiRegister({ name, email, password })
    localStorage.setItem('eco_token', data.token)
    localStorage.setItem('eco_user', JSON.stringify(data))
    setUser(data)
    return data
  }

  const signOut = () => {
    localStorage.removeItem('eco_token')
    localStorage.removeItem('eco_user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, signIn, signUp, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
