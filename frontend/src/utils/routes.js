import { ROLES } from './constants';

export function homeFor(role) {
  return role === ROLES.ADMIN ? '/admin/dashboard' : '/app/dashboard';
}

/** A remembered redirect target is only honoured if it lies inside the area the role may use. */
export function safeTarget(target, role) {
  if (!target || typeof target !== 'string') return null;
  const prefix = role === ROLES.ADMIN ? '/admin' : '/app';
  return target === prefix || target.startsWith(`${prefix}/`) ? target : null;
}
