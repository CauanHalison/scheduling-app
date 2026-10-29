import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { queryKeys } from "../lib/keys";
import { eventTypeService } from "../service/eventTypeService";
import type { EventTypeRequest, EventTypeResponse } from "../types/eventType";

export function useEventTypes() {
  return useQuery({
    queryKey: queryKeys.eventTypes.list(),
    queryFn: ({ signal }) => eventTypeService.findAll(signal),
  });
}

export function useEventType(eventTypeId: string) {
  const queryClient = useQueryClient();

  return useQuery({
    queryKey: queryKeys.eventTypes.detail(eventTypeId),
    queryFn: ({ signal }) => eventTypeService.findById(eventTypeId, signal),
    enabled: !!eventTypeId,
    refetchOnWindowFocus: false,
    initialData: (): EventTypeResponse | undefined => {
      const list = queryClient.getQueryData<EventTypeResponse[]>(
        queryKeys.eventTypes.list()
      );
      return list ? list.find((event) => event.id === eventTypeId) : undefined;
    },
    initialDataUpdatedAt: (): number | undefined => {
      return queryClient.getQueryState(queryKeys.eventTypes.list())?.dataUpdatedAt;
    },
  });
}

export function useCreateEventType() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: EventTypeRequest) => eventTypeService.create(payload),
    onSuccess: (created) => {
      queryClient.setQueryData(queryKeys.eventTypes.detail(created.id), created);
      queryClient.invalidateQueries({
        queryKey: queryKeys.eventTypes.list(),
      });
      toast.success("Tipo de evento criado com sucesso!");
    },
    onError: (error) => {
      if (error?.statusCode !== 409) {
        toast.error(error?.message || "Erro ao criar tipo de evento.");
      }
    },
  });
}

export function useUpdateEventType() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      eventTypeId,
      payload,
    }: {
      eventTypeId: string;
      payload: EventTypeRequest;
    }) => eventTypeService.update(payload, eventTypeId),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.eventTypes.detail(updated.id), updated);
      queryClient.invalidateQueries({
        queryKey: queryKeys.eventTypes.list(),
      });
      toast.success("Tipo de evento atualizado com sucesso!");
    },
    onError: (error, { eventTypeId }) => {
      if (error?.statusCode === 409) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.eventTypes.detail(eventTypeId),
        });
        queryClient.invalidateQueries({
          queryKey: queryKeys.eventTypes.list(),
        });
      } else {
        toast.error(error?.message || "Erro ao atualizar tipo de evento.");
      }
    },
  });
}

export function useToggleEventType() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => eventTypeService.toggleIsActive(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.eventTypes.all });

      const previousList = queryClient.getQueryData<EventTypeResponse[]>(
        queryKeys.eventTypes.list()
      );
      const previousDetail = queryClient.getQueryData<EventTypeResponse>(
        queryKeys.eventTypes.detail(id)
      );

      queryClient.setQueryData<EventTypeResponse[]>(
        queryKeys.eventTypes.list(),
        (old) =>
          old?.map((item) =>
            item.id === id ? { ...item, isActive: !item.isActive } : item
          )
      );
      queryClient.setQueryData<EventTypeResponse>(
        queryKeys.eventTypes.detail(id),
        (old) => (old ? { ...old, isActive: !old.isActive } : undefined)
      );

      return { previousList, previousDetail };
    },
    onError: (_err, id, ctx) => {
      if (ctx?.previousList) {
        queryClient.setQueryData(queryKeys.eventTypes.list(), ctx.previousList);
      }
      if (ctx?.previousDetail) {
        queryClient.setQueryData(queryKeys.eventTypes.detail(id), ctx.previousDetail);
      }
      toast.error("Não foi possível alterar o status do evento.");
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.eventTypes.all });
    },
  });
}

export function useDeleteEventType() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (eventTypeId: string) => eventTypeService.delete(eventTypeId),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: queryKeys.eventTypes.detail(id) });
      queryClient.invalidateQueries({
        queryKey: queryKeys.eventTypes.list(),
      });
      toast.success("Tipo de evento excluído com sucesso!");
    },
    onError: (error) => {
      toast.error(error?.message || "Erro ao excluir o tipo de evento.");
    },
  });
}
