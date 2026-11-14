import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { CancelBookingModal } from "../components";
import { useCancelProviderBooking, useProviderBookings } from "../hooks";
import type { BookingStatus, ProviderBookingResponse } from "../types";
import "./styles/ProviderBookingsPage.css";

export function ProviderBookingsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const statusParam = (searchParams.get("status") as BookingStatus) || undefined;
  const pageParam = Number(searchParams.get("page") || 0);

  const [cancelingBooking, setCancelingBooking] = useState<ProviderBookingResponse | null>(null);

  const { data, isLoading, isError, isPlaceholderData } = useProviderBookings({
    status: statusParam,
    page: pageParam,
    size: 10,
    sort: "startTime,asc",
  });

  const cancelMutation = useCancelProviderBooking();

  const handleStatusChange = (status?: BookingStatus) => {
    const next = new URLSearchParams(searchParams);

    if (status) {
      next.set("status", status);
    } else {
      next.delete("status");
    }

    next.set("page", "0");
    setSearchParams(next);
  };

  const handlePageChange = (newPage: number) => {
    const next = new URLSearchParams(searchParams);

    if (newPage !== Number(next.get("page"))) {
      next.set("page", String(newPage));
      setSearchParams(next);
    }
  };

  const handleConfirmCancel = (reason?: string) => {
    if (!cancelingBooking) return;

    cancelMutation.mutate(
      {
        id: cancelingBooking.id,
        payload: { reason },
      },
      {
        onSuccess: () => {
          setCancelingBooking(null);
        },
      }
    );
  };

  const formatDateTime = (iso: string) => {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(new Date(iso));
  };

  if (isLoading) {
    return (
      <div className="bookings-page">
        <div className="loading-state">Carregando agendamentos...</div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="bookings-page">
        <div className="error-state">Ocorreu um erro ao carregar agendamentos.</div>
      </div>
    );
  }

  return (
    <div className="bookings-page">
      <header className="page-header">
        <div>
          <h1>Agendamentos</h1>
          <p>Acompanhe por aqui os horários reservados pelos seus clientes.</p>
        </div>
      </header>

      <div className="filter-tabs">
        <button
          type="button"
          className={`filter-tab ${!statusParam ? "active" : ""}`}
          onClick={() => handleStatusChange(undefined)}
        >
          Todos
        </button>

        <button
          type="button"
          className={`filter-tab ${statusParam === "CONFIRMED" ? "active" : ""}`}
          onClick={() => handleStatusChange("CONFIRMED")}
        >
          Confirmados
        </button>

        <button
          type="button"
          className={`filter-tab ${statusParam === "CANCELED" ? "active" : ""}`}
          onClick={() => handleStatusChange("CANCELED")}
        >
          Cancelados
        </button>
      </div>

      {data?.empty ? (
        <div className="bookings-empty">
          <p>Nenhum agendamento encontrado.</p>
        </div>
      ) : (
        <div
          className="bookings-list"
          style={{ opacity: isPlaceholderData ? 0.6 : 1 }}
        >
          {data?.content.map((booking) => (
            <article key={booking.id} className="booking-card">
              <div className="booking-card-header">
                <div>
                  <h3>{booking.eventTitle}</h3>
                  <span className="booking-client-info">
                    👤 {booking.clientName} ({booking.clientEmail})
                  </span>
                </div>
                <span
                  className={`booking-status-badge ${
                    booking.status === "CONFIRMED"
                      ? "status-confirmed"
                      : "status-canceled"
                  }`}
                >
                  {booking.status === "CONFIRMED" ? "Confirmado" : "Cancelado"}
                </span>
              </div>

              <div className="booking-meta">
                <span>🗓️ {formatDateTime(booking.startTime)}</span>
                <span>⏱️ {booking.durationMinutes} min</span>
              </div>

              {booking.cancellationReason && (
                <p className="booking-cancel-reason">
                  <strong>Motivo do cancelamento: </strong>
                  {booking.cancellationReason}
                </p>
              )}

              {booking.status === "CONFIRMED" && (
                <div className="booking-actions">
                  <button
                    type="button"
                    className="btn-danger"
                    onClick={() => setCancelingBooking(booking)}
                  >
                    Cancelar Agendamento
                  </button>
                </div>
              )}
            </article>
          ))}

          {data && data.totalPages > 1 && (
            <div className="pagination">
              <button
                type="button"
                className="pagination-btn"
                disabled={data.first}
                onClick={() => handlePageChange(pageParam - 1)}
              >
                Anterior
              </button>

              <span className="pagination-info">
                Página {data.number + 1} de {data.totalPages}
              </span>

              <button
                type="button"
                className="pagination-btn"
                disabled={data.last}
                onClick={() => handlePageChange(pageParam + 1)}
              >
                Próxima
              </button>
            </div>
          )}
        </div>
      )}

      {cancelingBooking && (
        <CancelBookingModal
          eventTitle={cancelingBooking.eventTitle}
          participantName={cancelingBooking.clientName}
          isPending={cancelMutation.isPending}
          onClose={() => setCancelingBooking(null)}
          onConfirm={handleConfirmCancel}
        />
      )}
    </div>
  );
}

export default ProviderBookingsPage;
