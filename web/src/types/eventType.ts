export interface EventTypeRequest {
  title: string;
  slug: string;
  description: string;
  durationMinutes: number;
  bufferAfterMinutes: number;
}

export interface EventTypeResponse {
  id: string;
  title: string;
  slug: string;
  description: string;
  durationMinutes: number;
  bufferAfterMinutes: number;
  isActive: boolean;
  createdAt: string;
}