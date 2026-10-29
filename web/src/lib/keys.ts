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
  },
  schedule: {
    all: ["schedule"] as const,
  },
  days: {
    all: ["days"] as const,
  },
  slots: {
    all: ["slots"] as const,
  },
  bookings: {
    all: ["bookings"] as const,
  },
} as const;