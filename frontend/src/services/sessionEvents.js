import { tokenStore, sessionNotice } from '../utils/tokenStorage';

let handler = null;

/** AuthContext registers a callback so React state is cleared when the API reports a 401. */
export function onSessionExpired(fn) {
  handler = fn;
  return () => { if (handler === fn) handler = null; };
}

export function notifySessionExpired() {
  tokenStore.clear();
  sessionNotice.set();
  if (handler) handler();
  else if (typeof window !== 'undefined' && window.location.pathname !== '/login') window.location.assign('/login');
}
