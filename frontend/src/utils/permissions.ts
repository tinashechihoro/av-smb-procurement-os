import { useAuthStore } from '../store/authStore';

// Feature-to-permission mapping
export const FEATURE_PERMISSIONS = {
  // Workshop
  'workshop.vehicles.view': 'vehicles.view',
  'workshop.vehicles.manage': 'vehicles.manage',
  'workshop.jobs.view': 'jobs.view',
  'workshop.jobs.manage': 'jobs.manage',
  'workshop.jobs.approve': 'jobs.approve',
  
  // Procurement
  'procurement.requisitions.view': 'requisitions.view',
  'procurement.requisitions.create': 'requisitions.create',
  'procurement.requisitions.submit': 'requisitions.submit',
  'procurement.requisitions.approve': 'requisitions.approve',
  'procurement.quotations.view': 'quotations.view',
  'procurement.quotations.create': 'quotations.create',
  'procurement.quotations.negotiate': 'quotations.negotiate',
  'procurement.quotations.approve': 'quotations.approve',
  'procurement.orders.view': 'orders.view',
  'procurement.orders.create': 'orders.create',
  'procurement.orders.approve': 'orders.approve',
  'procurement.orders.amend': 'orders.amend',
  
  // Purchasing
  'purchasing.suppliers.view': 'suppliers.view',
  'purchasing.suppliers.manage': 'suppliers.manage',
  'purchasing.pos.view': 'supplier_po.view',
  'purchasing.pos.create': 'supplier_po.create',
  'purchasing.pos.approve': 'supplier_po.approve',
  
  // Inventory
  'inventory.view': 'inventory.view',
  'inventory.manage': 'inventory.manage',
  'inventory.goods_receipt.create': 'goods_receipt.create',
  'inventory.delivery.create': 'delivery.create',
  
  // Finance
  'finance.invoices.view': 'invoices.view',
  'finance.invoices.create': 'invoices.create',
  'finance.invoices.approve': 'invoices.approve',
  'finance.payments.view': 'payments.view',
  'finance.payments.create': 'payments.create',
  'finance.payments.approve': 'payments.approve',
  'finance.cashbook.view': 'cashbook.view',
  'finance.cashbook.post': 'cashbook.post',
  'finance.cashbook.reconcile': 'cashbook.reconcile',
  'finance.gl.view': 'gl.view',
  'finance.gl.post_journal': 'gl.post_journal',
  
  // Platform
  'platform.reports.view': 'reports.view',
  'platform.audit.view': 'audit.view',
  'platform.users.manage': 'users.manage',
  'platform.roles.manage': 'roles.manage',
  'platform.notifications.manage': 'notifications.manage',
} as const;

export type FeatureKey = keyof typeof FEATURE_PERMISSIONS;

// Navigation menu items with permission requirements
export interface MenuItem {
  key: string;
  label: string;
  path: string;
  icon: string;
  permission?: string;
  children?: MenuItem[];
}

