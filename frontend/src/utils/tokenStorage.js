/**
 * JWT holder: in-memory first, mirrored to sessionStorage (NOT localStorage) so a page
 * refresh keeps the session but closing the tab ends it. See README for the trade-offs.
 */
const KEY = 'hda.token';
let memoryToken = null;

function safe(fn) {
  try { return fn(); } catch { return undefined; }
}

export const tokenStore = {
  get() {
    if (memoryToken) return memoryToken;
    const stored = safe(() => window.sessionStorage.getItem(KEY));
    if (stored) memoryToken = stored;
    return memoryToken;
  },
  set(token) {
    memoryToken = token || null;
    safe(() => (token ? window.sessionStorage.setItem(KEY, token) : window.sessionStorage.removeItem(KEY)));
  },
  clear() {
    memoryToken = null;
    safe(() => window.sessionStorage.removeItem(KEY));
  },
};

const EXPIRED_KEY = 'hda.sessionExpired';
export const sessionNotice = {
  set() { safe(() => window.sessionStorage.setItem(EXPIRED_KEY, '1')); },
  consume() {
    const v = safe(() => window.sessionStorage.getItem(EXPIRED_KEY));
    if (v) safe(() => window.sessionStorage.removeItem(EXPIRED_KEY));
    return !!v;
  },
  peek() { return !!safe(() => window.sessionStorage.getItem(EXPIRED_KEY)); },
};
