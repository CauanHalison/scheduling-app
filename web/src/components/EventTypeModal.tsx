import { useEffect } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useAuth, useCreateEventType, useUpdateEventType } from "../hooks";
import { eventTypeSchema, type EventTypeFormData } from "../schemas/eventTypeSchema";
import type { EventTypeResponse } from "../types/eventType";
import { Save } from "lucide-react";

interface EventTypeModalProps {
  initialData: EventTypeResponse | null;
  onClose: () => void;
}

export function EventTypeModal({ initialData, onClose }: EventTypeModalProps) {
  const isEditing = !!initialData;
  const { provider } = useAuth();

  const editEventType = useUpdateEventType();
  const createEventType = useCreateEventType();

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<EventTypeFormData>({
    resolver: zodResolver(eventTypeSchema) as any,
    defaultValues: initialData
      ? {
          title: initialData.title,
          slug: initialData.slug,
          eventLink: initialData.eventLink || "",
          description: initialData.description || "",
          durationMinutes: initialData.durationMinutes,
          bufferAfterMinutes: initialData.bufferAfterMinutes,
        }
      : {
          title: "",
          slug: "",
          eventLink: "",
          description: "",
          durationMinutes: 30,
          bufferAfterMinutes: 0,
        },
  });

  useEffect(() => {
    if (initialData) {
      reset({
        title: initialData.title,
        slug: initialData.slug,
        eventLink: initialData.eventLink || "",
        description: initialData.description || "",
        durationMinutes: initialData.durationMinutes,
        bufferAfterMinutes: initialData.bufferAfterMinutes,
      });
    }
  }, [initialData, reset]);

  const onSubmit = (data: EventTypeFormData) => {
    if (isEditing) {
      editEventType.mutate(
        { eventTypeId: initialData.id, payload: data },
        {
          onSuccess: () => onClose(),
          onError: (error) => {
            if (error?.statusCode === 409) {
              setError("slug", {
                message: "Este slug já está em uso por outro tipo de evento seu.",
              });
            }
          },
        }
      );
    } else {
      createEventType.mutate(data, {
        onSuccess: () => onClose(),
        onError: (error) => {
          if (error?.statusCode === 409) {
            setError("slug", {
              message: "Este slug já está em uso por outro tipo de evento seu.",
            });
          }
        },
      });
    }
  };

  const isPending = createEventType.isPending || editEventType.isPending;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-container"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="modal-header">
          <h2 className="modal-title">{isEditing ? "Editar Tipo de Evento" : "Novo Tipo de Evento"}</h2>
          <button className="modal-close" type="button" onClick={onClose}>
            ✕
          </button>
        </header>

        <form className="modal-form" onSubmit={handleSubmit(onSubmit)}>
          <div className="form-group">
            <label className="form-label" htmlFor="title">Título</label>
            <input
              className="input-field"
              id="title"
              {...register("title")}
              placeholder="Ex: Consulta Inicial"
            />
            {errors.title && <span className="form-error">{errors.title.message}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="slug">Slug (URL amigável)</label>
            <div className="flex items-center overflow-hidden rounded-xl border border-slate-300 bg-white transition duration-300 focus-within:border-blue-600 focus-within:ring-3 focus-within:ring-blue-500/15">
              <span className="shrink-0 select-none border-r border-slate-200 bg-slate-50 px-3 py-2.5 text-xs font-mono text-slate-500">
                {provider?.slug}/events/
              </span>
              <input
                className="w-full px-3 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none disabled:bg-slate-100"
                id="slug"
                {...register("slug")}
                placeholder="Ex: consulta-inicial"
              />
            </div>
            {errors.slug && <span className="form-error">{errors.slug.message}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="eventLink">Link do evento</label>
            <input
              className="input-field"
              id="eventLink"
              {...register("eventLink")}
              placeholder="Ex: https://meet.google.com/abc-defg-hij"
            />
            {errors.eventLink && <span className="form-error">{errors.eventLink.message}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="description">Descrição</label>
            <textarea
              className="input-field"
              id="description"
              rows={3}
              {...register("description")}
              placeholder="Breve descrição sobre como será o atendimento..."
            />
            {errors.description && (
              <span className="form-error">{errors.description.message}</span>
            )}
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 md:gap-5">
            <div className="form-group">
              <label className="form-label" htmlFor="durationMinutes">Duração (minutos)</label>
              <input
                className="input-field"
                id="durationMinutes"
                type="number"
                step="5"
                {...register("durationMinutes", { valueAsNumber: true })}
              />
              {errors.durationMinutes && (
                <span className="form-error">{errors.durationMinutes.message}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="bufferAfterMinutes">Intervalo após (minutos)</label>
              <input
                className="input-field"
                id="bufferAfterMinutes"
                type="number"
                step="5"
                {...register("bufferAfterMinutes", { valueAsNumber: true })}
              />
              {errors.bufferAfterMinutes && (
                <span className="form-error">{errors.bufferAfterMinutes.message}</span>
              )}
            </div>
          </div>

          <footer className="modal-footer">
            <button
              className="btn-danger"
              type="button"
              onClick={onClose}
              disabled={isPending}
            >
              Cancelar
            </button>
            <button className="btn-primary" type="submit" disabled={isPending}>
              {isPending ? "Salvando..." : isEditing ? "Salvar Alterações" : "Criar Evento"}
              <Save className="w-4 h-4"/>
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}

export default EventTypeModal;
