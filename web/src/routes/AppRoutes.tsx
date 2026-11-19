import { Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { useAuth } from "../hooks";
import {
  DashboardPage,
  EventTypesPage,
  GuestBookingPage,
  GuestMagicLinkPage,
  LoginPage,
  ProviderBookingsPage,
  PublicBookingPage,
  PublicProviderCatalog,
  SchedulePage,
} from "../pages";

interface LocationState {
  from?: {
    pathname: string;
  };
}

function LoadingScreen() {
  return (
    <main>
      <div>
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

      <Route path="/users/:userSlug/events" element={<PublicProviderCatalog />} />
      <Route path="/users/:userSlug/events/:eventSlug" element={<PublicBookingPage />} />
      <Route path="/guest/bookings/:cancelToken" element={<GuestBookingPage />} />
      <Route path="/guest/magic-link" element={<GuestMagicLinkPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPage />}>
          <Route index element={<ProviderBookingsPage />} />
          <Route path="bookings" element={<ProviderBookingsPage />} />
          <Route path="event-types" element={<EventTypesPage />} />
          <Route path="schedules" element={<SchedulePage />} />
        </Route>
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default AppRoutes;
