import type { ProviderBookingFilters } from "../types";

export const queryKeys = {
  eventTypes: {
    all: ["event-types"] as const,
    list: () => ["event-types", "list"] as const,
    detail: (id: string) => ["event-types", "detail", id] as const,
  },
  event: {
    all: ["event"] as const,
    bySlug: (userSlug: string, eventSlug: string) =>
      ["event", userSlug, eventSlug] as const,
    providerEvents: (userSlug: string) => [
      "event", userSlug
    ] as const
  },
  schedule: {
    all: ["schedule"] as const,
    weekly: () => ["schedule", "weekly"] as const,
    overrides: () => ["schedule", "overrides"] as const,
  },
  days: {
    all: ["days"] as const,
    byMonth: (
      userSlug: string,
      eventSlug: string,
      year?: number,
      month?: number,
      timeZone?: string) => ["days", userSlug, eventSlug, { year, month, timeZone }] as const,
  },
  slots: {
    all: ["slots"] as const,
    byDate: (
      userSlug: string,
      eventSlug: string,
      date: string,
      timeZone?: string) => ["slots", userSlug, eventSlug, { date, timeZone }] as const
  },
  bookings: {
    all: ["bookings"] as const,
    list: (filters?: ProviderBookingFilters) =>
      ["bookings", "list", filters] as const,
    detail: (id: string) => [
      "bookings",
      "detail",
      id
    ] as const,
    guestByToken: (cancelTonen: string) => [
      "bookings",
      "guest",
      cancelTonen
    ] as const,
    magicLink: (token: string) => [
      "bookings",
      "magic-link",
      token
    ] as const
  },
} as const;