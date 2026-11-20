import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useCreateScheduleOverride } from "../hooks";
import {
  scheduleOverrideSchema,
  type ScheduleOverrideFormData,
} from "../schemas/scheduleOverrideSchema";
import { Save } from "lucide-react";

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
          <h2 className="modal-title">Novo Bloqueio de Agenda</h2>
          <button className="modal-close" type="button" onClick={onClose}>
            ✕
          </button>
        </header>

        <form className="modal-form" onSubmit={handleSubmit(onSubmit)}>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 md:gap-5">
            <div className="form-group">
              <label className="form-label" htmlFor="startTime">Início do bloqueio</label>
              <input
                className="input-field"
                id="startTime"
                type="datetime-local"
                {...register("startTime")}
              />
              {errors.startTime && (
                <span className="form-error">{errors.startTime.message}</span>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="endTime">Fim do bloqueio</label>
              <input
                className="input-field"
                id="endTime"
                type="datetime-local"
                {...register("endTime")}
              />
              {errors.endTime && (
                <span className="form-error">{errors.endTime.message}</span>
              )}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reason">Motivo / Justificativa (opcional)</label>
            <textarea
              className="input-field"
              id="reason"
              rows={3}
              placeholder="Ex: Consulta médica, feriado, viagem..."
              {...register("reason")}
            />
            {errors.reason && (
              <span className="form-error">{errors.reason.message}</span>
            )}
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
              {isPending ? "Salvando..." : "Cadastrar Bloqueio"}
              <Save className="w-4 h-4"/>
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}

export default ScheduleOverrideModal;
