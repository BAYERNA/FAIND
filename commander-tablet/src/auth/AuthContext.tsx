import { createContext, useCallback, useMemo, useState, type ReactNode } from 'react'
import { getStoredToken, setStoredToken } from '../api/client'
import * as authApi from '../api/auth'
import type { Role } from '../types'

interface AuthUser {
  userId: string
  name: string
  role: Role
  initialPassword: boolean
}

interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  login: (badgeNumber: string, password: string) => Promise<AuthUser>
  completeInitialPassword: () => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

const USER_STORAGE_KEY = 'faind.user'

function loadStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

function persistUser(user: AuthUser | null): void {
  if (user) {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_STORAGE_KEY)
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => (getStoredToken() ? loadStoredUser() : null))

  const login = useCallback(async (badgeNumber: string, password: string) => {
    const response = await authApi.login(badgeNumber, password)
    setStoredToken(response.accessToken)
    const nextUser: AuthUser = {
      userId: response.userId,
      name: response.name,
      role: response.role,
      initialPassword: response.initialPassword,
    }
    persistUser(nextUser)
    setUser(nextUser)
    return nextUser
  }, [])

  const completeInitialPassword = useCallback(() => {
    setUser((prev) => {
      if (!prev) return prev
      const next = { ...prev, initialPassword: false }
      persistUser(next)
      return next
    })
  }, [])

  const logout = useCallback(() => {
    setStoredToken(null)
    persistUser(null)
    setUser(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, login, completeInitialPassword, logout }),
    [user, login, completeInitialPassword, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
