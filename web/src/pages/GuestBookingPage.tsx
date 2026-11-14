import { useState } from "react";
import { useParams } from "react-router-dom";
import { useCancelGuestBooking, useGuestBooking } from "../hooks";
import "./styles/ProviderBookingsPage.css";
import "./styles/PublicBookingPages.css";

export function GuestBookingPage() {
  const { cancelToken = "" } = useParams();
  const [reason, setReason] = useState("");

  const { data: booking, isLoading, isError } = useGuestBooking(cancelToken);
  const cancelMutation = useCancelGuestBooking();

  const formatDateTime = (iso: string) =>
    new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(new Date(iso));

  if (isLoading) {
    return (
      <main className="public-page-container">
        <section className="public-card narrow">
          <div className="loading-state">Carregando reserva...</div>
        </section>
      </main>
    );
  }

  if (isError || !booking) {
    return (
      <main className="public-page-container">
        <section className="public-card narrow">
          <header className="public-header">
            <span className="public-badge">Área do Convidado</span>
            <h1>Reserva não encontrada</h1>
            <p>Verifique se o link utilizado está correto ou se o token é válido.</p>
          </header>
        </section>
      </main>
    );
  }

  return (
    <main className="public-page-container">
      <section className="public-card narrow">
        <header className="public-header">
          <span className="public-badge">Área do Convidado</span>
          <h1>Detalhes do Agendamento</h1>
        </header>

        <hr className="public-divider" />

        <div className="bookings-list">
          <div className="detail-box">
            <div className="detail-row">
              <span className="detail-label">Evento</span>
              <span className="detail-value">{booking.eventTitle}</span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Prestador</span>
              <span className="detail-value">{booking.providerName}</span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Cliente</span>
              <span className="detail-value">
                {booking.clientName} ({booking.clientEmail})
              </span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Data/Hora</span>
              <span className="detail-value">
                {formatDateTime(booking.startTime)} ({booking.durationMinutes} min)
              </span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Status</span>
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
          </div>

          {booking.status === "CANCELED" ? (
            <p className="booking-cancel-reason">
              <strong>Motivo do cancelamento: </strong>
              {booking.cancellationReason || "Não informado"}
            </p>
          ) : (
            <div className="guest-cancel-box">
              <div className="form-group">
                <label htmlFor="reason">
                  Deseja cancelar? Informe o motivo (opcional):
                </label>
                <textarea
                  id="reason"
                  className="form-input"
                  rows={3}
                  maxLength={500}
                  placeholder="Explique brevemente o motivo do cancelamento..."
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
              </div>

              <button
                type="button"
                className="btn-danger-full"
                disabled={cancelMutation.isPending}
                onClick={() =>
                  cancelMutation.mutate({
                    cancelToken,
                    payload: { reason: reason.trim() || undefined },
                  })
                }
              >
                {cancelMutation.isPending
                  ? "Cancelando..."
                  : "Cancelar meu agendamento"}
              </button>
            </div>
          )}
        </div>
      </section>
    </main>
  );
}

export default GuestBookingPage;
