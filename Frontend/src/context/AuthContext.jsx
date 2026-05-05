import { createContext, useContext, useState, useEffect } from 'react'
import { decodeToken, isTokenExpired } from '../utils/helpers'

//Create the context
const AuthContext = createContext(null)

//Provider wraps the entire app
export function AuthProvider({ children }) {

  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)

  //On app start, restore session from localStorage
  useEffect(() => {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')

    if (savedToken && savedUser) {
      //Check if token is still valid
      if (!isTokenExpired(savedToken)) {
        setToken(savedToken)
        setUser(JSON.parse(savedUser))
      } else {
        //Token expired — clear storage
        localStorage.removeItem('token')
        localStorage.removeItem('user')
      }
    }
    setLoading(false)
  }, [])

  //Called after successful login
  const loginUser = (authResponse) => {
    const { token, userId, email, fullName, role } = authResponse

    const userData = { userId, email, fullName, role }

    // Save to state
    setToken(token)
    setUser(userData)

    // Save to localStorage — persists on refresh
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(userData))
  }

  //Called on logout
  const logoutUser = () => {
    setToken(null)
    setUser(null)
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  //Role checks — used in ProtectedRoute and Sidebar
  const isEmployee = () => user?.role === 'EMPLOYEE'
  const isManager = () => user?.role === 'MANAGER'
  const isAdmin = () => user?.role === 'ADMIN'
  const isManagerOrAdmin = () =>
    user?.role === 'MANAGER' || user?.role === 'ADMIN'

  const value = {
    user,
    token,
    loading,
    loginUser,
    logoutUser,
    isEmployee,
    isManager,
    isAdmin,
    isManagerOrAdmin,
    isAuthenticated: !!token
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

//Custom hook — use this in any component
//instead of useContext(AuthContext) directly
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}