/** Validation rules mirroring docs/api-documentation.md (kept in one place). */
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const PHONE_RE = /^[0-9 +\-()]{7,20}$/;

export const rules = {
  fullName: {
    required: 'Full name is required',
    minLength: { value: 2, message: 'Full name must be at least 2 characters' },
    maxLength: { value: 100, message: 'Full name must be at most 100 characters' },
    validate: (v) => v.trim().length >= 2 || 'Full name must be at least 2 characters',
  },
  email: {
    required: 'Email is required',
    maxLength: { value: 255, message: 'Email must be at most 255 characters' },
    pattern: { value: EMAIL_RE, message: 'Enter a valid email address' },
  },
  password: {
    required: 'Password is required',
    minLength: { value: 8, message: 'Password must be at least 8 characters' },
    maxLength: { value: 72, message: 'Password must be at most 72 characters' },
    validate: {
      letter: (v) => /[A-Za-z]/.test(v) || 'Password must contain at least one letter',
      digit: (v) => /\d/.test(v) || 'Password must contain at least one digit',
    },
  },
};

export function pastDate(v) {
  if (!v) return true;
  const today = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  const t = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
  return v < t || 'Date of birth must be in the past';
}
