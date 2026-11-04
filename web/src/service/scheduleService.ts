import type { ScheduleOverrideRequest, ScheduleOverrideResponse, WeeklyScheduleRequest, WeeklyScheduleResponse } from "../types/schedule";
import api from "./api"

export const scheduleService = {
  async findAvailabilitySchedule(signal?: AbortSignal): Promise<WeeklyScheduleResponse> {
    const response = await api.get<WeeklyScheduleResponse>("/schedules", {signal})
    return response.data;
  },

  async replaceAvailabilitySchedule( payload: WeeklyScheduleRequest ): Promise<WeeklyScheduleResponse> {
    const response = await api.put<WeeklyScheduleResponse>("/schedules", payload);
    return response.data;
  },

  async findFutureScheduleOverrides(signal?: AbortSignal): Promise<ScheduleOverrideResponse[]> {
    const response = await api.get<ScheduleOverrideResponse[]>("/schedules/overrides", {signal});
    return response.data;
  },

  async createScheduleOverride(payload: ScheduleOverrideRequest): Promise<ScheduleOverrideResponse> {
    const response = await api.post<ScheduleOverrideResponse>("/schedules/overrides", payload);
    return response.data;
  },

  async deleteScheduleOverride(overrideId: string): Promise<void> {
    await api.delete(`/schedules/overrides/${overrideId}`);
  }
}