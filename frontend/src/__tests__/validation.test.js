import { validateLogin, validateProfile, validateRegister, validateSymptomSelection } from '../utils/validation'

describe('validateLogin', () => {
  it('requires email and password', () => {
    expect(validateLogin({ email: '', password: '' })).toEqual({ email: 'Email is required', password: 'Password is required' })
  })
  it('rejects malformed email', () => {
    expect(validateLogin({ email: 'nope', password: 'x' }).email).toMatch(/valid email/)
  })
  it('accepts valid input', () => {
    expect(validateLogin({ email: 'a@b.co', password: 'x' })).toEqual({})
  })
})

describe('validateRegister', () => {
  const ok = { fullName: 'Asha', email: 'a@b.co', password: 'Passw0rd1', confirmPassword: 'Passw0rd1' }
  it('accepts a valid form', () => expect(validateRegister(ok)).toEqual({}))
  it('enforces password strength', () => {
    expect(validateRegister({ ...ok, password: 'short1', confirmPassword: 'short1' }).password).toMatch(/at least 8/)
    expect(validateRegister({ ...ok, password: 'onlyletters', confirmPassword: 'onlyletters' }).password).toMatch(/letter and one number/)
  })
  it('requires matching confirmation', () => {
    expect(validateRegister({ ...ok, confirmPassword: 'Different1' }).confirmPassword).toBe('Passwords do not match')
  })
  it('requires a name', () => expect(validateRegister({ ...ok, fullName: ' ' }).fullName).toBeDefined())
})

describe('validateProfile', () => {
  it('validates phone and date of birth', () => {
    expect(validateProfile({ fullName: 'A', phone: 'abc' }).phone).toBeDefined()
    expect(validateProfile({ fullName: 'A', dateOfBirth: '2999-01-01' }).dateOfBirth).toBeDefined()
    expect(validateProfile({ fullName: 'A', phone: '+91 98765 43210', dateOfBirth: '1990-01-01' })).toEqual({})
  })
})

describe('validateSymptomSelection', () => {
  it('enforces min and max', () => {
    expect(validateSymptomSelection(['a'])).toMatch(/at least 2/)
    expect(validateSymptomSelection(['a', 'b'])).toBe('')
    expect(validateSymptomSelection(new Array(21).fill('x'))).toMatch(/at most 20/)
  })
})
