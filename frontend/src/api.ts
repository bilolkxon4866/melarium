export interface ApiErrorDetail { field: string; message: string }
export class ApiClientError extends Error {
  status: number;
  details: ApiErrorDetail[];
  constructor(message: string, status: number, details: ApiErrorDetail[] = []) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.details = details;
  }
}

export interface SessionTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: { id: string; phone: string; fullName?: string | null; roles: string[] };
}
export interface UserProfile {
  id: string;
  phone: string;
  fullName: string | null;
  email: string | null;
  accountStatus: string;
  roles: string[];
  createdAt: string;
}
export interface OtpRequestResponse { phone: string; expiresInSeconds: number; deliveryMode: string; debugCode?: string | null }
export interface ApiPage<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }
export interface ListingDto {
  id: string;
  beekeeperId: string;
  apiaryId: string;
  title: string;
  description: string;
  category: string;
  status: string;
  location: string;
  currency: string;
  unitPrice: number | string;
  unitsTotal: number;
  unitsAvailable: number;
  createdAt: string;
}
export interface ApiaryDto { id: string; ownerId: string; name: string; location: string; description: string; status: string; createdAt: string }
export interface ColonyDto { id: string; apiaryId: string; code: string | null; count: number; strength: string | null; status: string; description: string; createdAt: string }
export interface InvestmentDto {
  id: string; listingId: string; listingTitle: string; location: string; unitsQuantity: number;
  amount: number | string; currency: string; status: string; reservationExpiresAt: string | null; createdAt: string;
}
export interface HarvestDto {
  id: string; apiaryId: string; colonyId: string | null; reportedBy: string; quantityKg: number | string;
  harvestedOn: string; note: string; status: string; reviewedBy: string | null; reviewedAt: string | null; createdAt: string;
}
export interface ExpenseDto {
  id: string; apiaryId: string; colonyId: string | null; reportedBy: string; category: string;
  amount: number | string; currency: string; spentOn: string; note: string; createdAt: string;
}
export interface SensorReadingDto { id: string; apiaryId: string; colonyId: string | null; metric: string; value: number | string; unit: string; source: string; recordedAt: string }
export interface NotificationDto { id: string; title: string; body: string; kind: string; readAt: string | null; createdAt: string }
export interface AdminUserDto { id: string; phone: string; fullName: string | null; email: string | null; accountStatus: string; roles: string[]; createdAt: string }
export interface PaymentDto { id: string; investmentId: string; provider: string; providerTransactionId: string | null; amount: number | string; currency: string; status: string; paidAt: string | null; createdAt: string }
export interface AuditLogDto { id: string; actorId: string | null; action: string; entityType: string; entityId: string | null; outcome: string; details: Record<string, unknown>; createdAt: string }
export interface InvestorReportDto {
  pendingOrders: number; activeInvestments: number; cancelledOrders: number;
  pendingOrderAmount: number | string; activePrincipalAmount: number | string;
  investments: InvestmentDto[]; apiaryReports: HarvestDto[]; apiaryExpenses: ExpenseDto[]; financialDisclaimer: string;
}
export interface BeekeeperReportDto {
  apiaryCount: number; colonyCount: number; activeListings: number; pendingListings: number;
  harvestKg: number | string; expensesUzs: number | string; from: string; to: string;
}
export interface AdminDashboardDto {
  totalUsers: number; activeUsers: number; investors: number; beekeepers: number; suspendedUsers: number;
  activeListings: number; pendingListings: number; pendingOrders: number; paymentReviewOrders: number;
  activeInvestments: number; pendingPayments: number; successfulPayments: number; submittedHarvestReports: number;
}
export interface AdminSystemStatusDto {
  timezone: string; demoMode: boolean; smsProvider: string; smsConfigured: boolean; otpCodeExposureEnabled: boolean;
  investmentTermsConfigured: boolean; paymentsEnabled: boolean; configuredPaymentProviders: string[];
  bootstrapAdminConfigured: boolean; pushConfigured: boolean; automaticIotConfigured: boolean;
}
export interface NotificationPreferencesDto {
  investmentUpdates: boolean; reportUpdates: boolean; marketingUpdates: boolean; pushEnabled: boolean; updatedAt: string;
}
export interface SupportTicketDto { id: string; userId: string; category: 'GENERAL' | 'COMPLAINT' | 'TECHNICAL'; subject: string; message: string; status: 'OPEN' | 'IN_REVIEW' | 'RESOLVED' | 'CLOSED'; adminNote: string; createdAt: string; updatedAt: string }

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || '/api/v1';
const ACCESS_KEY = 'melarium.access';
const REFRESH_KEY = 'melarium.refresh';
let accessToken = sessionStorage.getItem(ACCESS_KEY);
let refreshToken = sessionStorage.getItem(REFRESH_KEY);
let refreshInFlight: Promise<boolean> | null = null;

