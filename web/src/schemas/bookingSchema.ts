import {z} from "zod";

export const createBookingSchema = z.object({
  clientName: z
    .string()
    .min(2, "Informe seu nome")
    .max(255, "O nome deve ter no máximo 255 caracteres"),
  clientEmail: z
    .email("Informe um email válido")
    .max(255, "O email deve ter no máximo 255 caracteres"),
  startTime: z
  .string()
  .min(1, "Selecione um hórario disponível")
})

export const cancelBookingSchema = z.object({
  reason: z
    .string()
    .max(500, "O motivo deve ter no máximo 500 caracteres")
    .optional()
})

export const magicLinkSchema = z.object({
  email: z
    .email("Informe um email válido")
    .max(255, "O email deve ter no máximo 255 caracteres")
})

export type CreateBookingFormData = z.infer<typeof createBookingSchema> 
export type CancelBookingFormData = z.infer<typeof cancelBookingSchema> 
export type MagicLinkSchema = z.infer<typeof magicLinkSchema> 