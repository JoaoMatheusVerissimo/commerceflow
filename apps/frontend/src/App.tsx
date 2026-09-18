import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { CatalogPage } from './catalog/CatalogPage'
import { ProductPage } from './catalog/ProductPage'
import { AdminCatalog } from './catalog/AdminCatalog'
import { CartPage } from './cart/CartPage'
import { FavoritesPage } from './cart/FavoritesPage'
import { CouponsPage } from './cart/CouponsPage'
import { OrdersPage } from './orders/OrdersPage'
import { OrderDetailPage } from './orders/OrderDetailPage'
import { InventoryPage } from './orders/InventoryPage'

export function App() {
  return <Routes>
    <Route path="/admin/coupons" element={<ProtectedRoute><CouponsPage /></ProtectedRoute>} />
    <Route path="/cart" element={<ProtectedRoute><CartPage key="cart" /></ProtectedRoute>} />
    <Route path="/checkout" element={<ProtectedRoute><CartPage key="checkout" checkout /></ProtectedRoute>} />
    <Route path="/favorites" element={<ProtectedRoute><FavoritesPage /></ProtectedRoute>} />
    <Route path="/account/orders" element={<ProtectedRoute><OrdersPage /></ProtectedRoute>} />
    <Route path="/account/orders/:id" element={<ProtectedRoute><OrderDetailPage /></ProtectedRoute>} />
    <Route path="/admin/orders" element={<ProtectedRoute><OrdersPage admin /></ProtectedRoute>} />
    <Route path="/admin/inventory" element={<ProtectedRoute><InventoryPage /></ProtectedRoute>} />
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
