export const DISCLAIMER_TEXT =
  'This tool is for educational purposes only. It does not provide a medical diagnosis and is not a substitute for professional medical advice, examination or treatment. If you think you may have a medical emergency, contact your local emergency services immediately.';

export const ROLES = { PATIENT: 'PATIENT', ADMIN: 'ADMIN' };

export const APPOINTMENT_STATUSES = ['PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'];

/** Legal admin transitions, mirroring docs/api-documentation.md section 1.4. */
export const ADMIN_TRANSITIONS = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['COMPLETED', 'CANCELLED', 'NO_SHOW'],
  CANCELLED: [],
  COMPLETED: [],
  NO_SHOW: [],
};

export const PATIENT_CANCELLABLE = ['PENDING', 'CONFIRMED'];

export const STATUS_LABELS = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  CANCELLED: 'Cancelled',
  COMPLETED: 'Completed',
  NO_SHOW: 'No show',
};

export const WEEKDAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

export const GENDERS = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
  { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
];

export const PAGE_SIZE = 10;
