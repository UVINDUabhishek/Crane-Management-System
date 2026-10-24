import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { api, session, ApiError } from '../../shared/api';
import { roles } from '../../shared/permissions';
const userSchema = z.object({
  id: z.number().int(),
  username: z.string(),
  email: z.string(),
  role: z.enum(roles),
});
export type User = z.infer<typeof userSchema>;
export function tokenExpiry(token: string): number {
  try {
    const part = token.split('.')[1];
    const payload = JSON.parse(
      atob(part.replace(/-/g, '+').replace(/_/g, '/')),
    );
    if (typeof payload.exp === 'number' && Number.isFinite(payload.exp))
      return payload.exp * 1000;
  } catch {
    /* Invalid token is never exposed. */
  }
  throw new ApiError(
    401,
    'INVALID_TOKEN',
    'Unable to establish a session. Please sign in again.',
  );
}
interface Auth {
  user: User | null;
  busy: boolean;
  expired: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}
const Context = createContext<Auth | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null),
    [busy, setBusy] = useState(false),
    [expired, setExpired] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined),
    epoch = useRef(0);
  const cache = useQueryClient();
  const clear = useCallback(
    (expiredSession = false) => {
      epoch.current++;
      clearTimeout(timer.current);
      session.set(null);
      setUser(null);
      setBusy(false);
      setExpired(expiredSession);
      cache.clear();
    },
    [cache],
  );
  useEffect(() => {
    const generation = epoch;
    const unsubscribe = session.onExpired(() => clear(true));
    return () => {
      unsubscribe();
      clearTimeout(timer.current);
      session.set(null);
      generation.current++;
    };
  }, [clear]);
  async function login(email: string, password: string) {
    clear();
    setBusy(true);
    const attempt = epoch.current;
    try {
      const response = await api.post<string>(
        '/auth/login',
        { email, password },
        { responseType: 'text', transformResponse: [(data) => data] },
      );
      const jwt = response.data.trim();
      const expiry = tokenExpiry(jwt);
      if (expiry <= Date.now())
        throw new ApiError(
          401,
          'EXPIRED_TOKEN',
          'Your session has ended. Please sign in.',
        );
      if (attempt !== epoch.current) return;
      session.set(jwt);
      const identity = userSchema.parse((await api.get('/auth/me')).data);
      if (attempt !== epoch.current) return;
      if (expiry <= Date.now()) {
        clear(true);
        return;
      }
      setUser(identity);
      timer.current = setTimeout(
        () => clear(true),
        Math.min(expiry - Date.now(), 2147483647),
      );
    } catch (error) {
      if (attempt === epoch.current) clear();
      throw error;
    } finally {
      if (attempt === epoch.current) setBusy(false);
    }
  }
  return (
    <Context.Provider
      value={{ user, busy, expired, login, logout: () => clear() }}
    >
      {children}
    </Context.Provider>
  );
}
export function useAuth() {
  const auth = useContext(Context);
  if (!auth) throw new Error('AuthProvider is required');
  return auth;
}
