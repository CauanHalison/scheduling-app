import { useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { EventTypeModal } from "../components";
import {
  useAuth,
  useDeleteEventType,
  useEventType,
  useEventTypes,
  useToggleEventType,
} from "../hooks";
import type { EventTypeResponse } from "../types/eventType";
import "./styles/EventTypePages.css";

export function EventTypesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { provider } = useAuth();

  const modalAction = searchParams.get("modal"); 
  const editingId = searchParams.get("id") || "";

  const { data: eventTypes, isLoading, isError } = useEventTypes();
  const toggleMutation = useToggleEventType();
  const deleteMutation = useDeleteEventType();

  const { data: editingEvent } = useEventType(editingId);

  const handleOpenCreate = () => {
    setSearchParams({ modal: "create" });
  };

  const handleOpenEdit = (item: EventTypeResponse) => {
    setSearchParams({ modal: "edit", id: item.id });
  };

  const handleCloseModal = () => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete("modal");
    nextParams.delete("id");
    setSearchParams(nextParams);
  };

  const handleCopyPublicLink = async (eventSlug: string) => {
    if (!provider?.slug) {
      toast.error("Não foi possível identificar o seu link público.");
      return;
    }

    const publicUrl = `${window.location.origin}/users/${provider.slug}/events/${eventSlug}`;

    try {
      await navigator.clipboard.writeText(publicUrl);
      toast.success("Link público copiado!");
    } catch {
      toast.error("Não foi possível copiar o link.");
    }
  };

  const handleDelete = (eventTypeId: string, title: string) => {
    if (confirm(`Deseja realmente excluir o tipo de evento "${title}"?`)) {
      deleteMutation.mutate(eventTypeId);
    }
  };

  if (isLoading) {
    return <div className="loading-state">Carregando tipos de eventos...</div>;
  }

  if (isError) {
    return <div className="error-state">Erro ao carregar tipos de eventos.</div>;
  }

  const isModalOpen = modalAction === "create" || (modalAction === "edit" && !!editingId);

  return (
    <div className="event-types-page">
      <header className="page-header">
        <div>
          <h1>Tipos de eventos</h1>
          <p>Crie e gerencie os serviços disponíveis para agendamento</p>
        </div>
        <button onClick={handleOpenCreate}>
          + Novo tipo de evento
        </button>
      </header>

      {eventTypes?.length === 0 ? (
        <div>
          <p>Nenhum tipo de evento cadastrado ainda.</p>
          <button onClick={handleOpenCreate}>Criar o primeiro evento</button>
        </div>
      ) : (
        <div className="event-types-grid">
          {eventTypes?.map((event) => (
            <article key={event.id} className="event-type-card">
              <div className="card-header">
                <div>
                  <h1>{event.title}</h1>
                  <span className="slug-name">/{event.slug}</span>
                </div>
                <label className="switch">
                  <input
                    type="checkbox"
                    checked={event.isActive}
                    onChange={() => toggleMutation.mutate(event.id)}
                  />
                  <span className="slider-round"></span>
                </label>
              </div>

              <p className="card-description">{event.description || "Sem descrição"}</p>

              <div className="card-meta">
                <span>⏱️ {event.durationMinutes} min</span>
                <span>☕ +{event.bufferAfterMinutes} min intervalo</span>
              </div>

              <div className="card-actions">
                <button
                  type="button"
                  className="btn-link"
                  onClick={() => handleCopyPublicLink(event.slug)}
                >
                  Copiar link
                </button>
                <button
                  type="button"
                  className="btn-link"
                  onClick={() => handleOpenEdit(event)}
                >
                  Editar
                </button>
                <button
                  type="button"
                  className="btn-danger"
                  onClick={() => handleDelete(event.id, event.title)}
                  disabled={deleteMutation.isPending}
                >
                  Excluir
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      {isModalOpen && (
        <EventTypeModal
          initialData={modalAction === "edit" ? editingEvent ?? null : null}
          onClose={handleCloseModal}
        />
      )}
    </div>
  );
}
