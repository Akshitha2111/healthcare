export function errorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (error?.response) {
    const body = error.response.data
    if (body?.fieldErrors && Object.keys(body.fieldErrors).length) return Object.values(body.fieldErrors)[0]
    if (body?.message) return body.message
    if (error.response.status === 429) return 'Too many requests. Please wait a moment and try again.'
    if (error.response.status >= 500) return 'The server had a problem. Please try again later.'
  } else if (error?.code === 'ECONNABORTED') {
    return 'The request timed out. Please try again.'
  } else if (error?.request) {
    return 'Cannot reach the server. Check your connection and try again.'
  }
  return fallback
}

export const fieldErrors = (error) => error?.response?.data?.fieldErrors || {}

export const formatDate = (value) =>
  value ? new Date(value).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }) : '—'

export const formatDateTime = (value) =>
  value ? new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—'

export const formatTime = (value) => (value ? String(value).slice(0, 5) : '')

export const titleCase = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : '')

export const todayIso = () => {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 10)
}

export const percent = (score) => `${Math.round((score || 0) * 100)}%`
