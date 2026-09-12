import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { CatalogPage } from './catalog/CatalogPage'
import { ProductPage } from './catalog/ProductPage'
import { AdminCatalog } from './catalog/AdminCatalog'

export function App() {
  return <Routes>
    <Route path="/" element={<CatalogPage home />} />
    <Route path="/products" element={<CatalogPage />} />
    <Route path="/products/:slug" element={<ProductPage />} />
    <Route path="/categories/:slug" element={<CatalogPage />} />
    <Route path="/account" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
    <Route path="/admin/products" element={<ProtectedRoute><AdminCatalog key="list" /></ProtectedRoute>} />
    <Route path="/admin/products/new" element={<ProtectedRoute><AdminCatalog key="new" mode="new" /></ProtectedRoute>} />
    <Route path="/admin/products/:id/edit" element={<ProtectedRoute><AdminCatalog key="edit" mode="edit" /></ProtectedRoute>} />
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route path="/app" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
}
