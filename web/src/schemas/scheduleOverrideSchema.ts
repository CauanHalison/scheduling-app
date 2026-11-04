import { z } from "zod";

export const scheduleOverrideSchema = z.object({
  startTime: z
    .string()
    .min(1, "A data de início é obrigatória.")
    .refine((val) => !Number.isNaN(Date.parse(val)), {
      message: "Data de início inválida.",
    }),

  endTime: z
    .string()
    .min(1, "A data de fim é obrigatória.")
    .refine((val) => !Number.isNaN(Date.parse(val)), {
      message: "Data de fim inválida.",
    })
    .refine((val) => new Date(val) > new Date(), {
      message: "A data final deve ser no futuro.",
    }),

  reason: z
    .string()
    .max(255, "A justificativa pode ter no máximo 255 caracteres.")
    .optional(),
}).refine((data) => new Date(data.endTime) > new Date(data.startTime), {
  message: "A data final deve ser posterior à data inicial.",
  path: ["endTime"],
});

export type ScheduleOverrideFormData = z.infer<typeof scheduleOverrideSchema>;