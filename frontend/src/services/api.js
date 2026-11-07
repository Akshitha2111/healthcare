import axios from 'axios'

// Authentication uses an HttpOnly cookie set by the server: no token is ever stored by (or readable from)
// JavaScript. "X-Requested-With" is the CSRF defence the backend requires on state-changing requests.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  withCredentials: true,
  timeout: 15000,
  headers: { 'X-Requested-With': 'XMLHttpRequest' },
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || ''
    const isAuthCall = url.startsWith('/api/auth/')
    if (error.response?.status === 401 && !isAuthCall) {
      // Session expired or revoked: let the AuthContext drop the user and send them to the login page.
      window.dispatchEvent(new Event('auth:expired'))
    }
    return Promise.reject(error)
  },
)

export default api
