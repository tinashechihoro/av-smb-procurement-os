import api from './client';
import type {
  AuthResponse, Vehicle, RepairJob, Requisition, Quotation, Order,
  Supplier, InventoryItem, Invoice, Payment, CashbookEntry, Journal,
  ChartOfAccount, AuditEntry, Notification
} from '../types';

// Auth
export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }),
  me: () => api.get('/auth/me'),
};

// Dashboard
export const dashboardApi = {
  stats: () => api.get('/dashboard/stats'),
  financeSummary: () => api.get('/dashboard/finance-summary'),
};

// Workshop
export const vehicleApi = {
  list: () => api.get<Vehicle[]>('/vehicles'),
  get: (id: string) => api.get<Vehicle>(`/vehicles/${id}`),
  create: (data: Partial<Vehicle>) => api.post<Vehicle>('/vehicles', data),
};

export const repairJobApi = {
  list: () => api.get<RepairJob[]>('/repair-jobs'),
  get: (id: string) => api.get<RepairJob>(`/repair-jobs/${id}`),
  create: (data: any) => api.post<RepairJob>('/repair-jobs', data),
  updateStatus: (id: string, data: { status: string }) =>
    api.patch<RepairJob>(`/repair-jobs/${id}/status`, data),
};

// Procurement
export const requisitionApi = {
  list: () => api.get<Requisition[]>('/requisitions'),
  get: (id: string) => api.get<Requisition>(`/requisitions/${id}`),
  create: (data: any) => api.post<Requisition>('/requisitions', data),
  submit: (id: string) => api.post<Requisition>(`/requisitions/${id}/submit`),
};

export const quotationApi = {
  list: () => api.get<Quotation[]>('/quotations'),
  create: (data: any) => api.post<Quotation>('/quotations', data),
  approve: (id: string) => api.post<Quotation>(`/quotations/${id}/approve`),
  convertToOrder: (id: string) => api.post<Order>(`/quotations/${id}/convert-to-order`),
};

export const orderApi = {
  list: () => api.get<Order[]>('/orders'),
  get: (id: string) => api.get<Order>(`/orders/${id}`),
};

// Purchasing
export const supplierApi = {
  list: () => api.get<Supplier[]>('/suppliers'),
  create: (data: any) => api.post<Supplier>('/suppliers', data),
};

export const supplierPoApi = {
  list: () => api.get('/supplier-pos'),
  create: (data: any) => api.post('/supplier-pos', data),
};

// Inventory
export const inventoryApi = {
  list: () => api.get<InventoryItem[]>('/inventory'),
  create: (data: any) => api.post<InventoryItem>('/inventory', data),
};

export const goodsReceiptApi = { list: () => api.get('/goods-receipts') };

export const deliveryApi = {
  list: () => api.get('/delivery-notes'),
  create: (data: any) => api.post('/delivery-notes', data),
};

// Finance
export const invoiceApi = {
  list: () => api.get<Invoice[]>('/invoices'),
  create: (data: any) => api.post<Invoice>('/invoices', data),
};

export const paymentApi = {
  list: () => api.get<Payment[]>('/payments'),
  create: (data: any) => api.post<Payment>('/payments', data),
};

export const cashbookApi = {
  list: () => api.get<CashbookEntry[]>('/cashbook'),
  create: (data: any) => api.post<CashbookEntry>('/cashbook', data),
};

export const journalApi = {
  list: () => api.get<Journal[]>('/journals'),
  create: (data: any) => api.post<Journal>('/journals', data),
  post: (id: string) => api.post<Journal>(`/journals/${id}/post`),
};

export const chartOfAccountsApi = { list: () => api.get<ChartOfAccount[]>('/chart-of-accounts') };
export const cashAccountsApi = { list: () => api.get('/cash-accounts') };

// Platform
export const auditApi = {
  list: (params?: Record<string, string>) => api.get<AuditEntry[]>('/audit', { params }),
};

export const notificationApi = {
  list: () => api.get<Notification[]>('/notifications'),
  count: () => api.get<{ unread: number }>('/notifications/count'),
  markRead: (id: string) => api.post(`/notifications/${id}/read`),
};

// Reports
export const reportApi = {
  trialBalance: () => api.get('/reports/trial-balance'),
  debtorAgeing: () => api.get('/reports/debtor-ageing'),
  jobCost: () => api.get('/reports/job-cost'),
  inventoryValuation: () => api.get('/reports/inventory-valuation'),
  procurementPipeline: () => api.get('/reports/procurement-pipeline'),
};
