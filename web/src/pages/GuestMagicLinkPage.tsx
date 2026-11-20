import { zodResolver } from "@hookform/resolvers/zod";
import { Calendar, Clock, MailCheck, User } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useSearchParams } from "react-router-dom";
import { useRequestMagicLink, useVerifyMagicLink } from "../hooks";
import { magicLinkSchema, type MagicLinkSchema } from "../schemas/bookingSchema";

export function GuestMagicLinkPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const [emailSent, setEmailSent] = useState(false);

  const {
    data: bookings,
    isLoading: isVerifying,
    isError: isInvalidToken,
  } = useVerifyMagicLink(token);

  const requestMagicLinkMutation = useRequestMagicLink();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<MagicLinkSchema>({
    resolver: zodResolver(magicLinkSchema),
    defaultValues: { email: "" },
  });

  const onSubmit = (data: MagicLinkSchema) => {
    requestMagicLinkMutation.mutate(data, {
      onSuccess: () => setEmailSent(true),
    });
  };

  const formatDateTime = (iso: string) =>
    new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(new Date(iso));

  if (token) {
    if (isVerifying) {
      return <div className="loading-state">Validando seu link de acesso...</div>;
    }

    if (isInvalidToken) {
      return (
        <div className="mx-auto w-full max-w-xl py-4">
          <section className="card items-center text-center gap-5 p-6 sm:p-8">
            <header className="flex flex-col items-center gap-2">
              <h1 className="page-title">Link expirado ou inválido</h1>
              <p className="page-subtitle">
                Este link de acesso expirou (validade de 15 minutos) ou já não é
                válido.
              </p>
            </header>

            <button
              className="btn-primary w-full"
              type="button"
              onClick={() => setSearchParams({})}
            >
              Solicitar um novo link
            </button>
          </section>
        </div>
      );
    }

    return (
      <section>
        <header className="mb-2">
          <h1 className="page-title">Meus Agendamentos</h1>
          <p className="page-subtitle">
            Aqui estão todos os agendamentos vinculados ao seu e-mail:
          </p>
        </header>

        {!bookings || bookings.length === 0 ? (
          <div className="empty-state">
            <p>Nenhum agendamento encontrado para este e-mail.</p>
          </div>
        ) : (
          <div className="card-grid">
            {bookings.map((booking) => (
              <article className="card" key={booking.id}>
                <div className="flex flex-col gap-3">
                  <div className="flex flex-row flex-nowrap items-start justify-between gap-2 w-full">
                    <h3 className="font-semibold text-base text-slate-900 leading-snug">
                      {booking.eventTitle}
                    </h3>
                    <span
                      className={`shrink-0 rounded-lg border-2 px-3 py-0.5 text-xs font-semibold ${
                        booking.status === "CONFIRMED"
                          ? "border-green-700 bg-green-100 text-green-700"
                          : "border-red-700 bg-red-100 text-red-700"
                      }`}
                    >
                      {booking.status === "CONFIRMED" ? "Confirmado" : "Cancelado"}
                    </span>
                  </div>

                  {booking.eventDescription && (
                    <p className="text-sm text-slate-600 line-clamp-2">
                      {booking.eventDescription}
                    </p>
                  )}

                  <span className="flex flex-row flex-nowrap items-center gap-2 text-sm text-slate-500 min-w-0">
                    <User className="h-4 w-4 shrink-0 text-slate-400" />
                    <span className="truncate">Prestador: {booking.providerName}</span>
                  </span>

                  <div className="flex flex-wrap items-center gap-4 pt-1 text-sm text-slate-600">
                    <span className="inline-flex items-center gap-2">
                      <Calendar className="h-4 w-4 text-blue-600" />
                      {formatDateTime(booking.startTime)}
                    </span>
                    <span className="inline-flex items-center gap-2">
                      <Clock className="h-4 w-4 text-blue-600" />
                      {booking.durationMinutes} min
                    </span>
                  </div>
                </div>

                {booking.cancellationReason && (
                  <div className="border-t border-slate-200 pt-3">
                    <p className="rounded-xl border border-dashed border-red-300 bg-red-50/70 px-3 py-2 text-xs text-red-800">
                      <strong className="font-semibold">Motivo do cancelamento: </strong>
                      <span>{booking.cancellationReason}</span>
                    </p>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    );
  }

  return (
    <div className="mx-auto w-full max-w-xl py-4">
      <section className="card gap-6 p-6 sm:p-8">
        <header className="flex flex-col items-center gap-2 text-center">
          <h1 className="page-title">Consultar Agendamentos</h1>
          <p className="page-subtitle">
            Informe o e-mail utilizado na reserva para receber um link temporário
            de acesso.
          </p>
        </header>

        <hr className="border-slate-200" />

        {emailSent ? (
          <div className="flex flex-col items-center gap-3 rounded-xl border border-blue-200 bg-blue-50/70 p-6 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-blue-100 text-blue-600">
              <MailCheck className="h-6 w-6" />
            </div>
            <h2 className="text-lg font-bold text-slate-900">Verifique seu e-mail!</h2>
            <p className="text-sm text-slate-600">
              Enviamos um link de acesso válido por{" "}
              <strong className="text-slate-900">15 minutos</strong> para o seu
              e-mail.
            </p>
          </div>
        ) : (
          <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)}>
            <div className="form-group">
              <label className="form-label" htmlFor="email">
                Seu e-mail
              </label>
              <input
                className="input-field"
                id="email"
                type="email"
                placeholder="cliente@email.com"
                {...register("email")}
              />
              {errors.email && (
                <span className="form-error">{errors.email.message}</span>
              )}
            </div>

            <button
              className="btn-primary w-full"
              type="submit"
              disabled={requestMagicLinkMutation.isPending}
            >
              {requestMagicLinkMutation.isPending
                ? "Enviando..."
                : "Receber link de acesso"}
            </button>
          </form>
        )}
      </section>
    </div>
  );
}

export default GuestMagicLinkPage;
