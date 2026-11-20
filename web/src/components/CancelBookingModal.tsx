import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { cancelBookingSchema, type CancelBookingFormData } from "../schemas/bookingSchema";

interface CancelBookingModalProps {
  eventTitle: string;
  participantName: string;
  isPending: boolean;
  onConfirm: (reason?: string) => void;
  onClose: () => void;
}

export function CancelBookingModal({
  eventTitle,
  participantName,
  isPending,
  onConfirm,
  onClose,
}: CancelBookingModalProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CancelBookingFormData>({
    resolver: zodResolver(cancelBookingSchema),
    defaultValues: {
      reason: "",
    },
  });

  const onSubmit = (data: CancelBookingFormData) => {
    onConfirm(data.reason?.trim() || undefined);
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container" onClick={(e) => e.stopPropagation()}>
        <header className="modal-header">
          <h2 className="modal-title">Cancelar Agendamento</h2>
          <button
            className="modal-close"
            type="button"
            onClick={onClose}
          >
            ✕
          </button>
        </header>

        <form className="modal-form" onSubmit={handleSubmit(onSubmit)}>
          <p className="text-sm text-slate-600 leading-relaxed">
            Tem certeza que deseja cancelar <strong className="text-slate-900">{eventTitle}</strong> com{" "}
            <strong className="text-slate-900">{participantName}</strong>?
          </p>

          <div className="form-group">
            <label className="form-label" htmlFor="reason">Motivo do cancelamento (Opcional)</label>
            <textarea
              className="input-field"
              id="reason"
              rows={3}
              placeholder="Informe o motivo do cancelamento..."
              {...register("reason")}
            />
            {errors.reason && (
              <span className="form-error">{errors.reason.message}</span>
            )}
          </div>

          <footer className="modal-footer">
            <button
              className="btn-secondary"
              type="button"
              onClick={onClose}
              disabled={isPending}
            >
              Voltar
            </button>
            <button className="btn-danger" type="submit" disabled={isPending}>
              {isPending ? "Cancelando..." : "Confirmar Cancelamento"}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}

export default CancelBookingModal;
