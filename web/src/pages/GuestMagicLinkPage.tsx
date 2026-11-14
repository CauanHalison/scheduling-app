import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useSearchParams } from "react-router-dom";
import { useRequestMagicLink, useVerifyMagicLink } from "../hooks";
import { magicLinkSchema, type MagicLinkSchema } from "../schemas/bookingSchema";
import "./styles/ProviderBookingsPage.css";
import "./styles/PublicBookingPages.css";

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
      return (
        <main className="public-page-container">
          <section className="public-card narrow">
            <div className="loading-state">Validando seu link de acesso...</div>
          </section>
        </main>
      );
    }

    if (isInvalidToken) {
      return (
        <main className="public-page-container">
          <section className="public-card narrow">
            <header className="public-header">
              <span className="public-badge">Acesso Expirado</span>
              <h1>Link expirado ou inválido</h1>
              <p>
                Este link de acesso expirou (validade de 15 minutos) ou já não é
                válido.
              </p>
            </header>

            <button
              type="button"
              className="login-submit"
              onClick={() => setSearchParams({})}
            >
              Solicitar um novo link
            </button>
          </section>
        </main>
      );
    }

    return (
      <main className="public-page-container">
        <section className="public-card">
          <header className="public-header">
            <span className="public-badge">Área do Cliente</span>
            <h1>Meus Agendamentos</h1>
            <p>Aqui estão todos os agendamentos vinculados ao seu e-mail:</p>
          </header>

          <hr className="public-divider" />

          {!bookings || bookings.length === 0 ? (
            <div className="public-empty-box">
              <p>Nenhum agendamento encontrado para este e-mail.</p>
            </div>
          ) : (
            <div className="bookings-list">
              {bookings.map((booking) => (
                <article key={booking.id} className="booking-card">
                  <div className="booking-card-header">
                    <div>
                      <h3>{booking.eventTitle}</h3>
                      <span className="booking-client-info">
                        Prestador: {booking.providerName}
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
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
    );
  }

  return (
    <main className="public-page-container">
      <section className="public-card narrow">
        <header className="public-header">
          <span className="public-badge">Área do Cliente</span>
          <h1>Consultar Agendamentos</h1>
          <p>
            Informe o e-mail utilizado na reserva para receber um link temporário
            de acesso.
          </p>
        </header>

        <hr className="public-divider" />

        {emailSent ? (
          <div className="magic-sent-box">
            <h2>📩 Verifique seu e-mail!</h2>
            <p>
              Enviamos um link de acesso válido por <strong>15 minutos</strong>{" "}
              para o seu e-mail.
            </p>
          </div>
        ) : (
          <form className="login-form" onSubmit={handleSubmit(onSubmit)}>
            <div className="form-group">
              <label htmlFor="email">Seu e-mail</label>
              <input
                id="email"
                type="email"
                className="form-input"
                placeholder="cliente@email.com"
                {...register("email")}
              />
              {errors.email && (
                <span className="field-error">{errors.email.message}</span>
              )}
            </div>

            <button
              type="submit"
              className="login-submit"
              disabled={requestMagicLinkMutation.isPending}
            >
              {requestMagicLinkMutation.isPending
                ? "Enviando..."
                : "Receber link de acesso"}
            </button>
          </form>
        )}
      </section>
    </main>
  );
}

export default GuestMagicLinkPage;