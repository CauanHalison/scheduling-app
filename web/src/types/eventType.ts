export interface EventTypeRequest {
  title: string;
  slug: string;
  eventLink?: string;
  description: string;
  durationMinutes: number;
  bufferAfterMinutes: number;
}

export interface EventTypeResponse {
  id: string;
  title: string;
  slug: string;
  eventLink?: string | null;
  description: string;
  durationMinutes: number;
  bufferAfterMinutes: number;
  isActive: boolean;
  createdAt: string;
}