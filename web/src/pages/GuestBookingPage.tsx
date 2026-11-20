import { useState } from "react";
import { useParams } from "react-router-dom";
import { useCancelGuestBooking, useGuestBooking } from "../hooks";

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
    return <div className="loading-state">Carregando reserva...</div>;
  }

  if (isError || !booking) {
    return (
      <div className="mx-auto w-full max-w-xl py-4">
        <section className="card items-center text-center gap-3 p-6 sm:p-8">
          <h1 className="page-title">Reserva não encontrada</h1>
          <p className="page-subtitle">
            Verifique se o link utilizado está correto ou se o token é válido.
          </p>
        </section>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-xl py-4">
      <section className="card gap-6 p-6 sm:p-8">
        <header className="flex flex-col items-center gap-2 text-center">
          <h1 className="page-title">Detalhes do Agendamento</h1>
        </header>

        <div className="flex flex-col divide-y divide-slate-200 rounded-xl border border-slate-200 bg-slate-50/70 px-4">
          <div className="flex items-center justify-between gap-4 py-3 text-sm">
            <span className="text-slate-500">Evento</span>
            <span className="font-semibold text-slate-900 text-right">
              {booking.eventTitle}
            </span>
          </div>

          <div className="flex items-center justify-between gap-4 py-3 text-sm">
            <span className="text-slate-500">Prestador</span>
            <span className="font-semibold text-slate-900 text-right">
              {booking.providerName}
            </span>
          </div>

          <div className="flex items-center justify-between gap-4 py-3 text-sm">
            <span className="text-slate-500">Cliente</span>
            <span className="font-semibold text-slate-900 text-right">
              {booking.clientName} ({booking.clientEmail})
            </span>
          </div>

          <div className="flex items-center justify-between gap-4 py-3 text-sm">
            <span className="text-slate-500">Data/Hora</span>
            <span className="font-semibold text-slate-900 text-right">
              {formatDateTime(booking.startTime)} ({booking.durationMinutes} min)
            </span>
          </div>

          <div className="flex items-center justify-between gap-4 py-3 text-sm">
            <span className="text-slate-500">Status</span>
            <span
              className={`rounded-lg border-2 px-3 py-0.5 text-xs font-semibold ${
                booking.status === "CONFIRMED"
                  ? "border-green-700 bg-green-100 text-green-700"
                  : "border-red-700 bg-red-100 text-red-700"
              }`}
            >
              {booking.status === "CONFIRMED" ? "Confirmado" : "Cancelado"}
            </span>
          </div>
        </div>

        {booking.status === "CANCELED" ? (
          <p className="rounded-xl border border-dashed border-red-300 bg-red-50/70 px-3 py-2 text-xs text-red-800">
            <strong className="font-semibold">Motivo do cancelamento: </strong>
            <span>{booking.cancellationReason || "Não informado"}</span>
          </p>
        ) : (
          <div className="flex flex-col gap-4 border-t border-slate-200 pt-4">
            <div className="form-group">
              <label className="form-label" htmlFor="reason">
                Deseja cancelar? Informe o motivo (opcional):
              </label>
              <textarea
                className="input-field"
                id="reason"
                rows={3}
                maxLength={500}
                placeholder="Explique brevemente o motivo do cancelamento..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            </div>

            <button
              className="btn-danger w-full"
              type="button"
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
      </section>
    </div>
  );
}

export default GuestBookingPage;
