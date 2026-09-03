import { Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { useAuthStore } from './store/authStore';
import Layout from './components/layout/Layout';
import Login from './pages/auth/Login';
import Dashboard from './pages/Dashboard';
import Vehicles from './pages/workshop/Vehicles';
import RepairJobs from './pages/workshop/RepairJobs';
import Requisitions from './pages/procurement/Requisitions';
import Quotations from './pages/procurement/Quotations';
import Orders from './pages/procurement/Orders';
import Suppliers from './pages/purchasing/Suppliers';
import SupplierPOs from './pages/purchasing/SupplierPOs';
import Inventory from './pages/inventory/Inventory';
import GoodsReceipts from './pages/inventory/GoodsReceipts';
import Deliveries from './pages/inventory/Deliveries';
import Invoices from './pages/finance/Invoices';
import Payments from './pages/finance/Payments';
import Cashbook from './pages/finance/Cashbook';
import GeneralLedger from './pages/finance/GeneralLedger';
import AuditTrail from './pages/platform/AuditTrail';
import Reports from './pages/platform/Reports';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token);
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <>
      <Toaster position="bottom-right" />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Dashboard />} />
          <Route path="vehicles" element={<Vehicles />} />
          <Route path="repair-jobs" element={<RepairJobs />} />
          <Route path="requisitions" element={<Requisitions />} />
          <Route path="quotations" element={<Quotations />} />
          <Route path="orders" element={<Orders />} />
          <Route path="suppliers" element={<Suppliers />} />
          <Route path="supplier-pos" element={<SupplierPOs />} />
          <Route path="inventory" element={<Inventory />} />
          <Route path="goods-receipts" element={<GoodsReceipts />} />
          <Route path="deliveries" element={<Deliveries />} />
          <Route path="invoices" element={<Invoices />} />
          <Route path="payments" element={<Payments />} />
          <Route path="cashbook" element={<Cashbook />} />
          <Route path="general-ledger" element={<GeneralLedger />} />
          <Route path="audit-trail" element={<AuditTrail />} />
          <Route path="reports" element={<Reports />} />
        </Route>
      </Routes>
    </>
  );
}
