import { useNavigate, useParams } from "react-router-dom";
import { useProviderPublicEvents } from "../hooks";
import "./styles/PublicBookingPages.css";

export function PublicProviderCatalog() {
  const navigate = useNavigate();
  const { userSlug = "" } = useParams();

  const { data, isLoading, isError } = useProviderPublicEvents(userSlug);

  const goToBooking = (eventSlug: string) => {
    navigate(`/users/${userSlug}/events/${eventSlug}`);
  };

  if (isLoading) {
    return (
      <main className="public-page-container">
        <section className="public-card">
          <div className="loading-state">Carregando eventos do prestador...</div>
        </section>
      </main>
    );
  }

  if (isError) {
    return (
      <main className="public-page-container">
        <section className="public-card">
          <div className="error-state">Erro ao carregar eventos do prestador.</div>
        </section>
      </main>
    );
  }

  return (
    <main className="public-page-container">
      <section className="public-card">
        <header className="public-header">
          <span className="public-badge">@{userSlug}</span>
          <h1>Agende um horário</h1>
          <p>Selecione um tipo de atendimento abaixo para ver os horários disponíveis.</p>
        </header>

        {!data || data.length === 0 ? (
          <div className="public-empty-box">
            <p>Este prestador não possui eventos ativos para agendamento no momento.</p>
          </div>
        ) : (
          <div className="catalog-grid">
            {data.map((eventType) => (
              <article
                key={eventType.id}
                className="catalog-card"
                role="button"
                tabIndex={0}
                onClick={() => goToBooking(eventType.slug)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    goToBooking(eventType.slug);
                  }
                }}
              >
                <div className="catalog-card-header">
                  <h2>{eventType.title}</h2>
                  <span className="slug-name">/{eventType.slug}</span>
                </div>

                <p className="catalog-card-description">
                  {eventType.description || "Sem descrição"}
                </p>

                <div className="catalog-card-footer">
                  <span className="catalog-duration">
                    ⏱️ Duração: {eventType.durationMinutes} min
                  </span>
                  <span className="catalog-cta">Agendar horário →</span>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default PublicProviderCatalog;