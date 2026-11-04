import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import { ScheduleOverrideModal } from "../components";
import {
  useAvailabilitySchedule,
  useDeleteScheduleOverride,
  useReplaceAvailabilitySchedule,
  useScheduleOverrides,
} from "../hooks";
import { weeklyScheduleSchema } from "../schemas/weeklyScheduleSchema";
import {
  DAY_OF_WEEK_LABELS,
  type DayOfWeek,
  type DayScheduleRequest,
  type TimeIntervalRequest,
} from "../types/schedule";
import "./styles/SchedulePage.css";

const ALL_DAYS: DayOfWeek[] = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

function buildNormalizedDays(existing?: DayScheduleRequest[]): DayScheduleRequest[] {
  return ALL_DAYS.map((dayOfWeek) => {
    const found = existing?.find((d) => d.dayOfWeek === dayOfWeek);
    return {
      dayOfWeek,
      intervals: found ? found.intervals.map((i) => ({ ...i })) : [],
    };
  });
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function SchedulePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const isOverrideModalOpen = searchParams.get("modal") === "override";

  const {
    data: scheduleData,
    isLoading: isLoadingSchedule,
    isError: isScheduleError,
  } = useAvailabilitySchedule();

  const {
    data: overrides,
    isLoading: isLoadingOverrides,
    isError: isOverridesError,
  } = useScheduleOverrides();

  const replaceMutation = useReplaceAvailabilitySchedule();
  const deleteOverrideMutation = useDeleteScheduleOverride();

  const [days, setDays] = useState<DayScheduleRequest[]>(() =>
    buildNormalizedDays()
  );
  const [dayErrors, setDayErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (scheduleData?.days) {
      setDays(buildNormalizedDays(scheduleData.days));
      setDayErrors({});
    }
  }, [scheduleData]);

  const handleToggleDay = (dayOfWeek: DayOfWeek, enabled: boolean) => {
    setDays((prev) =>
      prev.map((day) =>
        day.dayOfWeek === dayOfWeek
          ? {
              ...day,
              intervals: enabled
                ? [{ startTime: "09:00", endTime: "18:00" }]
                : [],
            }
          : day
      )
    );
    setDayErrors((prev) => {
      const next = { ...prev };
      delete next[dayOfWeek];
      return next;
    });
  };

  const handleAddInterval = (dayOfWeek: DayOfWeek) => {
    setDays((prev) =>
      prev.map((day) => {
        if (day.dayOfWeek !== dayOfWeek) return day;
        if (day.intervals.length >= 10) return day;

        const lastInterval = day.intervals[day.intervals.length - 1];
        const nextStart = lastInterval ? lastInterval.endTime : "09:00";
        const [hourStr, minStr] = nextStart.split(":");
        const nextHour = Math.min(Number(hourStr) + 1, 23)
          .toString()
          .padStart(2, "0");
        const nextEnd = `${nextHour}:${minStr || "00"}`;

        return {
          ...day,
          intervals: [
            ...day.intervals,
            {
              startTime: nextStart,
              endTime: nextEnd > nextStart ? nextEnd : "23:59",
            },
          ],
        };
      })
    );
  };

  const handleRemoveInterval = (dayOfWeek: DayOfWeek, index: number) => {
    setDays((prev) =>
      prev.map((day) =>
        day.dayOfWeek === dayOfWeek
          ? {
              ...day,
              intervals: day.intervals.filter((_, idx) => idx !== index),
            }
          : day
      )
    );
    setDayErrors((prev) => {
      const next = { ...prev };
      delete next[dayOfWeek];
      return next;
    });
  };

  const handleIntervalChange = (
    dayOfWeek: DayOfWeek,
    index: number,
    field: keyof TimeIntervalRequest,
    value: string
  ) => {
    setDays((prev) =>
      prev.map((day) => {
        if (day.dayOfWeek !== dayOfWeek) return day;
        const updated = day.intervals.map((interval, idx) =>
          idx === index ? { ...interval, [field]: value } : interval
        );
        return { ...day, intervals: updated };
      })
    );
    setDayErrors((prev) => {
      const next = { ...prev };
      delete next[dayOfWeek];
      return next;
    });
  };

  const handleSaveSchedule = () => {
    const result = weeklyScheduleSchema.safeParse({ days });

    if (!result.success) {
      const newErrors: Record<string, string> = {};
      for (const issue of result.error.issues) {
        const dayIndex = issue.path[1];
        if (typeof dayIndex === "number" && days[dayIndex]) {
          const dayKey = days[dayIndex].dayOfWeek;
          if (!newErrors[dayKey]) {
            newErrors[dayKey] = issue.message;
          }
        }
      }
      setDayErrors(newErrors);
      toast.error("Corrija os horários inválidos antes de salvar.");
      return;
    }

    setDayErrors({});
    replaceMutation.mutate(result.data);
  };

  const handleOpenOverrideModal = () => {
    setSearchParams({ modal: "override" });
  };

  const handleCloseOverrideModal = () => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete("modal");
    setSearchParams(nextParams);
  };

  const handleDeleteOverride = (overrideId: string) => {
    if (confirm("Deseja realmente remover este bloqueio de agenda?")) {
      deleteOverrideMutation.mutate(overrideId);
    }
  };

  if (isLoadingSchedule) {
    return (
      <div className="schedule-page">
        <div className="loading-state">Carregando disponibilidade...</div>
      </div>
    );
  }

  if (isScheduleError) {
    return (
      <div className="schedule-page">
        <div className="error-state">
          Erro ao carregar a grade de disponibilidade.
        </div>
      </div>
    );
  }

  return (
    <div className="schedule-page">
      <header className="page-header">
        <div>
          <h1>Disponibilidade</h1>
          <p>
            Configure seus horários de atendimento semanal
            {scheduleData?.timeZone ? ` (${scheduleData.timeZone})` : ""}
          </p>
        </div>
        <button
          type="button"
          onClick={handleSaveSchedule}
          disabled={replaceMutation.isPending}
        >
          {replaceMutation.isPending ? "Salvando..." : "Salvar Alterações"}
        </button>
      </header>

      <section className="schedule-section">
        <div className="weekly-card">
          {days.map((day) => {
            const isEnabled = day.intervals.length > 0;
            const errorMsg = dayErrors[day.dayOfWeek];

            return (
              <div
                key={day.dayOfWeek}
                className={`day-row ${!isEnabled ? "day-disabled" : ""}`}
              >
                <div className="day-label-col">
                  <label className="switch">
                    <input
                      type="checkbox"
                      checked={isEnabled}
                      onChange={(e) =>
                        handleToggleDay(day.dayOfWeek, e.target.checked)
                      }
                    />
                    <span className="slider-round"></span>
                  </label>
                  <span className="day-name">
                    {DAY_OF_WEEK_LABELS[day.dayOfWeek]}
                  </span>
                </div>

                <div className="day-intervals-col">
                  {!isEnabled ? (
                    <span className="unavailable-label">Sem expediente</span>
                  ) : (
                    <div className="intervals-list">
                      {day.intervals.map((interval, idx) => (
                        <div key={idx} className="interval-row">
                          <input
                            type="time"
                            value={interval.startTime}
                            onChange={(e) =>
                              handleIntervalChange(
                                day.dayOfWeek,
                                idx,
                                "startTime",
                                e.target.value
                              )
                            }
                            className="time-input"
                          />
                          <span className="time-separator">até</span>
                          <input
                            type="time"
                            value={interval.endTime}
                            onChange={(e) =>
                              handleIntervalChange(
                                day.dayOfWeek,
                                idx,
                                "endTime",
                                e.target.value
                              )
                            }
                            className="time-input"
                          />

                          <button
                            type="button"
                            className="btn-icon-remove"
                            onClick={() =>
                              handleRemoveInterval(day.dayOfWeek, idx)
                            }
                            title="Remover turno"
                          >
                            ✕
                          </button>
                        </div>
                      ))}

                      {errorMsg && (
                        <span className="day-error-msg">{errorMsg}</span>
                      )}
                    </div>
                  )}
                </div>

                <div className="day-actions-col">
                  {isEnabled && day.intervals.length < 10 && (
                    <button
                      type="button"
                      className="btn-add-interval"
                      onClick={() => handleAddInterval(day.dayOfWeek)}
                      title="Adicionar turno"
                    >
                      + Turno
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </section>

      <section className="overrides-section">
        <div className="overrides-header">
          <div>
            <h2>Bloqueios e Exceções</h2>
            <p>
              Adicione períodos em que você não estará disponível (férias,
              consultas, feriados)
            </p>
          </div>
          <button
            type="button"
            className="btn-secondary"
            onClick={handleOpenOverrideModal}
          >
            + Novo Bloqueio
          </button>
        </div>

        {isLoadingOverrides ? (
          <div className="loading-state">Carregando bloqueios...</div>
        ) : isOverridesError ? (
          <div className="error-state">Erro ao carregar bloqueios futuros.</div>
        ) : !overrides || overrides.length === 0 ? (
          <div className="overrides-empty">
            <p>Nenhum bloqueio futuro cadastrado.</p>
          </div>
        ) : (
          <div className="overrides-list">
            {overrides.map((override) => (
              <article key={override.id} className="override-card">
                <div className="override-info">
                  <div className="override-dates">
                    <span className="override-badge">Bloqueado</span>
                    <strong>{formatDateTime(override.startTime)}</strong>
                    <span>até</span>
                    <strong>{formatDateTime(override.endTime)}</strong>
                  </div>
                  {override.reason && (
                    <p className="override-reason">{override.reason}</p>
                  )}
                </div>

                <button
                  type="button"
                  className="btn-danger"
                  onClick={() => handleDeleteOverride(override.id)}
                  disabled={deleteOverrideMutation.isPending}
                >
                  Remover
                </button>
              </article>
            ))}
          </div>
        )}
      </section>

      {isOverrideModalOpen && (
        <ScheduleOverrideModal onClose={handleCloseOverrideModal} />
      )}
    </div>
  );
}

export default SchedulePage;

