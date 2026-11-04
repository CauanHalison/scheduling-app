import { z } from "zod";

const TIME_REGEX = /^([01]\d|2[0-3]):([0-5]\d)$/;

export const dayOfWeekSchema = z.enum([
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
]);

export const timeIntervalSchema = z
  .object({
    startTime: z
      .string()
      .min(1, "A hora de início é obrigatória.")
      .regex(TIME_REGEX, "Formato inválido (use HH:mm)."),
    endTime: z
      .string()
      .min(1, "A hora de fim é obrigatória.")
      .regex(TIME_REGEX, "Formato inválido (use HH:mm)."),
  })
  .refine((data) => data.startTime < data.endTime, {
    message: "A hora de início deve ser anterior à hora de fim",
    path: ["endTime"],
  });

export const dayScheduleSchema = z
  .object({
    dayOfWeek: dayOfWeekSchema,
    intervals: z
      .array(timeIntervalSchema)
      .max(10, "Um dia pode ter no máximo 10 intervalos."),
  })
  .refine(
    (day) => {
      const sorted = [...day.intervals].sort((a, b) =>
        a.startTime.localeCompare(b.startTime)
      );
      for (let i = 1; i < sorted.length; i++) {
        if (sorted[i].startTime < sorted[i - 1].endTime) {
          return false;
        }
      }
      return true;
    },
    {
      message: "Os blocos de horário do mesmo dia não podem se sobrepor.",
      path: ["intervals"],
    }
  );

export const weeklyScheduleSchema = z.object({
  days: z
    .array(dayScheduleSchema)
    .max(7, "A grade semanal pode ter no máximo 7 dias"),
});

export type WeeklyScheduleFormData = z.infer<typeof weeklyScheduleSchema>;