export const MENU_ITEMS: MenuItem[] = [
  { key: 'dashboard', label: 'Dashboard', path: '/', icon: 'dashboard' },
  {
    key: 'workshop',
    label: 'Workshop',
    path: '/workshop',
    icon: 'car',
    children: [
      { key: 'vehicles', label: 'Vehicles', path: '/vehicles', icon: 'car', permission: 'vehicles.view' },
      { key: 'jobs', label: 'Repair Jobs', path: '/repair-jobs', icon: 'wrench', permission: 'jobs.view' },
    ],
  },
  {
    key: 'procurement',
    label: 'Procurement',
    path: '/procurement',
    icon: 'cart',
    children: [
      { key: 'requisitions', label: 'Requisitions', path: '/requisitions', icon: 'list', permission: 'requisitions.view' },
      { key: 'quotations', label: 'Quotations', path: '/quotations', icon: 'file', permission: 'quotations.view' },
      { key: 'orders', label: 'Orders', path: '/orders', icon: 'package', permission: 'orders.view' },
    ],
  },
  {
    key: 'purchasing',
    label: 'Purchasing',
    path: '/purchasing',
    icon: 'truck',
    children: [
      { key: 'suppliers', label: 'Suppliers', path: '/suppliers', icon: 'building', permission: 'suppliers.view' },
      { key: 'supplier-pos', label: 'Supplier POs', path: '/supplier-pos', icon: 'file', permission: 'supplier_po.view' },
    ],
  },
  {
    key: 'inventory',
    label: 'Inventory',
    path: '/inventory',
    icon: 'box',
    children: [
      { key: 'inventory-items', label: 'Items', path: '/inventory', icon: 'box', permission: 'inventory.view' },
      { key: 'goods-receipts', label: 'Goods Receipts', path: '/goods-receipts', icon: 'check', permission: 'inventory.view' },
      { key: 'deliveries', label: 'Deliveries', path: '/deliveries', icon: 'truck', permission: 'inventory.view' },
    ],
  },
  {
    key: 'finance',
    label: 'Finance',
    path: '/finance',
    icon: 'dollar',
    children: [
      { key: 'invoices', label: 'Invoices', path: '/invoices', icon: 'file', permission: 'invoices.view' },
      { key: 'payments', label: 'Payments', path: '/payments', icon: 'credit-card', permission: 'payments.view' },
      { key: 'cashbook', label: 'Cashbook', path: '/cashbook', icon: 'book', permission: 'cashbook.view' },
      { key: 'general-ledger', label: 'General Ledger', path: '/general-ledger', icon: 'layers', permission: 'gl.view' },
    ],
  },
  {
    key: 'platform',
    label: 'Platform',
    path: '/platform',
    icon: 'settings',
    children: [
      { key: 'reports', label: 'Reports', path: '/reports', icon: 'bar-chart', permission: 'reports.view' },
      { key: 'audit-trail', label: 'Audit Trail', path: '/audit-trail', icon: 'shield', permission: 'audit.view' },
      { key: 'users', label: 'Users', path: '/users', icon: 'users', permission: 'users.manage' },
      { key: 'roles', label: 'Roles', path: '/roles', icon: 'lock', permission: 'roles.manage' },
    ],
  },
];

export function usePermission() {
  const user = useAuthStore((s) => s.user);
  const permissions = user?.permissions || [];

  const hasPermission = (permission: string | undefined): boolean => {
    if (!permission) return true;
    return permissions.includes(permission);
  };

  const hasAnyPermission = (perms: string[]): boolean => {
    return perms.some((p) => permissions.includes(p));
  };

  const hasAllPermissions = (perms: string[]): boolean => {
    return perms.every((p) => permissions.includes(p));
  };

  const canAccessFeature = (featureKey: FeatureKey): boolean => {
    const permission = FEATURE_PERMISSIONS[featureKey];
    return hasPermission(permission);
  };

  const getVisibleMenuItems = (items: MenuItem[] = MENU_ITEMS): MenuItem[] => {
    return items
      .filter((item) => hasPermission(item.permission))
      .map((item) => {
        if (item.children) {
          const visibleChildren = getVisibleMenuItems(item.children);
          if (visibleChildren.length === 0) return null;
          return { ...item, children: visibleChildren };
        }
        return item;
      })
      .filter((item): item is MenuItem => item !== null);
  };

  return {
    permissions,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    canAccessFeature,
    getVisibleMenuItems,
  };
}

// Actions that require OTP verification
export const OTP_REQUIRED_ACTIONS = {
  // Login is handled separately
  'approval.requisition': 'APPROVAL',
  'approval.quotation': 'APPROVAL',
  'approval.order': 'APPROVAL',
  'approval.invoice': 'APPROVAL',
  'approval.payment': 'APPROVAL',
  'approval.supplier_po': 'APPROVAL',
  'approval.job': 'APPROVAL',
  'signature.document': 'SIGNATURE',
  'finance.post_journal': 'APPROVAL',
  'user.create': 'APPROVAL',
  'user.update': 'APPROVAL',
  'user.deactivate': 'APPROVAL',
} as const;

export type OtpAction = keyof typeof OTP_REQUIRED_ACTIONS;

export function requiresOtp(action: string): boolean {
  return action in OTP_REQUIRED_ACTIONS;
}

export function getOtpPurpose(action: string): 'APPROVAL' | 'SIGNATURE' | 'MFA' | null {
  return (OTP_REQUIRED_ACTIONS as Record<string, string>)[action] as 'APPROVAL' | 'SIGNATURE' | 'MFA' || null;
}
