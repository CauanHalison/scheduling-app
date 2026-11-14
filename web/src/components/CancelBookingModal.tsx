import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { cancelBookingSchema, type CancelBookingFormData } from "../schemas/bookingSchema";
import "./styles/EventTypeModal.css";

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
          <h2>Cancelar Agendamento</h2>
          <button
            type="button"
            className="btn-close"
            onClick={onClose}
          >
            ✕
          </button>
        </header>

        <form onSubmit={handleSubmit(onSubmit)} className="modal-form">
          <p className="modal-description">
            Tem certeza que deseja cancelar <strong>{eventTitle}</strong> com{" "}
            <strong>{participantName}</strong>?
          </p>

          <div className="form-group">
            <label htmlFor="reason">Motivo do cancelamento (Opcional)</label>
            <textarea
              id="reason"
              rows={3}
              placeholder="Informe o motivo do cancelamento..."
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
              Voltar
            </button>
            <button type="submit" className="btn-danger" disabled={isPending}>
              {isPending ? "Cancelando..." : "Confirmar Cancelamento"}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}

export default CancelBookingModal;