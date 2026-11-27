import { useEffect, useMemo, useRef, useState } from "react";
import { Check, ChevronDown, Globe, LocateFixed, Search, X } from "lucide-react";
import {
  getLocalTimeZone,
  getTimeZoneOffset,
  getTimeZoneOptions,
  type TimeZoneOption,
} from "../utils/timezones";

interface TimeZoneSelectProps {
  id?: string;
  value: string;
  onChange: (timeZone: string) => void;
  disabled?: boolean;
}

export function TimeZoneSelect({
  id = "timeZone",
  value,
  onChange,
  disabled = false,
}: TimeZoneSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  const localTimeZone = useMemo(() => getLocalTimeZone(), []);
  const presetOptions = useMemo(() => getTimeZoneOptions(), []);

  const allBrowserZones = useMemo(() => {
    try {
      if (typeof Intl !== "undefined" && "supportedValuesOf" in Intl) {
        return (Intl as unknown as { supportedValuesOf: (key: string) => string[] }).supportedValuesOf(
          "timeZone"
        );
      }
    } catch {
      return [];
    }
    return [];
  }, []);

  const selectedOption = useMemo<TimeZoneOption>(() => {
    const found = presetOptions.find((opt) => opt.value === value);
    if (found) return found;

    const offset = getTimeZoneOffset(value || localTimeZone);
    const cleanValue = value || localTimeZone;
    const city = cleanValue.split("/").pop()?.replace(/_/g, " ") || cleanValue;
    return {
      value: cleanValue,
      city,
      region: "Outros fusos IANA",
      offset,
      label: `(${offset}) ${cleanValue} — ${city}`,
    };
  }, [presetOptions, value, localTimeZone]);

  const filteredGroups = useMemo(() => {
    const query = search.trim().toLowerCase();

    let matchingOptions = presetOptions.filter((opt) => {
      if (!query) return true;
      return (
        opt.value.toLowerCase().includes(query) ||
        opt.city.toLowerCase().includes(query) ||
        opt.region.toLowerCase().includes(query) ||
        opt.offset.toLowerCase().includes(query)
      );
    });

    if (query.length >= 2 && allBrowserZones.length > 0) {
      const presetValues = new Set(presetOptions.map((o) => o.value));
      const extraZones = allBrowserZones
        .filter(
          (tz) =>
            !presetValues.has(tz) &&
            tz.toLowerCase().replace(/_/g, " ").includes(query.replace(/_/g, " "))
        )
        .slice(0, 12)
        .map((tz): TimeZoneOption => {
          const offset = getTimeZoneOffset(tz);
          const city = tz.split("/").pop()?.replace(/_/g, " ") || tz;
          return {
            value: tz,
            city,
            region: "Outros fusos IANA",
            offset,
            label: `(${offset}) ${tz} — ${city}`,
          };
        });

      matchingOptions = [...matchingOptions, ...extraZones];
    }

    const groups = new Map<string, TimeZoneOption[]>();
    for (const option of matchingOptions) {
      const list = groups.get(option.region) ?? [];
      list.push(option);
      groups.set(option.region, list);
    }

    return Array.from(groups.entries());
  }, [presetOptions, allBrowserZones, search]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      setTimeout(() => searchInputRef.current?.focus(), 0);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  const handleSelect = (tz: string) => {
    onChange(tz);
    setIsOpen(false);
    setSearch("");
  };

  return (
    <div ref={containerRef} className="relative">
      <div className="flex flex-col gap-1.5">
        <button
          id={id}
          type="button"
          disabled={disabled}
          aria-haspopup="listbox"
          aria-expanded={isOpen}
          onClick={() => setIsOpen((prev) => !prev)}
          className={`flex w-full cursor-pointer items-center justify-between gap-2 rounded-xl border bg-white px-3.5 py-2.5 text-left text-sm transition duration-200 focus:outline-none disabled:cursor-not-allowed disabled:bg-slate-100 disabled:opacity-70 ${
            isOpen
              ? "border-blue-600 ring-3 ring-blue-500/15"
              : "border-slate-300 hover:border-slate-400"
          }`}
        >
          <span className="flex min-w-0 items-center gap-2.5">
            <Globe className="h-4 w-4 shrink-0 text-blue-600" />
            <span className="truncate font-medium text-slate-900">
              {selectedOption.city}
            </span>
            <span className="inline-flex shrink-0 items-center rounded-md border border-blue-200/80 bg-blue-50 px-2 py-0.5 font-mono text-xs font-semibold text-blue-700">
              {selectedOption.offset}
            </span>
          </span>
          <ChevronDown
            className={`h-4 w-4 shrink-0 text-slate-400 transition-transform duration-200 ${
              isOpen ? "rotate-180 text-blue-600" : ""
            }`}
          />
        </button>

        {value !== localTimeZone && (
          <button
            type="button"
            disabled={disabled}
            onClick={() => handleSelect(localTimeZone)}
            className="inline-flex cursor-pointer items-center gap-1.5 self-start text-xs font-medium text-blue-600 hover:text-blue-700 hover:underline"
          >
            <LocateFixed className="h-3.5 w-3.5" />
            Usar fuso detectado ({localTimeZone})
          </button>
        )}
      </div>

      {isOpen && (
        <div
          role="listbox"
          className="absolute z-50 mt-1.5 w-full overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl shadow-slate-900/10"
        >
          <div className="border-b border-slate-100 p-2">
            <div className="relative flex items-center">
              <Search className="pointer-events-none absolute left-3 h-4 w-4 text-slate-400" />
              <input
                ref={searchInputRef}
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Escape") {
                    setIsOpen(false);
                  }
                }}
                placeholder="Buscar cidade, país, fuso (ex: Sao_Paulo, UTC-3, Lisbon)..."
                className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2 pl-9 pr-8 text-xs text-slate-900 placeholder:text-slate-400 focus:border-blue-600 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/15"
              />
              {search && (
                <button
                  type="button"
                  onClick={() => setSearch("")}
                  className="absolute right-2.5 cursor-pointer rounded p-0.5 text-slate-400 hover:bg-slate-200/60 hover:text-slate-600"
                  aria-label="Limpar busca"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              )}
            </div>
          </div>

          <div className="max-h-64 overflow-y-auto p-1.5">
            {filteredGroups.length === 0 ? (
              <div className="py-6 text-center text-xs text-slate-500">
                Nenhum fuso horário encontrado para "{search}".
              </div>
            ) : (
              filteredGroups.map(([region, options]) => (
                <div key={region} className="mb-2 last:mb-0">
                  <div className="flex flex-col gap-0.5">
                    {options.map((option) => {
                      const isSelected = option.value === value;
                      return (
                        <button
                          key={option.value}
                          type="button"
                          role="option"
                          aria-selected={isSelected}
                          onClick={() => handleSelect(option.value)}
                          className={`flex w-full cursor-pointer items-center justify-between gap-2 rounded-lg px-2.5 py-2 text-left text-xs transition ${
                            isSelected
                              ? "bg-blue-50 font-semibold text-blue-700"
                              : "text-slate-700 hover:bg-slate-100"
                          }`}
                        >
                          <div className="flex min-w-0 flex-col gap-0.5">
                            <div className="flex items-center gap-1.5">
                              <span className="truncate font-medium text-slate-900">
                                {option.city}
                              </span>
                            </div>
                            <span className="truncate font-mono text-[11px] text-slate-500">
                              {option.value}
                            </span>
                          </div>

                          <div className="flex shrink-0 items-center gap-2">
                            <span
                              className={`rounded px-1.5 py-0.5 font-mono text-[11px] ${
                                isSelected
                                  ? "bg-blue-100 text-blue-800"
                                  : "bg-slate-100 text-slate-600"
                              }`}
                            >
                              {option.offset}
                            </span>
                            {isSelected && <Check className="h-4 w-4 text-blue-600" />}
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default TimeZoneSelect;
