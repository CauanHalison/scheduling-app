import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useCreateScheduleOverride } from "../hooks";
import {
  scheduleOverrideSchema,
  type ScheduleOverrideFormData,
} from "../schemas/scheduleOverrideSchema";
import "./styles/EventTypeModal.css";

interface ScheduleOverrideModalProps {
  onClose: () => void;
}

export function ScheduleOverrideModal({ onClose }: ScheduleOverrideModalProps) {
  const createScheduleOverride = useCreateScheduleOverride();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ScheduleOverrideFormData>({
    resolver: zodResolver(scheduleOverrideSchema) as any,
    defaultValues: {
      startTime: "",
      endTime: "",
      reason: "",
    },
  });

  const onSubmit = (data: ScheduleOverrideFormData) => {
    const payload = {
      startTime: new Date(data.startTime).toISOString(),
      endTime: new Date(data.endTime).toISOString(),
      reason: data.reason?.trim() || undefined,
    };

    createScheduleOverride.mutate(payload, {
      onSuccess: () => onClose(),
    });
  };

  const isPending = createScheduleOverride.isPending;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container" onClick={(e) => e.stopPropagation()}>
        <header className="modal-header">
          <h2>Novo Bloqueio de Agenda</h2>
          <button type="button" className="btn-close" onClick={onClose}>
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit(onSubmit)} className="modal-form">
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="startTime">Início do bloqueio</label>
              <input
                id="startTime"
                type="datetime-local"
                {...register("startTime")}
                className={errors.startTime ? "input-error" : ""}
              />
              {errors.startTime && (
                <span className="field-error">{errors.startTime.message}</span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="endTime">Fim do bloqueio</label>
              <input
                id="endTime"
                type="datetime-local"
                {...register("endTime")}
                className={errors.endTime ? "input-error" : ""}
              />
              {errors.endTime && (
                <span className="field-error">{errors.endTime.message}</span>
              )}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="reason">Motivo / Justificativa (opcional)</label>
            <textarea
              id="reason"
              rows={3}
              placeholder="Ex: Consulta médica, feriado, viagem..."
              {...register("reason")}
              className={errors.reason ? "input-error" : ""}
            />
            {errors.reason && (
              <span className="field-error">{errors.reason.message}</span>
            )}
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
              {isPending ? "Salvando..." : "Cadastrar Bloqueio"}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}

export default ScheduleOverrideModal;