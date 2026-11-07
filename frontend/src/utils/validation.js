const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateLogin({ email, password }) {
  const errors = {}
  if (!email?.trim()) errors.email = 'Email is required'
  else if (!EMAIL.test(email.trim())) errors.email = 'Enter a valid email address'
  if (!password) errors.password = 'Password is required'
  return errors
}

export function validateRegister({ fullName, email, password, confirmPassword }) {
  const errors = validateLogin({ email, password })
  if (!fullName?.trim()) errors.fullName = 'Full name is required'
  else if (fullName.trim().length > 120) errors.fullName = 'Full name is too long'
  if (password) {
    if (password.length < 8) errors.password = 'Password must be at least 8 characters'
    else if (password.length > 72) errors.password = 'Password must be at most 72 characters'
    else if (!/[A-Za-z]/.test(password) || !/\d/.test(password))
      errors.password = 'Password must contain at least one letter and one number'
  }
  if (!confirmPassword) errors.confirmPassword = 'Please confirm your password'
  else if (password !== confirmPassword) errors.confirmPassword = 'Passwords do not match'
  return errors
}

export function validateProfile({ fullName, phone, dateOfBirth }) {
  const errors = {}
  if (!fullName?.trim()) errors.fullName = 'Full name is required'
  if (phone && !/^[0-9+()\- ]{7,20}$/.test(phone)) errors.phone = 'Enter a valid phone number'
  if (dateOfBirth && new Date(dateOfBirth) >= new Date()) errors.dateOfBirth = 'Date of birth must be in the past'
  return errors
}

export function validateSymptomSelection(selected, min = 2, max = 20) {
  if (selected.length < min) return `Select at least ${min} symptoms`
  if (selected.length > max) return `Select at most ${max} symptoms`
  return ''
}

export const hasErrors = (errors) => Object.keys(errors).length > 0
