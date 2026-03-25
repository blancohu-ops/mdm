import { Navigate, createBrowserRouter } from "react-router-dom";
import App from "@/App";
import { AuthLayout } from "@/components/backoffice/AuthLayout";
import { BackofficeShell } from "@/components/backoffice/BackofficeShell";
import { HomePage } from "@/pages/HomePage";
import { PlatformPage } from "@/pages/PlatformPage";
import { OnboardingPage } from "@/pages/OnboardingPage";
import { ProductsPage } from "@/pages/ProductsPage";
import { AiToolsPage } from "@/pages/AiToolsPage";
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

export const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "platform", element: <PlatformPage /> },
      { path: "onboarding", element: <OnboardingPage /> },
      { path: "products", element: <ProductsPage /> },
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
      { path: "import", element: <EnterpriseImportPage /> },
      { path: "messages", element: <EnterpriseMessagesPage /> },
      { path: "settings", element: <EnterpriseSettingsPage /> },
    ],
  },
  {
    path: "/admin",
    element: <BackofficeShell scope="admin" />,
    children: [
      { index: true, element: <Navigate replace to="/admin/overview" /> },
      { path: "overview", element: <AdminOverviewPage /> },
      { path: "users", element: <AdminUserManagementPage /> },
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
