import { Clock } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { useProviderPublicEvents } from "../hooks";
import { PublicHeader } from "../components/PublicHeader";

export function PublicProviderCatalog() {
  const navigate = useNavigate();
  const { userSlug = "" } = useParams();

  const { data, isLoading, isError } = useProviderPublicEvents(userSlug);

  const goToBooking = (eventSlug: string) => {
    navigate(`/users/${userSlug}/events/${eventSlug}`);
  };

  if (isLoading) {
    return (
      <div>
        <div className="loading-state">Carregando eventos do prestador...</div>
      </div>
    );
  }

  if (isError) {
    return (
      <div>
        <div className="alert-error">Erro ao carregar eventos do prestador.</div>
      </div>
    );
  }

  return (
    <div>
      <section>
        <PublicHeader 
          userSlug={userSlug}
          headerTitle="Agende um horário"
          headerDescription="Selecione um tipo de atendimento abaixo para ver os horários disponíveis."
          servicesCount={data?.length || undefined}
        />

        {!data || data.length === 0 ? (
          <div className="empty-state">
            <p>Este prestador não possui eventos ativos para agendamento no momento.</p>
          </div>
        ) : (
          <div className="card-grid">
            {data.map((eventType) => (
              <article
                className="card cursor-pointer"
                key={eventType.id}
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
                <div className="flex flex-col gap-3">
                  <div className="flex flex-row flex-nowrap items-start justify-between gap-2">
                    <h2 className="text-base font-semibold text-slate-900 leading-snug">
                      {eventType.title}
                    </h2>
                    <span className="shrink-0 px-2 py-0.5 text-xs border border-blue-600 bg-blue-100 rounded-md text-blue-600 font-semibold">
                      /{eventType.slug}
                    </span>
                  </div>

                  <p className="text-sm text-slate-600 line-clamp-2">
                    {eventType.description || "Sem descrição"}
                  </p>

                  <div className="flex flex-col text-sm text-slate-600">
                    <span className="flex flex-row flex-nowrap items-center gap-2">
                      <Clock className="w-4 h-4 text-blue-600" /> Duração: {eventType.durationMinutes} min
                    </span>
                  </div>
                </div>

                <div className="w-full pt-3 border-t border-slate-200">
                  <button
                    className="btn-primary w-full py-2"
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      goToBooking(eventType.slug);
                    }}
                  >
                    Agendar horário
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default PublicProviderCatalog;
