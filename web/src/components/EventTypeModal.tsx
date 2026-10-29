import { useEffect } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useAuth, useCreateEventType, useUpdateEventType } from "../hooks";
import { eventTypeSchema, type EventTypeFormData } from "../schemas/eventTypeSchema";
import type { EventTypeResponse } from "../types/eventType";
import "./styles/EventTypeModal.css";

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
    watch,
    reset,
    setError,
    formState: { errors },
  } = useForm<EventTypeFormData>({
    resolver: zodResolver(eventTypeSchema) as any,
    defaultValues: initialData
      ? {
          title: initialData.title,
          slug: initialData.slug,
          description: initialData.description || "",
          durationMinutes: initialData.durationMinutes,
          bufferAfterMinutes: initialData.bufferAfterMinutes,
        }
      : {
          title: "",
          slug: "",
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
        description: initialData.description || "",
        durationMinutes: initialData.durationMinutes,
        bufferAfterMinutes: initialData.bufferAfterMinutes,
      });
    }
  }, [initialData, reset]);

  const currentSlug = watch("slug");

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
      <div className="modal-container" onClick={(e) => e.stopPropagation()}>
        <header className="modal-header">
          <h2>{isEditing ? "Editar Tipo de Evento" : "Novo Tipo de Evento"}</h2>
          <button type="button" className="btn-close" onClick={onClose}>
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit(onSubmit)} className="modal-form">
          <div className="form-group">
            <label htmlFor="title">Título</label>
            <input
              id="title"
              {...register("title")}
              placeholder="Ex: Consulta Inicial"
              className={errors.title ? "input-error" : ""}
            />
            {errors.title && <span className="field-error">{errors.title.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="slug">Slug (URL amigável)</label>
            <input
              id="slug"
              {...register("slug")}
              placeholder="Ex: consulta-inicial"
              className={errors.slug ? "input-error" : ""}
            />
            {errors.slug && <span className="field-error">{errors.slug.message}</span>}

            <div className="slug-preview">
              <span className="slug-preview-label">Link público:</span>
              <code>
                /{provider?.slug || "provider"}/{currentSlug || "slug-do-evento"}
              </code>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="description">Descrição</label>
            <textarea
              id="description"
              rows={3}
              {...register("description")}
              placeholder="Breve descrição sobre como será o atendimento..."
              className={errors.description ? "input-error" : ""}
            />
            {errors.description && (
              <span className="field-error">{errors.description.message}</span>
            )}
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="durationMinutes">Duração (minutos)</label>
              <input
                id="durationMinutes"
                type="number"
                step="5"
                {...register("durationMinutes", { valueAsNumber: true })}
                className={errors.durationMinutes ? "input-error" : ""}
              />
              {errors.durationMinutes && (
                <span className="field-error">{errors.durationMinutes.message}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="bufferAfterMinutes">Intervalo após (minutos)</label>
              <input
                id="bufferAfterMinutes"
                type="number"
                step="5"
                {...register("bufferAfterMinutes", { valueAsNumber: true })}
                className={errors.bufferAfterMinutes ? "input-error" : ""}
              />
              {errors.bufferAfterMinutes && (
                <span className="field-error">{errors.bufferAfterMinutes.message}</span>
              )}
            </div>
          </div>

          <footer className="modal-footer">
            <button
              type="button"
              className="btn-cancel"
              onClick={onClose}
              disabled={isPending}
            >
              Cancelar
            </button>
            <button type="submit" className="btn-submit" disabled={isPending}>
              {isPending ? "Salvando..." : isEditing ? "Salvar Alterações" : "Criar Evento"}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}

export default EventTypeModal;
