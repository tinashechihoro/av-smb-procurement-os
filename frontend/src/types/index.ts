export interface AuthResponse {
  userId: string;
  email: string;
  fullName: string;
  organisationId: string;
  organisationName: string;
  orgType: 'BUYER' | 'SUPPLIER';
  roleCode: string;
  roleName: string;
  permissions: string[];
  accessToken: string;
  refreshToken: string;
}

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  organisationId: string;
  organisationName?: string;
  roles: Role[];
}

export interface Role {
  id: string;
  name: string;
  code: string;
  permissions: string[];
}

export interface Vehicle {
  id: string;
  registration: string;
  make: string;
  model: string;
  year?: number;
  color?: string;
  status: string;
  customerId?: string;
  insurerId?: string;
}

export interface RepairJob {
  id: string;
  vehicleId: string;
  jobNumber: string;
  title: string;
  status: string;
  repairStage: string;
  priority: string;
  assignedTechnician?: string;
  estimatedTotal: number;
  actualTotal: number;
}

export interface Requisition {
  id: string;
  requisitionNumber: string;
  title: string;
  status: string;
  priority: string;
  totalEstimate: number;
  items: RequisitionItem[];
}

export interface RequisitionItem {
  id: string;
  lineNumber: number;
  partNumber?: string;
  description: string;
  quantity: number;
  estimatedCost?: number;
}

export interface Quotation {
  id: string;
  quotationNumber: string;
  title?: string;
  status: string;
  currentVersion: number;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  items: QuotationItem[];
}

export interface QuotationItem {
  id: string;
  lineNumber: number;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  status: string;
  totalAmount: number;
  items: OrderItem[];
}

export interface OrderItem {
  id: string;
  lineNumber?: number;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  deliveredQty: number;
}

export interface Supplier {
  id: string;
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  isActive: boolean;
}

export interface InventoryItem {
  id: string;
  partNumber: string;
  description: string;
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
  reorderLevel: number;
  unitCost?: number;
  sellingPrice?: number;
  category?: string;
  status: string;
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  status: string;
  fromEntity: string;
  toEntity: string;
  totalAmount: number;
  amountPaid: number;
  balanceDue: number;
  invoiceDate: string;
  dueDate?: string;
  items?: { id: string; lineNumber: number; description: string; quantity: number; unitPrice: number; lineTotal: number }[];
}

export interface Payment {
  id: string;
  paymentNumber: string;
  paymentType: string;
  amount: number;
  paymentMethod: string;
  reference?: string;
  status: string;
  paymentDate: string;
}

export interface CashbookEntry {
  id: string;
  entryType: string;
  amount: number;
  counterparty?: string;
  narration?: string;
  runningBalance?: number;
  reconciliationStatus: string;
  entryDate: string;
}

export interface Journal {
  id: string;
  journalNumber: string;
  journalType: string;
  description?: string;
  totalDebit: number;
  totalCredit: number;
  status: string;
  lines: JournalLine[];
}

export interface JournalLine {
  id: string;
  lineNumber: number;
  accountId: string;
  description?: string;
  debitAmount: number;
  creditAmount: number;
}

export interface ChartOfAccount {
  id: string;
  accountCode: string;
  accountName: string;
  accountType: string;
  subType?: string;
  currentBalance: number;
  normalBalance: string;
  isPostable?: boolean;
}

export interface AuditEntry {
  id: number;
  userId: string;
  action: string;
  entityType: string;
  entityId: string;
  createdAt: string;
}

export interface Notification {
  id: string;
  title: string;
  message?: string;
  notificationType: string;
  isRead: boolean;
  createdAt: string;
}
