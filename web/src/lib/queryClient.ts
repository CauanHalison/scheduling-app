import { QueryClient } from "@tanstack/react-query";
import { STALE } from "../config/cache";
import type { ApiErrorResponse } from "../types";
import { queryKeys } from "./keys";

declare module "@tanstack/react-query" {
  interface Register {
    defaultError: ApiErrorResponse;
  }
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60,
      gcTime: 1000 * 60 * 5,
      refetchOnWindowFocus: true,
      refetchOnReconnect: true,
      retry: (failureCount, error) => {
        if (error.statusCode >= 400 && error.statusCode < 500) {
          return false;
        }
        return failureCount < 2;
      },
    },
    mutations: {
      retry: false,
    },
  },
});

queryClient.setQueryDefaults(queryKeys.eventTypes.all, { staleTime: STALE.eventTypes });
queryClient.setQueryDefaults(queryKeys.event.all, { staleTime: STALE.event });
queryClient.setQueryDefaults(queryKeys.schedule.all, { staleTime: STALE.schedule });
queryClient.setQueryDefaults(queryKeys.days.all, { staleTime: STALE.days });
queryClient.setQueryDefaults(queryKeys.slots.all, { staleTime: STALE.slots });
queryClient.setQueryDefaults(queryKeys.bookings.all, { staleTime: STALE.bookings });
