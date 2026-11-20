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
import { Calendar, Plus, Save, Trash, X } from "lucide-react";

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
    return <div className="loading-state">Carregando disponibilidade...</div>;
  }

  if (isScheduleError) {
    return (
      <div className="alert-error">
        Erro ao carregar a grade de disponibilidade.
      </div>
    );
  }

  return (
    <div>
      <header>
        <div>
          <h1 className="page-title">Disponibilidade</h1>
          <p className="page-subtitle">
            Configure seus horários de atendimento semanal
            {scheduleData?.timeZone ? ` (${scheduleData.timeZone})` : ""}
          </p>
        </div>
        <button
          className="btn-primary mt-3"
          type="button"
          onClick={handleSaveSchedule}
          disabled={replaceMutation.isPending}
        >
          <Save className="h-4 w-4"/>
          {replaceMutation.isPending ? "Salvando..." : "Salvar Alterações"}
        </button>
      </header>

      <section className="mt-3">
        <div className="grid items-start grid-cols-1 gap-3 md:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-7">
          {days.map((day) => {
            const isEnabled = day.intervals.length > 0;
            const errorMsg = dayErrors[day.dayOfWeek];

            return (
              <div 
                className="card pt-0 px-0 overflow-hidden"
                key={day.dayOfWeek}
              >
                <div className="w-full flex items-center">
                  <span className={`w-full text-md font-bold text-center py-2 ${isEnabled? 'bg-blue-600 text-white' : ' bg-slate-200 text-slate-600'}`}>
                    {DAY_OF_WEEK_LABELS[day.dayOfWeek]}
                  </span>
                </div>

                <div className="flex flex-row flex-wrap justify-center px-3 items-center">
                  {!isEnabled ? (
                    <div className="rounded-xl border border-dashed border-slate-300 bg-slate-100/80 px-3 py-3 text-center">
                      <span className="text-sm font-semibold text-slate-600">Sem expediente</span>
                      <p className="mt-0.5 text-xs text-slate-500">
                        Adicione um turno para ativar este dia
                      </p>
                    </div>
                  ) : (
                    <div className="flex w-full flex-col gap-2">
                      {day.intervals.map((interval, idx) => (
                        <div
                          key={idx}
                          className="flex flex-col gap-1.5 rounded-xl border border-slate-200 bg-slate-50/70 p-2"
                        >
                          <div className="flex items-center justify-between gap-3">
                            <input
                              type="time"
                              className="time-input"
                              value={interval.startTime}
                              onChange={(e) =>
                                handleIntervalChange(
                                  day.dayOfWeek,
                                  idx,
                                  "startTime",
                                  e.target.value
                                )
                              }
                            />
                            <span className="shrink-0 text-[11px] font-medium text-slate-400">até</span>
                            <input
                              type="time"
                              className="time-input"
                              value={interval.endTime}
                              onChange={(e) =>
                                handleIntervalChange(
                                  day.dayOfWeek,
                                  idx,
                                  "endTime",
                                  e.target.value
                                )
                              }
                            />
                            <button
                              type="button"
                              className="shrink-0 cursor-pointer rounded-md p-1 text-slate-400 transition hover:bg-red-50 hover:text-red-600"
                              onClick={() =>
                                handleRemoveInterval(day.dayOfWeek, idx)
                              }
                            >
                              <X className="h-3.5 w-3.5"/>
                            </button>
                          </div>
                        </div>
                      ))}

                      {errorMsg && (
                        <span className="form-error text-center">{errorMsg}</span>
                      )}
                    </div>
                  )}
                </div>

                <div className="w-full px-3">
                  {day.intervals.length < 10 && (
                    <button
                      className={`py-1 w-full ${isEnabled ? 'btn-primary' : 'btn-secondary'}`}
                      type="button"
                      onClick={() => handleAddInterval(day.dayOfWeek)}
                      title="Adicionar turno"
                    >
                      <Plus className="w-4 h-4"/> Turno
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </section>

      <section className="mt-10 border-t border-slate-200 pt-8">
        <div>
          <div>
            <h2 className="text-xl font-bold tracking-tight text-slate-900">Bloqueios e Exceções</h2>
            <p className="page-subtitle">
              Adicione períodos em que você não estará disponível (férias,
              consultas, feriados)
            </p>
          </div>
          <button
            className="btn-primary mt-3 mb-3"
            type="button"
            onClick={handleOpenOverrideModal}
          >
            <Plus className="h-4 w-4"/> Novo Bloqueio
          </button>
        </div>

        {isLoadingOverrides ? (
          <div className="loading-state">Carregando bloqueios...</div>
        ) : isOverridesError ? (
          <div className="alert-error">Erro ao carregar bloqueios futuros.</div>
        ) : !overrides || overrides.length === 0 ? (
          <div className="empty-state">
            <p>Nenhum bloqueio futuro cadastrado.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {overrides.map((override) => (
              <article className="card" key={override.id}>
                <div className="flex w-full flex-col gap-2">
                  <div className="flex w-full items-start justify-between gap-2">
                    <div className="text-base font-semibold leading-snug text-slate-900">
                      {override.reason ? (
                        <p>{override.reason}</p>
                      ) : (
                        <p className="text-slate-500">Exceção sem justificativa</p>
                      )}
                    </div>
                    <span className="shrink-0 rounded-lg border-2 border-red-700 bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-700">
                      Bloqueado
                    </span>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 text-sm text-slate-700">
                    <Calendar className="h-4 w-4 shrink-0 text-blue-600" />
                    <strong>{formatDateTime(override.startTime)}</strong>
                    <span className="text-slate-400 font-bold">-</span>
                    <strong>{formatDateTime(override.endTime)}</strong>
                  </div>
                </div>

                <div className="w-full border-t border-slate-200 pt-2">
                  <button
                    className="btn-danger w-full py-2"
                    type="button"
                    onClick={() => handleDeleteOverride(override.id)}
                    disabled={deleteOverrideMutation.isPending}
                  >
                    Remover
                    <Trash className="h-4 w-4" />
                  </button>
                </div>
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
