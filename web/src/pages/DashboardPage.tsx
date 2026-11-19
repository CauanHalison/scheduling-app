import { Outlet } from "react-router-dom";
import { Navbar } from "../components";
import { useAuth } from "../hooks";

export function DashboardPage() {
  const { provider } = useAuth();

  if (!provider) {
    return null;
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="mx-auto w-full flex-1 px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}

export default DashboardPage;
