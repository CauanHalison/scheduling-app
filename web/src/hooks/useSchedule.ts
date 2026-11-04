import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { queryKeys } from "../lib/keys";
import { scheduleService } from "../service/scheduleService";
import type { ScheduleOverrideRequest, WeeklyScheduleRequest } from "../types/schedule";

export function useAvailabilitySchedule() {
  return useQuery({
    queryKey: queryKeys.schedule.weekly(),
    queryFn: ({ signal }) => scheduleService.findAvailabilitySchedule(signal),
  });
}

export function useReplaceAvailabilitySchedule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: WeeklyScheduleRequest) =>
      scheduleService.replaceAvailabilitySchedule(payload),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.schedule.weekly(), updated);
      toast.success("Cronograma atualizado com sucesso!");
    },
    onError: (error) => {
      toast.error(error?.message || "Erro ao atualizar o cronograma.");
    },
  });
}

export function useScheduleOverrides() {
  return useQuery({
    queryKey: queryKeys.schedule.overrides(),
    queryFn: ({ signal }) => scheduleService.findFutureScheduleOverrides(signal),
  });
}

export function useCreateScheduleOverride() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ScheduleOverrideRequest) =>
      scheduleService.createScheduleOverride(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.schedule.overrides(),
      });
      toast.success("Exceção adicionada ao cronograma!");
    },
    onError: (error) => {
      toast.error(error?.message || "Erro ao cadastrar nova exceção no cronograma.");
    },
  });
}

export function useDeleteScheduleOverride() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (overrideId: string) =>
      scheduleService.deleteScheduleOverride(overrideId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.schedule.overrides(),
      });
      toast.success("Exceção removida com sucesso!");
    },
    onError: (error) => {
      toast.error(error?.message || "Erro ao remover exceção do cronograma.");
    },
  });
}