import { Outlet } from "react-router-dom";
import { Footer } from "@/components/layout/Footer";
import { ScrollToTop } from "@/components/layout/ScrollToTop";
import { TopNav } from "@/components/layout/TopNav";

export function AppShell() {
  return (
    <div className="min-h-screen">
      <ScrollToTop />
      <TopNav />
      <main className="pt-20">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
