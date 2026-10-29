import { jwtDecode } from "jwt-decode";
import type { ProviderResponse } from "../types";

interface CustomJwtPayload {
  sub: string;
  name: string;
  email: string;
  slug: string;
  timeZone: string;
  exp: number;
}

export function decodeToken(token: string): ProviderResponse | null {
  try {
    const payload = jwtDecode<CustomJwtPayload>(token);

    const nowInSeconds = Math.floor(Date.now() / 1000);
    if (payload.exp && payload.exp < nowInSeconds) {
      return null;
    }

    return {
      id: payload.sub,
      name: payload.name,
      email: payload.email,
      slug: payload.slug,
      timeZone: payload.timeZone,
    };
  } catch {
    return null;
  }
}
