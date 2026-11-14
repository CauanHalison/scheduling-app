import type { AvailableDaysResponse, AvailableSlotsResponse, BookingConfirmationResponse, CancelBookingRequest, CreateBookingRequest, EventTypeResponse, GetAvailableDaysParams, GetAvailableSlotsParams, GuestBookingResponse, MagicLinkRequest, PaginatedResponse, ProviderBookingFilters, ProviderBookingResponse, PublicEventDetailsResponse } from "../types";
import api from "./api";

export const bookingService = {
  async listProviderBookings(
    filters?: ProviderBookingFilters,
    signal?: AbortSignal): Promise<PaginatedResponse<ProviderBookingResponse>> {
    const response = await api.get<PaginatedResponse<ProviderBookingResponse>>("/provider/bookings", {
      params: filters,
      signal
    })

    return response.data;
  },

  async getProviderBookingById(id: string, signal?: AbortSignal): Promise<ProviderBookingResponse> {
    const response = await api.get<ProviderBookingResponse>(`/provider/bookings/${id}`, { signal })

    return response.data;
  },

  async cancelByProvider(id: string, payload?: CancelBookingRequest): Promise<ProviderBookingResponse> {
    const response = await api.post<ProviderBookingResponse>(`/provider/bookings/${id}/cancel`, payload ?? {})

    return response.data;
  },

  async getProviderPublicEvents(userSlug: string, signal?: AbortSignal): Promise<EventTypeResponse[]> {
    const response = await api.get<EventTypeResponse[]>(`/public/users/${userSlug}`, { signal })

    return response.data;
  },

  async getPublicEventDetails(userSlug: string, eventSlug: string, signal?: AbortSignal): Promise<PublicEventDetailsResponse> {
    const response = await api.get<PublicEventDetailsResponse>(`/public/users/${userSlug}/events/${eventSlug}`, { signal })

    return response.data
  },

  async getAvailableDays(
    userSlug: string,
    eventSulg: string,
    params: GetAvailableDaysParams,
    signal?: AbortSignal
  ): Promise<AvailableDaysResponse> {
    const response = await api.get<AvailableDaysResponse>(`/public/users/${userSlug}/events/${eventSulg}/available-days`,
      { params, signal }
    )

    return response.data
  },

  async getAvailableSlots(
    userSlug: string,
    eventSlug: string,
    params: GetAvailableSlotsParams,
    signal?: AbortSignal
  ): Promise<AvailableSlotsResponse> {
    const response = await api.get<AvailableSlotsResponse>(`/public/users/${userSlug}/events/${eventSlug}/slots`,
      { params, signal }
    )

    return response.data
  },

  async createBooking(
    userSlug: string,
    eventSlug: string,
    payload: CreateBookingRequest
  ): Promise<BookingConfirmationResponse> {
    const response = await api.post<BookingConfirmationResponse>(`/public/users/${userSlug}/events/${eventSlug}/book`, payload)

    return response.data;
  },

  async getGuestBookingByToken(
    cancelToken: string,
    signal?: AbortSignal
  ): Promise<GuestBookingResponse> {
    const response = await api.get<GuestBookingResponse>(`/guest/bookings/${cancelToken}`, { signal })

    return response.data;
  },

  async cancelByGuest(
    cancelToken: string,
    payload?: CancelBookingRequest
  ): Promise<GuestBookingResponse> {
    const response = await api.post<GuestBookingResponse>(
      `/guest/bookings/${cancelToken}/cancel`,
      payload ?? {}
    );
    return response.data;
  },
  async requestMagicLink(payload: MagicLinkRequest): Promise<void> {
    await api.post("/guest/magic-link/request", payload);
  },
  async verifyMagicLink(
    token: string,
    signal?: AbortSignal
  ): Promise<GuestBookingResponse[]> {
    const response = await api.get<GuestBookingResponse[]>(
      "/guest/magic-link/verify",
      { params: { token }, signal }
    );
    return response.data;
  },
}