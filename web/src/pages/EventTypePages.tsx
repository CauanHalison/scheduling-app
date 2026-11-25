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
import { Clock, Copy, Edit, Link2, Plus, Trash } from "lucide-react";

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
    return <div className="alert-error">Erro ao carregar tipos de eventos.</div>;
  }

  const isModalOpen = modalAction === "create" || (modalAction === "edit" && !!editingId);

  return (
    <div>
      <header>
        <div>
          <h1 className="page-title">Tipos de eventos</h1>
          <p className="page-subtitle">Crie e gerencie os serviços disponíveis para agendamento</p>
        </div>
        <button
          className="btn-primary mt-3"
          onClick={handleOpenCreate}
        >
          <Plus className="w-4 h-4 font-semibold"/> Novo tipo de evento
        </button>
      </header>

      {eventTypes?.length === 0 ? (
        <div className="empty-state">
          <p>Nenhum tipo de evento cadastrado ainda.</p>
          <button className="btn-primary" onClick={handleOpenCreate}>
            Criar o primeiro evento
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-5 py-5 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {eventTypes?.map((event) => (
            <article className="card" key={event.id}>
              <div className="flex flex-col gap-3">
                <div>
                  <div className="flex flex-row flex-nowrap items-start justify-between gap-2">
                    <h1 className="text-base font-semibold text-slate-900 leading-snug">
                      {event.title}
                    </h1>
                    <span
                      className={`shrink-0 rounded-lg border-2 px-3 py-0.5 text-xs font-semibold flex items-center ${
                        event.isActive
                          ? "border-blue-600 bg-blue-100 text-blue-600"
                          : "border-slate-500 bg-slate-100 text-slate-600"
                      }`}
                    >
                      {event.isActive ? "Ativo" : "Inativo"}
                    </span>
                  </div>

                  <span className="mt-1 flex w-max items-center rounded-md border border-blue-600 bg-blue-100 px-2 py-0.5 text-xs text-blue-600">
                    /{event.slug}
                  </span>
                </div>

                <p className="text-sm text-slate-600 line-clamp-2">
                  {event.description || "Sem descrição"}
                </p>

                {event.eventLink && (
                  <span className="flex flex-row flex-nowrap items-center gap-2 text-sm text-slate-500 min-w-0">
                    <Link2 className="h-4 w-4 shrink-0 text-blue-600" />
                    <a
                      href={event.eventLink}
                      className="truncate text-blue-600 hover:underline"
                    >
                      {event.eventLink}
                    </a>
                  </span>
                )}

                <div className="flex flex-wrap items-center gap-3 text-sm text-slate-600">
                  <span className="inline-flex items-center gap-1">
                    <Clock className="h-4 w-4 text-blue-600" /> {event.durationMinutes} min
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <Plus className="h-4 w-4 text-slate-600" /> {event.bufferAfterMinutes} min intervalo
                  </span>
                </div>
              </div>

              <div className="flex flex-col gap-2">
                <div className="flex flex-row flex-nowrap gap-2 w-full">
                  <button
                    className="btn-primary py-1.5 w-full"
                    type="button"
                    onClick={() => handleCopyPublicLink(event.slug)}
                  >
                    Copiar link
                    <Copy className="w-4 h-4" />
                  </button>
                  <button
                    className="btn-primary py-1.5 w-full"
                    type="button"
                    onClick={() => handleOpenEdit(event)}
                  >
                    Editar
                    <Edit className="w-4 h-4" />
                  </button>
                </div>

                <button
                  type="button"
                  onClick={() => toggleMutation.mutate(event.id)}
                  disabled={toggleMutation.isPending}
                  className={`w-full inline-flex cursor-pointer items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold text-white transition duration-200 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60 ${
                    event.isActive
                      ? "bg-slate-500 hover:bg-slate-600"
                      : "bg-blue-600 hover:bg-blue-700 shadow-sm shadow-blue-600/20"
                  }`}
                >
                  {event.isActive ? "Desativar evento" : "Ativar evento"}
                </button>

                <div className="w-full border-t border-slate-200 pt-2">
                  <button
                    className="btn-danger w-full py-2"
                    type="button"
                    onClick={() => handleDelete(event.id, event.title)}
                    disabled={deleteMutation.isPending}
                  >
                    Excluir
                    <Trash className="w-4 h-4" />
                  </button>
                </div>
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
