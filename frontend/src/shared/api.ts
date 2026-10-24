import axios from 'axios';
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public fields: { field: string; message: string }[] = [],
  ) {
    super(message);
  }
}
export function normalizeError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  if (!axios.isAxiosError(error))
    return new ApiError(
      0,
      'UNKNOWN',
      'Something went wrong. Please try again.',
    );
  const status = error.response?.status ?? 0;
  let body: unknown = error.response?.data;
  if (typeof body === 'string') {
    try {
      body = JSON.parse(body);
    } catch {
      body = null;
    }
  }
  const data =
    body && typeof body === 'object' ? (body as Record<string, unknown>) : {};
  const fallback =
    status === 401
      ? 'Your session has ended. Please sign in.'
      : status === 403
        ? 'You do not have access to this page.'
        : status === 409
          ? 'This change conflicts with the current record. Refresh and try again.'
          : status === 400
            ? 'Please check the supplied information.'
            : status >= 500
              ? 'The service is unavailable. Please try again.'
              : 'Unable to connect. Please try again.';
  const fields =
    status === 400 && Array.isArray(data.errors)
      ? data.errors.filter(
          (e): e is { field: string; message: string } =>
            !!e && typeof e.field === 'string' && typeof e.message === 'string',
        )
      : [];
  return new ApiError(
    status,
    typeof data.code === 'string' ? data.code : 'REQUEST_FAILED',
    status >= 500 || status === 401
      ? fallback
      : typeof data.detail === 'string'
        ? data.detail
        : fallback,
    fields,
  );
}
let token: string | null = null;
let onExpired = () => {};
export const session = {
  set: (value: string | null) => {
    token = value;
  },
  onExpired: (callback: () => void) => {
    onExpired = callback;
    return () => {
      onExpired = () => {};
    };
  },
};
export const api = axios.create({ baseURL: '/api', timeout: 15000 });
api.interceptors.request.use((config) => {
  if (token && config.url !== '/auth/login')
    config.headers.Authorization = `Bearer ${token}`;
  return config;
});
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const normalized = normalizeError(error);
    // A late response from an older session must not terminate a newer session.
    if (
      normalized.status === 401 &&
      token &&
      error.config?.headers?.Authorization === `Bearer ${token}`
    ) {
      token = null;
      onExpired();
    }
    return Promise.reject(normalized);
  },
);
