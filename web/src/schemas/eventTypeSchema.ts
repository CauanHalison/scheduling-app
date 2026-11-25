import { z } from "zod";

export const eventTypeSchema = z.object({
  title: z
    .string()
    .min(3, "O título deve ter no mínimo 3 caracteres")
    .max(100, "O título deve ter no máximo 100 caracteres"),

  slug: z
    .string()
    .min(2, "O slug deve ter no mínimo 2 caracteres")
    .max(50, "O slug deve ter no máximo 50 caracteres")
    .regex(
      /^[a-z0-9]+(?:-[a-z0-9]+)*$/,
      "Use apenas letras minúsculas, números e hífens (ex: consulta-inicial)"
    ),

  eventLink: z
    .string()
    .max(500, "O link deve ter no máximo 500 caracteres")
    .regex(
      /^(\s*|\s*https?:\/\/\S+\s*)$/,
      "O link do evento deve ser uma URL válida iniciando com http:// ou https://"
    )
    .default(""),

  description: z
    .string()
    .max(500, "A descrição não pode passar de 500 caracteres")
    .default(""),

  durationMinutes: z
    .number({ error: "A duração deve ser um número" })
    .min(5, "A duração mínima é de 5 minutos")
    .max(1440, "A duração máxima é de 24 horas (1440 min)"),

  bufferAfterMinutes: z
    .number({ error: "O intervalo deve ser um número" })
    .min(0, "O intervalo não pode ser negativo")
    .max(180, "O intervalo máximo é de 3 horas (180 min)")
    .default(0),
});

export type EventTypeFormData = z.infer<typeof eventTypeSchema>;

