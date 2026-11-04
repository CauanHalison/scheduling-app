export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export const DAY_OF_WEEK_LABELS: Record<DayOfWeek, string> = {
  MONDAY: "Segunda-feira",
  TUESDAY: "Terça-feira",
  WEDNESDAY: "Quarta-feira",
  THURSDAY: "Quinta-feira",
  FRIDAY: "Sexta-feira",
  SATURDAY: "Sábado",
  SUNDAY: "Domingo",
} as const;

export interface TimeIntervalRequest {
  startTime: string;
  endTime: string;
}

export interface DayScheduleRequest {
  dayOfWeek: DayOfWeek;
  intervals: TimeIntervalRequest[];
}

export interface WeeklyScheduleRequest {
  days: DayScheduleRequest[];
}

export interface ScheduleOverrideRequest {
  startTime: string,
  endTime: string,
  reason?: string,
}

export interface TimeIntervalResponse {
  startTime: string;
  endTime: string;
}

export interface DayScheduleResponse {
  dayOfWeek: DayOfWeek;
  intervals: TimeIntervalResponse[];
}

export interface WeeklyScheduleResponse {
  timeZone: string;
  days: DayScheduleResponse[];
}

export interface ScheduleOverrideResponse {
  id: string,
  startTime: string,
  endTime: string,
  reason: string | null,
  createdAt: string
}
