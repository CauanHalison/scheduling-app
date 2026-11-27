export interface TimeZoneOption {
  value: string;
  label: string;
  city: string;
  region: string;
  offset: string;
}

interface PresetTimeZone {
  value: string;
  city: string;
  region: string;
}

const PRESET_TIMEZONES: PresetTimeZone[] = [
  { value: "America/Sao_Paulo", city: "Brasília / São Paulo / Rio de Janeiro", region: "Brasil" },
  { value: "America/Manaus", city: "Manaus / Amazônia", region: "Brasil" },
  { value: "America/Cuiaba", city: "Cuiabá / Mato Grosso", region: "Brasil" },
  { value: "America/Fortaleza", city: "Fortaleza / Recife / Salvador", region: "Brasil" },
  { value: "America/Belem", city: "Belém", region: "Brasil" },
  { value: "America/Rio_Branco", city: "Rio Branco / Acre", region: "Brasil" },
  { value: "America/Noronha", city: "Fernando de Noronha", region: "Brasil" },
  { value: "America/Argentina/Buenos_Aires", city: "Buenos Aires", region: "Américas" },
  { value: "America/Santiago", city: "Santiago", region: "Américas" },
  { value: "America/Bogota", city: "Bogotá / Lima", region: "Américas" },
  { value: "America/Mexico_City", city: "Cidade do México", region: "Américas" },
  { value: "America/New_York", city: "Nova York / Miami / Toronto (EST)", region: "Américas" },
  { value: "America/Chicago", city: "Chicago / Dallas (CST)", region: "Américas" },
  { value: "America/Denver", city: "Denver (MST)", region: "Américas" },
  { value: "America/Los_Angeles", city: "Los Angeles / São Francisco / Vancouver (PST)", region: "Américas" },
  { value: "UTC", city: "Tempo Universal Coordenado (UTC)", region: "Europa & UTC" },
  { value: "Europe/Lisbon", city: "Lisboa / Porto", region: "Europa & UTC" },
  { value: "Europe/London", city: "Londres / Dublin", region: "Europa & UTC" },
  { value: "Europe/Madrid", city: "Madri / Barcelona", region: "Europa & UTC" },
  { value: "Europe/Paris", city: "Paris / Berlim / Roma / Amsterdã", region: "Europa & UTC" },
  { value: "Europe/Zurich", city: "Zurique / Genebra", region: "Europa & UTC" },
  { value: "Europe/Athens", city: "Atenas / Helsinque", region: "Europa & UTC" },
  { value: "Europe/Istanbul", city: "Istambul", region: "Europa & UTC" },
  { value: "Africa/Johannesburg", city: "Joanesburgo / Cidade do Cabo", region: "Ásia, África & Oceania" },
  { value: "Asia/Dubai", city: "Dubai / Abu Dhabi", region: "Ásia, África & Oceania" },
  { value: "Asia/Kolkata", city: "Mumbai / Nova Déli", region: "Ásia, África & Oceania" },
  { value: "Asia/Bangkok", city: "Bangkok / Jacarta", region: "Ásia, África & Oceania" },
  { value: "Asia/Singapore", city: "Singapura", region: "Ásia, África & Oceania" },
  { value: "Asia/Shanghai", city: "Xangai / Pequim / Hong Kong", region: "Ásia, África & Oceania" },
  { value: "Asia/Tokyo", city: "Tóquio / Seul", region: "Ásia, África & Oceania" },
  { value: "Australia/Sydney", city: "Sydney / Melbourne", region: "Ásia, África & Oceania" },
  { value: "Pacific/Auckland", city: "Auckland", region: "Ásia, África & Oceania" },
];

export function getTimeZoneOffset(timeZone: string): string {
  try {
    const now = new Date();
    const parts = new Intl.DateTimeFormat("en-US", {
      timeZone,
      timeZoneName: "longOffset",
    }).formatToParts(now);

    const offsetPart = parts.find((p) => p.type === "timeZoneName")?.value;
    if (!offsetPart || offsetPart === "GMT") {
      return "UTC+00:00";
    }
    return offsetPart.replace("GMT", "UTC");
  } catch {
    return "UTC";
  }
}

export function getLocalTimeZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || "America/Sao_Paulo";
  } catch {
    return "America/Sao_Paulo";
  }
}

export function getTimeZoneOptions(): TimeZoneOption[] {
  const localTz = getLocalTimeZone();
  const options: TimeZoneOption[] = PRESET_TIMEZONES.map((preset) => {
    const offset = getTimeZoneOffset(preset.value);
    return {
      value: preset.value,
      city: preset.city,
      region: preset.region,
      offset,
      label: `(${offset}) ${preset.value} — ${preset.city}`,
    };
  });

  if (localTz && !options.some((opt) => opt.value === localTz)) {
    const offset = getTimeZoneOffset(localTz);
    const city = localTz.split("/").pop()?.replace(/_/g, " ") || localTz;
    options.unshift({
      value: localTz,
      city: `${city} (Fuso local)`,
      region: "Detectado no dispositivo",
      offset,
      label: `(${offset}) ${localTz} — ${city}`,
    });
  }

  return options;
}
