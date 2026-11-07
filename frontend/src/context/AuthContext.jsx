import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { authApi } from '../services/endpoints'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // The session lives in an HttpOnly cookie, so the only way to know who is logged in is to ask the server.
  useEffect(() => {
    let alive = true
    authApi.me().then((u) => alive && setUser(u)).catch(() => alive && setUser(null)).finally(() => alive && setLoading(false))
    const expire = () => setUser(null)
    window.addEventListener('auth:expired', expire)
    return () => { alive = false; window.removeEventListener('auth:expired', expire) }
  }, [])

  const login = useCallback(async (credentials) => {
    const { user: u } = await authApi.login(credentials)
    setUser(u)
    return u
  }, [])

  const register = useCallback(async (details) => {
    const { user: u } = await authApi.register(details)
    setUser(u)
    return u
  }, [])

  const logout = useCallback(async () => {
    try { await authApi.logout() } finally { setUser(null) }
  }, [])

  const value = useMemo(() => ({ user, loading, login, register, logout, setUser }), [user, loading, login, register, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

export const homePathFor = (user) => (user?.role === 'ADMIN' ? '/admin' : '/dashboard')
