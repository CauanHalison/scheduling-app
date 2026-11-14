import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { CreateBookingRequest, GetAvailableDaysParams, GetAvailableSlotsParams, MagicLinkRequest, ProviderBookingFilters } from "../types";
import { queryKeys } from "../lib/keys";
import { bookingService } from "../service/bookingService";
import { STALE } from "../config/cache";
import type { CancelBookingRequest } from "../types";
import { toast } from "sonner";

export function useProviderBookings(filters?: ProviderBookingFilters) {
  return useQuery({
    queryKey: queryKeys.bookings.list(filters),
    queryFn: ({signal}) => bookingService.listProviderBookings(filters, signal),
    staleTime: STALE.bookings,
    placeholderData: keepPreviousData
  });
}

export function useProviderBooking(id: string) {
  return useQuery({
    queryKey: queryKeys.bookings.detail(id),
    queryFn: ({signal}) => bookingService.getProviderBookingById(id, signal),
    staleTime: STALE.bookings,
    enabled: !!id
  });
}

export function useCancelProviderBooking() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn:({
      id,
      payload
    }: {
      id: string,
      payload?: CancelBookingRequest
    }) => 
      bookingService.cancelByProvider(id, payload),
      onSuccess: (updateBooking) => {
        queryClient.setQueryData(
          queryKeys.bookings.detail(updateBooking.id),
          updateBooking
        );
        queryClient.invalidateQueries({
          queryKey: queryKeys.bookings.all
        });
        toast.success("Agendamento cancelado com sucesso!")
      },
      onError: (error) => {
        toast.error(error?.message || "Erro ao cancelar o agendamento.")
      }
  });
}

export function useProviderPublicEvents(userSlug: string) {
  return useQuery({
    queryKey: queryKeys.event.providerEvents(userSlug),
    queryFn: ({signal}) => bookingService.getProviderPublicEvents(userSlug, signal),
    enabled: !!userSlug,
    staleTime: STALE.eventTypes
  });
}

export function usePublicEventDetails(userSlug: string, eventSlug: string) {
  return useQuery({
    queryKey: queryKeys.event.bySlug(userSlug, eventSlug),
    queryFn: ({signal}) => bookingService.getPublicEventDetails(userSlug, eventSlug, signal),
    enabled: !!userSlug && !!eventSlug,
    staleTime: STALE.event
  });
}

export function useAvailableDays(userSlug: string, eventSlug: string, params:GetAvailableDaysParams) {
  return useQuery({
    queryKey: queryKeys.days.byMonth(
      userSlug,
      eventSlug,
      params.year,
      params.month,
      params.timeZone
    ),
    queryFn: ({signal}) => bookingService.getAvailableDays(userSlug, eventSlug, params, signal),
    enabled: !!userSlug && !!eventSlug && !!params.timeZone,
    staleTime: STALE.days
  });
}

export function useAvailableSlots(userSlug: string, eventSlug: string, params: GetAvailableSlotsParams) {
  return useQuery({
    queryKey: queryKeys.slots.byDate(
      userSlug, 
      eventSlug, 
      params.date, 
      params.timeZone
    ),
    queryFn: ({signal}) => bookingService.getAvailableSlots(userSlug, eventSlug, params, signal),
    enabled: !!userSlug && !!eventSlug && !!params.date && !!params.timeZone,
    staleTime: STALE.slots,
    refetchOnWindowFocus: true
  });
}

export function useCreateBookings(userSlug: string, eventSlug: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateBookingRequest) => bookingService.createBooking(userSlug, eventSlug, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.slots.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.days.all });
      toast.success("Agendamento realizado com sucesso!");
    },
    onError: (error) => {
      if(error?.statusCode === 409 || error?.statusCode === 400) {
        queryClient.invalidateQueries({ queryKey: queryKeys.slots.all });
        queryClient.invalidateQueries({ queryKey: queryKeys.days.all });
      }

      toast.error(error?.message || "Não foi possível concluir o agendamento.");
    }
  })
}

export function useGuestBooking(cancelToken: string) {
  return useQuery({
    queryKey: queryKeys.bookings.guestByToken(cancelToken),
    queryFn: ({signal}) => bookingService.getGuestBookingByToken(cancelToken, signal),
    enabled: !!cancelToken,
    staleTime: STALE.bookings
  })
}

export function useCancelGuestBooking() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      cancelToken,
      payload,
    }: {
      cancelToken: string;
      payload?: CancelBookingRequest;
    }) => bookingService.cancelByGuest(cancelToken, payload),
    onSuccess: (updatedBooking, { cancelToken }) => {
      queryClient.setQueryData(
        queryKeys.bookings.guestByToken(cancelToken),
        updatedBooking
      );
      queryClient.invalidateQueries({ queryKey: queryKeys.bookings.all });
      toast.success("Seu agendamento foi cancelado.");
    },
    onError: (error) => {
      toast.error(error?.message || "Erro ao cancelar o agendamento.");
    }
  })
}

export function useRequestMagicLink() {
  return useMutation({
    mutationFn: (payload: MagicLinkRequest) =>
      bookingService.requestMagicLink(payload),
    onSuccess: () => {
      toast.success("Se houver agendamentos para este e-mail, enviaremos um link de acesso!");
    },
    onError: (error) => {
      toast.error(error?.message || "Erro ao solicitar o link de acesso.");
    }
  })
}


export function useVerifyMagicLink(token: string) {
  return useQuery({
    queryKey: queryKeys.bookings.magicLink(token),
    queryFn: ({ signal }) => bookingService.verifyMagicLink(token, signal),
    enabled: !!token,
    staleTime: Infinity, 
    retry: false,        
    refetchOnWindowFocus: false
  })
}