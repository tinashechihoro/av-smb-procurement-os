import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

const resources = {
  en: {
    translation: {
      app: { title: 'Procurement OS', subtitle: 'AV Motors × SMB' },
      nav: {
        dashboard: 'Command Centre', vehicles: 'Vehicles & Jobs', repairJobs: 'Repair Jobs',
        requisitions: 'Requisitions', quotations: 'Quotations', orders: 'Orders',
        suppliers: 'Suppliers', supplierPOs: 'Supplier Orders', inventory: 'Inventory',
        goodsReceipts: 'Goods Receipt', deliveries: 'Deliveries', invoices: 'Invoices',
        payments: 'Payments', cashbook: 'Cashbook', generalLedger: 'General Ledger',
        reports: 'Reports', auditTrail: 'Audit Trail',
      },
      common: {
        search: 'Search…', create: 'Create', save: 'Save', cancel: 'Cancel',
        submit: 'Submit', approve: 'Approve', reject: 'Reject', export: 'Export',
        reset: 'Reset', loading: 'Loading…', noData: 'No data found',
        status: 'Status', amount: 'Amount', date: 'Date', actions: 'Actions',
      },
      auth: { login: 'Sign In', email: 'Email', password: 'Password', logout: 'Sign Out' },
      dashboard: {
        hero: 'Every vehicle. Every part. One accountable flow.',
        vehicles: 'Vehicles', jobs: 'Repair Jobs', requisitions: 'Requisitions',
        quotations: 'Quotations', orders: 'Orders', invoices: 'Invoices',
      },
    },
  },
  fr: {
    translation: {
      app: { title: 'OS Approvisionnement', subtitle: 'AV Motors × SMB' },
      nav: {
        dashboard: 'Centre de Commandement', vehicles: 'Véhicules & Travaux',
        requisitions: 'Réquisitions', quotations: 'Devis', orders: 'Commandes',
        invoices: 'Factures', reports: 'Rapports', auditTrail: 'Journal d\'Audit',
      },
      common: {
        search: 'Rechercher…', create: 'Créer', save: 'Enregistrer', cancel: 'Annuler',
        submit: 'Soumettre', approve: 'Approuver', loading: 'Chargement…',
      },
      auth: { login: 'Se Connecter', email: 'E-mail', password: 'Mot de passe', logout: 'Déconnexion' },
    },
  },
};

i18n.use(initReactI18next).init({
  resources,
  lng: 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
});

export default i18n;
