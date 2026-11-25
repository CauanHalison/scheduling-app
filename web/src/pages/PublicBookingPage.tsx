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
import { PublicHeader } from "../components/PublicHeader";
import { ArrowLeft } from "lucide-react";
import {DayPicker} from 'react-day-picker'
import "react-day-picker/style.css";
import { ptBR } from "date-fns/locale";
import { format } from "date-fns";

function parseDateKey(dateStr: string): Date {
  const [y, m, d] = dateStr.split("-").map(Number);
  return new Date(y, m - 1, d);
}

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

  const { data: daysData } = useAvailableDays(
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

  const availableDaysSet = new Set(daysData?.availableDays ?? [])

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
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <div className="loading-state">Carregando evento...</div>
      </main>
    );
  }

  if (errorEvent || !eventDetails) {
    return (
      <main className="mx-auto w-full max-w-xl flex-1 px-4 py-12">
        <section className="card items-center text-center">
          <div className="alert-error w-full">
            Evento não encontrado ou indisponível.
          </div>
          <Link to={`/users/${userSlug}/events`} className="btn-secondary">
            <ArrowLeft className="h-4 w-4" />
            Ver outros eventos do prestador
          </Link>
        </section>
      </main>
    );
  }

  if (confirmation) {
    return (
      <main className="mx-auto w-full max-w-xl flex-1 px-4 py-10">
        <section className="card gap-6 p-6 sm:p-8">
          <header className="flex flex-col items-center gap-2 text-center">
            <h1 className="page-title">Agendamento Confirmado!</h1>
            <p className="page-subtitle">
              Sua reserva foi registrada com sucesso. Guarde o link abaixo caso
              precise gerenciar ou cancelar o horário.
            </p>
          </header>

          <div className="flex flex-col divide-y divide-slate-200 rounded-xl border border-slate-200 bg-slate-50/70 px-4">
            <div className="flex items-center justify-between gap-4 py-3 text-sm">
              <span className="text-slate-500">Evento</span>
              <span className="font-semibold text-slate-900 text-right">
                {confirmation.eventTitle}
              </span>
            </div>
            <div className="flex items-center justify-between gap-4 py-3 text-sm">
              <span className="text-slate-500">Prestador</span>
              <span className="font-semibold text-slate-900 text-right">
                {confirmation.providerName}
              </span>
            </div>
            <div className="flex items-center justify-between gap-4 py-3 text-sm">
              <span className="text-slate-500">Cliente</span>
              <span className="font-semibold text-slate-900 text-right">
                {confirmation.clientName} ({confirmation.clientEmail})
              </span>
            </div>
            <div className="flex items-center justify-between gap-4 py-3 text-sm">
              <span className="text-slate-500">Início</span>
              <span className="font-semibold text-slate-900 text-right">
                {formatFullDateTime(confirmation.startTime)}
              </span>
            </div>
            {confirmation.eventLink && confirmation.status === "CONFIRMED" && (
              <div className="flex items-center justify-between gap-4 py-3 text-sm">
                <span className="text-slate-500">Link do evento</span>
                <a
                  href={confirmation.eventLink}
                  className="font-semibold text-blue-600 hover:underline text-right truncate max-w-[240px]"
                >
                  {confirmation.eventLink}
                </a>
              </div>
            )}
          </div>

          <div className="flex flex-col gap-2.5">
            <Link
              to={`/guest/bookings/${confirmation.cancelToken}`}
              className="btn-primary w-full"
            >
              Gerenciar ou cancelar este agendamento
            </Link>
            <Link
              to={`/users/${userSlug}/events`}
              className="btn-secondary w-full"
            >
              <ArrowLeft className="h-4 w-4" />
              Voltar para os eventos
            </Link>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main>
      <section>
        <div>
          <Link className="btn-secondary mb-3" to={`/users/${userSlug}/events`}>
            <ArrowLeft className="w-4 h-4" /> Voltar para os eventos
          </Link>
          <PublicHeader
            userSlug={eventDetails.providerSlug}
            headerTitle={eventDetails.eventTitle}
            headerDescription={
              eventDetails.description ||
              `Selecione o horário para marcar um atendimento com ${eventDetails.providerName}`
            }
            durationMinutes={eventDetails.durationMinutes}
          />
        </div>

        <div className="flex flex-col w-full gap-5 lg:flex-row lg:flex-nowrap lg:items-start">
          <div className="flex w-full shrink-0 flex-col items-center overflow-x-auto bg-white p-4 rounded-xl border border-slate-200 shadow-xs sm:px-6 sm:py-5 lg:w-auto">
            <h3 className="text-center font-semibold">
              Datas disponíveis para ({String(month).padStart(2, "0")}/{year})
            </h3>
            <DayPicker
              mode="single"
              locale={ptBR}
              month={new Date(year, month - 1, 1)}
              onMonthChange={(newMonth) => {
                setYear(newMonth.getFullYear());
                setMonth(newMonth.getMonth() + 1);
                setSelectedDate("");
                setValue("startTime", "");
              }}
              selected={selectedDate ? parseDateKey(selectedDate) : undefined}
              onSelect={(date) => {
                if (!date) return;
                setSelectedDate(format(date, "yyyy-MM-dd"));
                setValue("startTime", "");
              }}
              disabled={(date) => {
                return !availableDaysSet.has(format(date, "yyyy-MM-dd"));
              }}
              modifiers={{
                available: (date) => availableDaysSet.has(format(date, "yyyy-MM-dd")),
              }}
              modifiersClassNames={{
                available: "rdp-day-available",
              }}
            />
          </div>
          
          <div className="flex flex-col gap-5 w-full">
            {!selectedDate ? (
              <p className="text-center text-sm text-slate-500">
                Selecione uma data no calendário para ver os horários.
              </p>
            ) : (
              <div className="bg-white px-8 py-4 rounded-xl border border-slate-200 shadow-xs w-full">
                {selectedDate && (
                  <div>
                    <h3 className="mb-3 font-semibold text-center pb-2 border-b border-slate-200">
                      Horários disponíveis para {selectedDate.split("-").reverse().join("/")}
                    </h3>
                    {loadingSlots ? (
                      <div>Carregando horários...</div>
                    ) : !slotsData || slotsData.slots.length === 0 ? (
                      <div className="empty-state">
                        <p>Sem horários livres para esta data.</p>
                      </div>
                    ) : (
                      <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 xl:grid-cols-10">
                        {slotsData.slots.map((slot) => (
                          <button
                            className={`py-1 ${slot.startTime === selectedSlot ? 'btn-primary ' : 'btn-secondary'}`}
                            key={slot.startTime}
                            type="button"
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
                      <span>{errors.startTime.message}</span>
                    )}
                  </div>
                )}
              </div>
            )}

            <div>
              {selectedSlot && (
                <>
                  <form 
                    className="flex flex-col gap-3 w-full bg-white px-8 py-4 border border-slate-200 rounded-xl sm:px-8 sm:py-5"
                    onSubmit={handleSubmit(onSubmit)}
                  >
                    <div className="px-3 py-2 text-lg text-center font-semibold text-slate-900 border-b border-slate-200">
                      <span>Confirmar agendamento</span>
                    </div>
                    <div className="form-group">
                      <label className="form-label" htmlFor="clientName">Seu nome</label>
                      <input
                        className="input-field"
                        id="clientName"
                        placeholder="Nome completo"
                        {...register("clientName")}
                      />
                      {errors.clientName && (
                        <span className="form-error">{errors.clientName.message}</span>
                      )}
                    </div>

                    <div className="form-group">
                      <label className="form-label" htmlFor="clientEmail">Seu e-mail</label>
                      <input
                        className="input-field"
                        id="clientEmail"
                        type="email"
                        placeholder="voce@exemplo.com"
                        {...register("clientEmail")}
                      />
                      {errors.clientEmail && (
                        <span className="form-error">{errors.clientEmail.message}</span>
                      )}
                    </div>

                    <button
                      className="btn-primary w-full"
                      type="submit"
                      disabled={createBookingMutation.isPending}
                    >
                      {createBookingMutation.isPending
                        ? "Confirmando..."
                        : `Confirmar Agendamento para ${formatFullDateTime(selectedSlot)}`}
                    </button>
                  </form>
                </>
              )}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}

export default PublicBookingPage;
