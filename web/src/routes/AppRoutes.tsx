import { Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { useAuth } from "../hooks";
import { DashboardPage, EventTypesPage, LoginPage } from "../pages";

interface LocationState {
  from?: {
    pathname: string;
  };
}

function LoadingScreen() {
  return (
    <main className="login-container">
      <div className="login-card" style={{ textAlign: "center" }}>
        <p>Carregando sessão...</p>
      </div>
    </main>
  );
}

export function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <LoadingScreen />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
}

export function PublicRoute() {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();
  const state = location.state as LocationState | null;
  const destination = state?.from?.pathname || "/dashboard";

  if (loading) {
    return <LoadingScreen />;
  }

  if (isAuthenticated) {
    return <Navigate to={destination} replace />;
  }

  return <Outlet />;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicRoute />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPage />}>
          <Route index element={<EventTypesPage />} />
          <Route path="event-types" element={<EventTypesPage />} />
        </Route>
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default AppRoutes;