export function hasSession() { return Boolean(accessToken && refreshToken); }
export function saveSession(tokens: SessionTokens) {
  accessToken = tokens.accessToken;
  refreshToken = tokens.refreshToken;
  sessionStorage.setItem(ACCESS_KEY, tokens.accessToken);
  sessionStorage.setItem(REFRESH_KEY, tokens.refreshToken);
}
export function clearSession() {
  accessToken = null;
  refreshToken = null;
  sessionStorage.removeItem(ACCESS_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
}

function errorMessage(payload: unknown, fallback: string) {
  if (payload && typeof payload === 'object' && 'message' in payload && typeof (payload as { message?: unknown }).message === 'string') {
    return (payload as { message: string }).message;
  }
  return fallback;
}

async function refreshAccessToken(): Promise<boolean> {
  if (!refreshToken) return false;
  if (refreshInFlight) return refreshInFlight;
  refreshInFlight = (async () => {
    try {
      const response = await fetch(`${API_BASE}/auth/token/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ refreshToken }),
        cache: 'no-store',
      });
      if (!response.ok) { clearSession(); return false; }
      const tokens = await response.json() as SessionTokens;
      saveSession(tokens);
      return true;
    } catch {
      return false;
    } finally {
      refreshInFlight = null;
    }
  })();
  return refreshInFlight;
}

async function send<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers || {});
  headers.set('Accept', 'application/json');
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (accessToken && !path.startsWith('/auth/')) headers.set('Authorization', `Bearer ${accessToken}`);
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, { ...init, headers, cache: 'no-store' });
  } catch {
    throw new ApiClientError('Server bilan bog‘lanib bo‘lmadi. API manzili va backend holatini tekshiring.', 0);
  }
  if (response.status === 401 && retry && !path.startsWith('/auth/')) {
    const refreshed = await refreshAccessToken();
    if (refreshed) return send<T>(path, init, false);
    clearSession();
  }
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  let payload: unknown = null;
  if (text) {
    try { payload = JSON.parse(text); } catch { payload = text; }
  }
  if (!response.ok) {
    const details = payload && typeof payload === 'object' && 'details' in payload && Array.isArray((payload as { details?: unknown }).details)
      ? (payload as { details: ApiErrorDetail[] }).details : [];
    throw new ApiClientError(errorMessage(payload, `So‘rov bajarilmadi (${response.status})`), response.status, details);
  }
  return payload as T;
}

const json = (value: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(value) });
const patchJson = (value: unknown): RequestInit => ({ method: 'PATCH', body: JSON.stringify(value) });

export const api = {
  requestOtp: (phone: string) => send<OtpRequestResponse>('/auth/otp/request', json({ phone })),
  verifyOtp: (phone: string, code: string) => send<SessionTokens>('/auth/otp/verify', json({ phone, code })),
  async logout() {
    if (refreshToken) { try { await send<void>('/auth/logout', json({ refreshToken }), false); } finally { clearSession(); } }
    else clearSession();
  },
  me: () => send<UserProfile>('/users/me'),
  updateProfile: (fullName: string, email: string) => send<UserProfile>('/users/me', { method: 'PATCH', body: JSON.stringify({ fullName, email }) }),
  health: () => send<{ service: string; status: string; timestamp: string; timezone: string }>('/health'),
  listings: (query = '', page = 0, size = 50) => send<ApiPage<ListingDto>>(`/listings?query=${encodeURIComponent(query)}&page=${page}&size=${size}`),
  myListings: () => send<ListingDto[]>('/listings/my'),
  moderationQueue: (page = 0, size = 50) => send<ApiPage<ListingDto>>(`/listings/moderation?page=${page}&size=${size}`),
  createListing: (body: { apiaryId: string; title: string; description: string; location: string; unitPrice: number; unitsTotal: number }) => send<ListingDto>('/listings', json(body)),
  updateListing: (id: string, body: { title: string; description: string; location: string; unitPrice: number; unitsTotal: number }) => send<ListingDto>(`/listings/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  updateListingStatus: (id: string, status: 'ACTIVE' | 'DRAFT' | 'INACTIVE', note?: string) => send<ListingDto>(`/listings/${id}/status`, patchJson({ status, note })),
  apiaries: () => send<ApiaryDto[]>('/apiaries'),
  createApiary: (body: { name: string; location: string; description: string }) => send<ApiaryDto>('/apiaries', json(body)),
  updateApiary: (id: string, body: { name: string; location: string; description: string }) => send<ApiaryDto>(`/apiaries/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deactivateApiary: (id: string) => send<void>(`/apiaries/${id}`, { method: 'DELETE' }),
  colonies: (apiaryId: string) => send<ColonyDto[]>(`/colonies?apiaryId=${encodeURIComponent(apiaryId)}`),
  createColony: (apiaryId: string, body: { code?: string; count: number; strength?: string; description?: string }) => send<ColonyDto>(`/colonies?apiaryId=${encodeURIComponent(apiaryId)}`, json(body)),
  updateColony: (id: string, body: { code?: string; count: number; strength?: string; description?: string }) => send<ColonyDto>(`/colonies/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deactivateColony: (id: string) => send<void>(`/colonies/${id}`, { method: 'DELETE' }),
  investments: () => send<InvestmentDto[]>('/investments/my'),
  createOrder: (body: { listingId: string; unitsQuantity: number; idempotencyKey: string }) => send<InvestmentDto>('/investments', json(body)),
  cancelOrder: (id: string) => send<InvestmentDto>(`/investments/${id}`, { method: 'DELETE' }),
  initiatePayment: (body: { investmentId: string; provider: string; idempotencyKey: string }) => send<unknown>('/payments/initiate', json(body)),
  myPayments: (page = 0, size = 50) => send<ApiPage<PaymentDto>>(`/payments/my?page=${page}&size=${size}`),
  adminPayments: (page = 0, size = 50) => send<ApiPage<PaymentDto>>(`/payments/admin?page=${page}&size=${size}`),
  harvests: () => send<HarvestDto[]>('/harvests'),
  createHarvest: (body: { apiaryId: string; colonyId?: string; quantityKg: number; harvestedOn: string; note: string }) => send<HarvestDto>('/harvests', json(body)),
  expenses: () => send<ExpenseDto[]>('/expenses'),
  createExpense: (body: { apiaryId: string; colonyId?: string; category: string; amount: number; spentOn: string; note: string }) => send<ExpenseDto>('/expenses', json(body)),
  sensorReadings: (apiaryId: string, page = 0, size = 100) => send<ApiPage<SensorReadingDto>>(`/sensor-readings?apiaryId=${encodeURIComponent(apiaryId)}&page=${page}&size=${size}`),
  createSensorReading: (body: { apiaryId: string; colonyId?: string; metric: 'TEMPERATURE' | 'HUMIDITY' | 'WEIGHT'; value: number; unit: 'C' | '%' | 'kg'; source: 'MANUAL' | 'SIMULATED'; recordedAt: string }) => send<SensorReadingDto>('/sensor-readings', json(body)),
  reviewHarvest: (id: string, status: 'APPROVED' | 'RETURNED') => send<HarvestDto>(`/harvests/${id}/review`, patchJson({ status })),
  notifications: (page = 0, size = 50) => send<ApiPage<NotificationDto>>(`/notifications?page=${page}&size=${size}`),
  markNotificationRead: (id: string) => send<NotificationDto>(`/notifications/${id}/read`, { method: 'PATCH' }),
  markAllNotificationsRead: () => send<{ updated: number }>('/notifications/read-all', { method: 'PATCH' }),
  notificationPreferences: () => send<NotificationPreferencesDto>('/notifications/preferences'),
  updateNotificationPreferences: (body: Omit<NotificationPreferencesDto, 'updatedAt'>) => send<NotificationPreferencesDto>('/notifications/preferences', patchJson(body)),
  supportTickets: (page = 0, size = 50) => send<ApiPage<SupportTicketDto>>(`/support/tickets?page=${page}&size=${size}`),
  createSupportTicket: (body: { category: SupportTicketDto['category']; subject: string; message: string }) => send<SupportTicketDto>('/support/tickets', json(body)),
  adminSupportTickets: (status = '', page = 0, size = 50) => send<ApiPage<SupportTicketDto>>(`/admin/support/tickets${status ? `?status=${encodeURIComponent(status)}&page=${page}&size=${size}` : `?page=${page}&size=${size}`}`),
  reviewSupportTicket: (id: string, status: SupportTicketDto['status'], adminNote: string) => send<SupportTicketDto>(`/admin/support/tickets/${id}/status`, patchJson({ status, adminNote })),
  investorReport: () => send<InvestorReportDto>('/reports/investor'),
  beekeeperReport: (from?: string, to?: string) => send<BeekeeperReportDto>(`/reports/beekeeper${from || to ? `?${new URLSearchParams({ ...(from ? { from } : {}), ...(to ? { to } : {}) }).toString()}` : ''}`),
  adminDashboard: () => send<AdminDashboardDto>('/admin/dashboard'),
  adminSystemStatus: () => send<AdminSystemStatusDto>('/admin/system/status'),
  adminUsers: (query = '', page = 0, size = 50) => send<ApiPage<AdminUserDto>>(`/admin/users?query=${encodeURIComponent(query)}&page=${page}&size=${size}`),
  updateUserRoles: (id: string, roles: string[]) => send<AdminUserDto>(`/admin/users/${id}/roles`, patchJson({ roles })),
  updateUserStatus: (id: string, status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED') => send<AdminUserDto>(`/admin/users/${id}/status`, patchJson({ status })),
  adminInvestments: (page = 0, size = 50) => send<ApiPage<InvestmentDto>>(`/admin/investments?page=${page}&size=${size}`),
  auditLogs: (page = 0, size = 50) => send<ApiPage<AuditLogDto>>(`/admin/audit-logs?page=${page}&size=${size}`),
  async downloadReport(kind: 'investor' | 'beekeeper') {
    const path = `/reports/${kind}/export.csv`;
    const headers = new Headers({ Accept: 'text/csv' });
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
    let response = await fetch(`${API_BASE}${path}`, { headers, cache: 'no-store' });
    if (response.status === 401 && await refreshAccessToken()) {
      const retryHeaders = new Headers({ Accept: 'text/csv', Authorization: `Bearer ${accessToken}` });
      response = await fetch(`${API_BASE}${path}`, { headers: retryHeaders, cache: 'no-store' });
    }
    if (!response.ok) throw new ApiClientError('Hisobotni yuklab olib bo‘lmadi.', response.status);
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `melarium-${kind}-report.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  },
};
