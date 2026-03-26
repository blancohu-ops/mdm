import { Navigate, createBrowserRouter } from "react-router-dom";
import App from "@/App";
import { AuthLayout } from "@/components/backoffice/AuthLayout";
import { BackofficeShell } from "@/components/backoffice/BackofficeShell";
import { HomePage } from "@/pages/HomePage";
import { PlatformPage } from "@/pages/PlatformPage";
import { OnboardingPage } from "@/pages/OnboardingPage";
import { ProductDetailPage } from "@/pages/ProductDetailPage";
import { ProductsPage } from "@/pages/ProductsPage";
import { AiToolsPage } from "@/pages/AiToolsPage";
import { ProvidersPage } from "@/pages/marketplace/ProvidersPage";
import { ProviderDetailPage } from "@/pages/marketplace/ProviderDetailPage";
import { ProviderJoinPage } from "@/pages/marketplace/ProviderJoinPage";
import { ServiceDetailPage } from "@/pages/marketplace/ServiceDetailPage";
import { ServicesPage } from "@/pages/marketplace/ServicesPage";
import { ForgotPasswordPage } from "@/pages/auth/ForgotPasswordPage";
import { ActivateAccountPage } from "@/pages/auth/ActivateAccountPage";
import { LoginPage } from "@/pages/auth/LoginPage";
import { RegisterPage } from "@/pages/auth/RegisterPage";
import { AdminCategoryConfigPage } from "@/pages/admin/AdminCategoryConfigPage";
import { AdminCompanyManagementPage } from "@/pages/admin/AdminCompanyManagementPage";
import { AdminCompanyReviewDetailPage } from "@/pages/admin/AdminCompanyReviewDetailPage";
import { AdminCompanyReviewListPage } from "@/pages/admin/AdminCompanyReviewListPage";
import { AdminAccessGrantRequestsPage } from "@/pages/admin/AdminAccessGrantRequestsPage";
import { AdminOverviewPage } from "@/pages/admin/AdminOverviewPage";
import { AdminProductManagementPage } from "@/pages/admin/AdminProductManagementPage";
import { AdminProductReviewDetailPage } from "@/pages/admin/AdminProductReviewDetailPage";
import { AdminProductReviewListPage } from "@/pages/admin/AdminProductReviewListPage";
import { AdminReviewDomainAssignmentsPage } from "@/pages/admin/AdminReviewDomainAssignmentsPage";
import { AdminUserManagementPage } from "@/pages/admin/AdminUserManagementPage";
import {
  AdminFulfillmentPage,
  AdminMarketplacePublishPage,
  AdminPaymentsPage,
  AdminProviderReviewsPage,
  AdminProvidersPage,
  AdminServiceOrdersPage,
  AdminServicesPage,
} from "@/pages/admin/AdminMarketplacePages";
import { EnterpriseDashboardPage } from "@/pages/enterprise/EnterpriseDashboardPage";
import { EnterpriseImportPage } from "@/pages/enterprise/EnterpriseImportPage";
import { EnterpriseMessagesPage } from "@/pages/enterprise/EnterpriseMessagesPage";
import { EnterpriseOnboardingApplyPage } from "@/pages/enterprise/EnterpriseOnboardingApplyPage";
import { EnterpriseOnboardingSuccessPage } from "@/pages/enterprise/EnterpriseOnboardingSuccessPage";
import { EnterpriseProductEditorPage } from "@/pages/enterprise/EnterpriseProductEditorPage";
import { EnterpriseProductPreviewPage } from "@/pages/enterprise/EnterpriseProductPreviewPage";
import { EnterpriseProductsPage } from "@/pages/enterprise/EnterpriseProductsPage";
import { EnterpriseProfilePage } from "@/pages/enterprise/EnterpriseProfilePage";
import { EnterpriseSettingsPage } from "@/pages/enterprise/EnterpriseSettingsPage";
import {
  EnterpriseDeliveriesPage,
  EnterprisePaymentsPage,
  EnterpriseProductPromotionPage,
  EnterpriseServiceOrderDetailPage,
  EnterpriseServiceOrdersPage,
  EnterpriseServicesPage,
} from "@/pages/enterprise/EnterpriseServicesPage";
import {
  ProviderDashboardPage,
  ProviderFulfillmentPage,
  ProviderOrderDetailPage,
  ProviderOrdersPage,
  ProviderProfilePage,
  ProviderServicesPage,
} from "@/pages/provider/ProviderPages";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "platform", element: <PlatformPage /> },
      { path: "onboarding", element: <OnboardingPage /> },
      { path: "products", element: <ProductsPage /> },
      { path: "products/:id", element: <ProductDetailPage /> },
      { path: "services", element: <ServicesPage /> },
      { path: "services/:id", element: <ServiceDetailPage /> },
      { path: "providers", element: <ProvidersPage /> },
      { path: "providers/:id", element: <ProviderDetailPage /> },
      { path: "providers/join", element: <ProviderJoinPage /> },
      { path: "ai-tools", element: <AiToolsPage /> },
    ],
  },
  {
    path: "/auth",
    element: <AuthLayout />,
    children: [
      { index: true, element: <Navigate replace to="/auth/login" /> },
      { path: "login", element: <LoginPage /> },
      { path: "register", element: <RegisterPage /> },
      { path: "activate", element: <ActivateAccountPage /> },
      { path: "forgot-password", element: <ForgotPasswordPage /> },
    ],
  },
  {
    path: "/enterprise",
    element: <BackofficeShell scope="enterprise" />,
    children: [
      { index: true, element: <Navigate replace to="/enterprise/dashboard" /> },
      { path: "dashboard", element: <EnterpriseDashboardPage /> },
      { path: "onboarding/apply", element: <EnterpriseOnboardingApplyPage /> },
      { path: "onboarding/submitted", element: <EnterpriseOnboardingSuccessPage /> },
      { path: "profile", element: <EnterpriseProfilePage /> },
      { path: "products", element: <EnterpriseProductsPage /> },
      { path: "products/new", element: <EnterpriseProductEditorPage /> },
      { path: "products/:id", element: <EnterpriseProductPreviewPage /> },
      { path: "products/:id/edit", element: <EnterpriseProductEditorPage /> },
      { path: "services", element: <EnterpriseServicesPage /> },
      { path: "orders", element: <EnterpriseServiceOrdersPage /> },
      { path: "orders/:id", element: <EnterpriseServiceOrderDetailPage /> },
      { path: "payments", element: <EnterprisePaymentsPage /> },
      { path: "deliveries", element: <EnterpriseDeliveriesPage /> },
      { path: "product-promotion", element: <EnterpriseProductPromotionPage /> },
      { path: "import", element: <EnterpriseImportPage /> },
      { path: "messages", element: <EnterpriseMessagesPage /> },
      { path: "settings", element: <EnterpriseSettingsPage /> },
    ],
  },
  {
    path: "/provider",
    element: <BackofficeShell scope="provider" />,
    children: [
      { index: true, element: <Navigate replace to="/provider/dashboard" /> },
      { path: "dashboard", element: <ProviderDashboardPage /> },
      { path: "profile", element: <ProviderProfilePage /> },
      { path: "services", element: <ProviderServicesPage /> },
      { path: "orders", element: <ProviderOrdersPage /> },
      { path: "orders/:id", element: <ProviderOrderDetailPage /> },
      { path: "fulfillment", element: <ProviderFulfillmentPage /> },
    ],
  },
  {
    path: "/admin",
    element: <BackofficeShell scope="admin" />,
    children: [
      { index: true, element: <Navigate replace to="/admin/overview" /> },
      { path: "overview", element: <AdminOverviewPage /> },
      { path: "users", element: <AdminUserManagementPage /> },
      { path: "services", element: <AdminServicesPage /> },
      { path: "service-orders", element: <AdminServiceOrdersPage /> },
      { path: "payments", element: <AdminPaymentsPage /> },
      { path: "providers", element: <AdminProvidersPage /> },
      { path: "provider-reviews", element: <AdminProviderReviewsPage /> },
      { path: "fulfillment", element: <AdminFulfillmentPage /> },
      { path: "marketplace-publish", element: <AdminMarketplacePublishPage /> },
      { path: "reviews/companies", element: <AdminCompanyReviewListPage /> },
      { path: "reviews/companies/:id", element: <AdminCompanyReviewDetailPage /> },
      { path: "companies", element: <AdminCompanyManagementPage /> },
      { path: "reviews/products", element: <AdminProductReviewListPage /> },
      { path: "reviews/products/:id", element: <AdminProductReviewDetailPage /> },
      { path: "products", element: <AdminProductManagementPage /> },
      { path: "iam/access-grant-requests", element: <AdminAccessGrantRequestsPage /> },
      { path: "iam/review-domains", element: <AdminReviewDomainAssignmentsPage /> },
      { path: "categories", element: <AdminCategoryConfigPage /> },
    ],
  },
]);
