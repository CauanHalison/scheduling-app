export type BookingStatus = 'CONFIRMED' | 'CANCELED';

export interface PublicEventDetailsResponse {
  providerName: string,
  providerSlug: string,
  providerTimeZone: string,
  eventTitle: string,
  eventSlug: string,
  description: string | null,
  durationMinutes: number
}

export interface AvailableDaysResponse {
  availableDays: string[]
}

export interface SlotResponse {
  startTime: string,
  endTime: string
}

export interface AvailableSlotsResponse {
  date: string,
  timeZone: string,
  slots: SlotResponse[]
}

export interface CreateBookingRequest {
  clientName: string,
  clientEmail: string,
  startTime: string
}

export interface BookingConfirmationResponse {
  id: string,
  eventTitle: string,
  providerName: string,
  clientName: string,
  clientEmail: string,
  startTime: string,
  endTime: string,
  status: BookingStatus,
  cancelToken: string,
  createdAt: string
}

export interface CancelBookingRequest {
  reason?: string
}

export interface GuestBookingResponse {
  id: string,
  eventTitle: string,
  eventDescription: string | null,
  durationMinutes: number,
  providerName: string,
  clientName: string,
  clientEmail: string,
  startTime: string,
  endTime: string,
  status: BookingStatus,
  cancellationReason: string | null
}

export interface MagicLinkRequest {
  email: string
}

export interface ProviderBookingResponse {
  id: string,
  eventTypeId: string,
  eventTitle: string,
  durationMinutes: number,
  clientName: string,
  clientEmail: string,
  startTime: string,
  endTime: string,
  status: BookingStatus,
  cancellationReason: string | null,
  createdAt: string
}

export interface ProviderBookingFilters {
  status?: BookingStatus;
  startDate?: string;
  endDate?: string;
  eventTypeId?: string;
  page?: number;
  size?: number;
  sort?: string;
}


export interface GetAvailableDaysParams {
  month?: number,
  year?: number,
  timeZone: string
}

export interface GetAvailableSlotsParams {
  date: string,
  timeZone: string
}