import type { EventTypeRequest, EventTypeResponse } from "../types/eventType";
import api from "./api";

export const eventTypeService = {
  async findAll(signal?: AbortSignal): Promise<EventTypeResponse[]> {
    const response = await api.get<EventTypeResponse[]>("/event-types", { signal });
    return response.data;
  },

  async findById(eventTypeId: string, signal?: AbortSignal): Promise<EventTypeResponse> {
    const response = await api.get<EventTypeResponse>(`/event-types/${eventTypeId}`, { signal });
    return response.data;
  },

  async create(payload: EventTypeRequest): Promise<EventTypeResponse> {
    const response = await api.post<EventTypeResponse>("/event-types", payload);
    return response.data;
  },

  async update(payload: EventTypeRequest, eventTypeId: string): Promise<EventTypeResponse> {
    const response = await api.put<EventTypeResponse>(`/event-types/${eventTypeId}`, payload);
    return response.data;
  },

  async toggleIsActive(eventTypeId: string): Promise<EventTypeResponse> {
    const response = await api.patch<EventTypeResponse>(`/event-types/${eventTypeId}`);
    return response.data;
  },

  async delete(eventTypeId: string): Promise<void> {
    await api.delete(`/event-types/${eventTypeId}`);
  },
};
