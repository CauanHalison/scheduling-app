import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { CancelBookingModal } from "../components";
import { useCancelProviderBooking, useEventTypes, useProviderBookings } from "../hooks";
import { Calendar, Clock, Link2, User } from 'lucide-react';
import type { BookingStatus, ProviderBookingResponse } from "../types";

export function ProviderBookingsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const statusParam = (searchParams.get("status") as BookingStatus) || undefined;
  const eventTypeIdParam = (searchParams.get("etid")) || undefined;
  const pageParam = Number(searchParams.get("page") || 0);
  const {
    data:eventTypes, 
    isLoading: isLoadingEventTYpes, 
    isError: isErrorEventTypes
  } = useEventTypes();

  const [cancelingBooking, setCancelingBooking] = useState<ProviderBookingResponse | null>(null);

  const { data, isLoading, isError } = useProviderBookings({
    status: statusParam,
    eventTypeId: eventTypeIdParam,
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

  const handleEventTypeChange = (eventTypeId?: string) => {
    const next = new URLSearchParams(searchParams)

    if(eventTypeId && eventTypeId !== eventTypeIdParam) {
      next.set("etid", eventTypeId)
    } else {
      next.delete("etid")
    }

    next.set("page", "0")
    setSearchParams(next);
  }

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

  const filterBtnClass = (status?: BookingStatus) => {
    const isActive = status === statusParam;

    return `cursor-pointer rounded-xl px-4 py-1 text-sm font-medium transition duration-200 ${
      isActive ?
        "bg-blue-600 text-white shadow-sm"
        : "border border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:bg-blue-50 hover:text-blue-600"
    }`
  } 

  if (isLoading) {
    return <div className="loading-state">Carregando agendamentos...</div>;
  }

  if (isError) {
    return (
      <div className="alert-error">Ocorreu um erro ao carregar agendamentos.</div>
    );
  }

  return (
    <div>
      <header>
        <div>
          <h1 className="page-title">Agendamentos</h1>
          <p className="page-subtitle">Acompanhe por aqui os horários reservados pelos seus clientes.</p>
        </div>
      </header>

      <div className="flex flex-row gap-3 pt-4">
        <button
          className={filterBtnClass(undefined)}
          type="button"
          onClick={() => handleStatusChange(undefined)}
        >
          Todos
        </button>

        <button
          className={filterBtnClass("CONFIRMED")}
          type="button"
          onClick={() => handleStatusChange("CONFIRMED")}
        >
          Confirmados
        </button>

        <button
          className={filterBtnClass("COMPLETED")}
          type="button"
          onClick={() => handleStatusChange("COMPLETED")}
        >
          Concluídos
        </button>

        <button
          className={filterBtnClass("CANCELED")}
          type="button"
          onClick={() => handleStatusChange("CANCELED")}
        >
          Cancelados
        </button>
      </div>

      {!isErrorEventTypes && (
        <select
          value={eventTypeIdParam || ""}
          disabled={isLoadingEventTYpes}
          onChange={(e) => handleEventTypeChange(e.target.value || undefined)}
          className="w-max cursor-pointer mt-3 rounded-xl border border-slate-200 bg-white px-3.5 py-1.5 text-sm font-medium text-slate-700 shadow-2xs transition duration-200 hover:border-blue-300 focus:border-blue-600 focus:outline-none focus:ring-3 focus:ring-blue-500/15 disabled:cursor-not-allowed disabled:opacity-60 sm:w-64"
        >
          <option className="cursor-pointer" value="">
            {isLoadingEventTYpes ? "Carregando eventos..." : "Todos os eventos"}
          </option>
          {eventTypes?.map((eventType) => (
            <option
              className="cursor-pointer"
              key={eventType.id}
              value={eventType.id}
            >
              {eventType.title}  
            </option>
          ))}
        </select>
      )}

      {data?.empty ? (
        <div className="empty-state">
          <p>Nenhum agendamento encontrado.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          <div className="grid grid-cols-1 gap-5 py-5 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {data?.content.map((booking) => (
              <article
                className="card"
                key={booking.id}
              >
                <div className="flex flex-col gap-3">
                  <div className="flex flex-row flex-nowrap items-start justify-between gap-2 w-full">
                    <h3 className="font-semibold text-base text-slate-900 leading-snug">{booking.eventTitle}</h3>
                    <span className={`shrink-0 font-semibold text-xs border px-3 py-0.5 flex items-center rounded-lg border-2 ${
                      booking.status === "CONFIRMED" ? 'border-green-700 bg-green-100 text-green-700' : 
                        ( booking.status === "CANCELED" ? 'border-red-700 bg-red-100 text-red-700' : 'border-slate-700 bg-slate-100 text-slate-700')}`
                    }>
                      {booking.status === "CONFIRMED" ? "Confirmado" : ( booking.status === "CANCELED" ? 'Cancelado' : 'Concluído')}
                    </span>
                  </div>

                  <span className="flex flex-row flex-nowrap items-center gap-2 text-sm text-slate-500 min-w-0">
                    <User className="h-4 w-4 shrink-0 text-slate-400"/>
                    <span className="truncate">
                      {booking.clientName} ({booking.clientEmail})
                    </span>
                  </span>

                  {booking.eventLink && (
                    <span className="flex flex-row flex-nowrap items-center gap-2 text-sm text-slate-500 min-w-0">
                      <Link2 className="h-4 w-4 shrink-0 text-blue-600"/>
                      <a
                        href={booking.eventLink}
                        className="truncate text-blue-600 hover:underline"
                      >
                        {booking.eventLink}
                      </a>
                    </span>
                  )}

                  <div className="flex flex-wrap items-center gap-4 pt-1 text-sm text-slate-600">
                    <span className="inline-flex items-center gap-2"><Calendar className="h-4 w-4 text-blue-600"/> {formatDateTime(booking.startTime)}</span>
                    <span className="inline-flex items-center gap-2"><Clock className="h-4 w-4 text-blue-600"/> {booking.durationMinutes} min</span>
                  </div>
                </div>

                {(booking.cancellationReason || booking.status === "CONFIRMED") && (
                  <div className="flex flex-col gap-3 border-t border-slate-200 pt-3">
                    {booking.cancellationReason && (
                      <p className="rounded-xl border border-dashed border-red-300 bg-red-50/70 px-3 py-2 text-xs text-red-800">
                        <strong className="font-semibold">Motivo do cancelamento: </strong>
                        <span>{booking.cancellationReason}</span>
                      </p>
                    )}

                    {booking.status === "CONFIRMED" && (
                      <button
                        className="btn-danger w-full py-2"
                        type="button"
                        onClick={() => setCancelingBooking(booking)}
                      >
                        Cancelar Agendamento
                      </button>
                    )}
                  </div>
                )}
              </article>
            ))}
          </div>

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 pt-2">
              <button
                className="btn-secondary"
                type="button"
                disabled={data.first}
                onClick={() => handlePageChange(pageParam - 1)}
              >
                Anterior
              </button>

              <span className="text-sm font-medium text-slate-600">
                Página {data.number + 1} de {data.totalPages}
              </span>

              <button
                className="btn-secondary"
                type="button"
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
