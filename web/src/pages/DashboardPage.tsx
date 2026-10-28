import { Outlet } from "react-router-dom";
import { Navbar } from "../components";
import { useAuth } from "../hooks";

export function DashboardPage() {
  const { provider } = useAuth();

  if (!provider) {
    return null;
  }

  return (
    <div className="dashboard-container">
      <Navbar />

      <main className="dashboard-content">
        <Outlet />
      </main>
    </div>
  );
}

export default DashboardPage;
