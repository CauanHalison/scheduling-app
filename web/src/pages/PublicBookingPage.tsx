import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useParams } from "react-router-dom";
import {
  useAvailableDays,
  useAvailableSlots,
  useCreateBookings,
  usePublicEventDetails,
} from "../hooks";
import {
  createBookingSchema,
  type CreateBookingFormData,
} from "../schemas/bookingSchema";
import type { BookingConfirmationResponse } from "../types";
import "./styles/PublicBookingPages.css";

export function PublicBookingPage() {
  const { userSlug = "", eventSlug = "" } = useParams();
  const clientTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;

  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [selectedDate, setSelectedDate] = useState("");
  const [confirmation, setConfirmation] =
    useState<BookingConfirmationResponse | null>(null);

  const { data: eventDetails, isLoading: loadingEvent, isError: errorEvent } =
    usePublicEventDetails(userSlug, eventSlug);

  const { data: daysData, isLoading: loadingDays } = useAvailableDays(
    userSlug,
    eventSlug,
    { year, month, timeZone: clientTimeZone }
  );

  const { data: slotsData, isLoading: loadingSlots } = useAvailableSlots(
    userSlug,
    eventSlug,
    { date: selectedDate, timeZone: clientTimeZone }
  );

  const createBookingMutation = useCreateBookings(userSlug, eventSlug);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CreateBookingFormData>({
    resolver: zodResolver(createBookingSchema),
    defaultValues: {
      clientName: "",
      clientEmail: "",
      startTime: "",
    },
  });

  const selectedSlot = watch("startTime");

  const onSubmit = (formData: CreateBookingFormData) => {
    createBookingMutation.mutate(formData, {
      onSuccess: (created) => {
        setConfirmation(created);
      },
    });
  };

  const formatTime = (iso: string) =>
    new Intl.DateTimeFormat("pt-BR", {
      hour: "2-digit",
      minute: "2-digit",
      timeZone: clientTimeZone,
    }).format(new Date(iso));

  const formatFullDateTime = (iso: string) =>
    new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
      timeZone: clientTimeZone,
    }).format(new Date(iso));

  if (loadingEvent) {
    return (
      <main className="public-page-container">
        <section className="public-card narrow">
          <div className="loading-state">Carregando evento...</div>
        </section>
      </main>
    );
  }

  if (errorEvent || !eventDetails) {
    return (
      <main className="public-page-container">
        <section className="public-card narrow">
          <div className="error-state">Evento não encontrado ou indisponível.</div>
          <Link to={`/users/${userSlug}/events`} className="btn-secondary-link">
            Ver outros eventos do prestador
          </Link>
        </section>
      </main>
    );
  }

  if (confirmation) {
    return (
      <main className="public-page-container">
        <section className="public-card narrow">
          <header className="confirmation-header">
            <span className="public-badge">Reserva Confirmada</span>
            <h1>✅ Agendamento Confirmado!</h1>
            <p>
              Sua reserva foi registrada com sucesso. Guarde o link abaixo caso
              precise gerenciar ou cancelar o horário.
            </p>
          </header>

          <div className="detail-box">
            <div className="detail-row">
              <span className="detail-label">Evento</span>
              <span className="detail-value">{confirmation.eventTitle}</span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Prestador</span>
              <span className="detail-value">{confirmation.providerName}</span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Cliente</span>
              <span className="detail-value">
                {confirmation.clientName} ({confirmation.clientEmail})
              </span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Início</span>
              <span className="detail-value">
                {formatFullDateTime(confirmation.startTime)}
              </span>
            </div>
          </div>

          <Link
            to={`/guest/bookings/${confirmation.cancelToken}`}
            className="btn-secondary-link"
          >
            Gerenciar ou cancelar este agendamento
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className="public-page-container">
      <section className="public-card">
        <header className="public-header">
          <Link to={`/users/${userSlug}/events`} className="public-back-link">
            ← Voltar para os eventos
          </Link>
          <span className="public-badge">@{eventDetails.providerName}</span>
          <h1>{eventDetails.eventTitle}</h1>
          {eventDetails.description && <p>{eventDetails.description}</p>}
          <div className="public-meta-pills">
            <span>⏱️ {eventDetails.durationMinutes} min</span>
            <span>🌍 Fuso: {clientTimeZone}</span>
          </div>
        </header>

        <hr className="public-divider" />

        <div className="booking-step-section">
          <div className="booking-step-header">
            <h3>
              Datas disponíveis ({String(month).padStart(2, "0")}/{year})
            </h3>
            <div className="month-nav-btns">
              <button
                type="button"
                className="btn-month-nav"
                onClick={() => {
                  if (month === 1) {
                    setMonth(12);
                    setYear((y) => y - 1);
                  } else {
                    setMonth((m) => m - 1);
                  }
                  setSelectedDate("");
                  setValue("startTime", "");
                }}
              >
                ◀ Mês anterior
              </button>
              <button
                type="button"
                className="btn-month-nav"
                onClick={() => {
                  if (month === 12) {
                    setMonth(1);
                    setYear((y) => y + 1);
                  } else {
                    setMonth((m) => m + 1);
                  }
                  setSelectedDate("");
                  setValue("startTime", "");
                }}
              >
                Próximo mês ▶
              </button>
            </div>
          </div>

          {loadingDays ? (
            <div className="loading-state">Buscando dias disponíveis...</div>
          ) : !daysData || daysData.availableDays.length === 0 ? (
            <div className="public-empty-box">
              <p>Nenhum dia disponível neste mês.</p>
            </div>
          ) : (
            <div className="days-grid">
              {daysData.availableDays.map((day) => (
                <button
                  key={day}
                  type="button"
                  className={`choice-btn ${selectedDate === day ? "active" : ""}`}
                  onClick={() => {
                    setSelectedDate(day);
                    setValue("startTime", "");
                  }}
                >
                  {day.split("-").reverse().join("/")}
                </button>
              ))}
            </div>
          )}
        </div>

        {selectedDate && (
          <div className="booking-step-section">
            <h3>Horários para {selectedDate.split("-").reverse().join("/")}</h3>
            {loadingSlots ? (
              <div className="loading-state">Carregando horários...</div>
            ) : !slotsData || slotsData.slots.length === 0 ? (
              <div className="public-empty-box">
                <p>Sem horários livres para esta data.</p>
              </div>
            ) : (
              <div className="slots-grid">
                {slotsData.slots.map((slot) => (
                  <button
                    key={slot.startTime}
                    type="button"
                    className={`choice-btn ${
                      selectedSlot === slot.startTime ? "active" : ""
                    }`}
                    onClick={() =>
                      setValue("startTime", slot.startTime, { shouldValidate: true })
                    }
                  >
                    {formatTime(slot.startTime)}
                  </button>
                ))}
              </div>
            )}
            {errors.startTime && (
              <span className="field-error">{errors.startTime.message}</span>
            )}
          </div>
        )}

        {selectedSlot && (
          <>
            <hr className="public-divider" />
            <form
              onSubmit={handleSubmit(onSubmit)}
              className="booking-step-section login-form"
            >
              <h3>Seus dados</h3>
              <div className="form-group">
                <label htmlFor="clientName">Seu nome</label>
                <input
                  id="clientName"
                  className="form-input"
                  placeholder="Nome completo"
                  {...register("clientName")}
                />
                {errors.clientName && (
                  <span className="field-error">{errors.clientName.message}</span>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="clientEmail">Seu e-mail</label>
                <input
                  id="clientEmail"
                  type="email"
                  className="form-input"
                  placeholder="voce@exemplo.com"
                  {...register("clientEmail")}
                />
                {errors.clientEmail && (
                  <span className="field-error">{errors.clientEmail.message}</span>
                )}
              </div>

              <button
                type="submit"
                className="login-submit"
                disabled={createBookingMutation.isPending}
              >
                {createBookingMutation.isPending
                  ? "Confirmando..."
                  : "Confirmar Agendamento"}
              </button>
            </form>
          </>
        )}
      </section>
    </main>
  );
}

export default PublicBookingPage;
