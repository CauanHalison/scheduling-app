import { Clock, Globe } from "lucide-react";

interface PublicHeaderProps {
  userSlug: string;
  headerTitle: string;
  headerDescription: string;
  servicesCount?: number;
  durationMinutes?: number;
}

export function PublicHeader({
  userSlug,
  headerTitle,
  headerDescription,
  servicesCount,
  durationMinutes,
}: PublicHeaderProps) {
  return (
    <header className="mb-6 flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-xs sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-center gap-4">
        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-blue-600 text-xl font-bold text-white shadow-sm shadow-blue-600/20">
          {userSlug.charAt(0).toUpperCase()}
        </div>
        <div>
          <span className="badge-blue mb-1">@{userSlug}</span>
          <h1 className="page-title">{headerTitle}</h1>
          <p className="page-subtitle">{headerDescription}</p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2 text-xs font-medium text-slate-600">
        {durationMinutes !== undefined && (
          <span className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5">
            <Clock className="h-3.5 w-3.5 text-blue-600" />
            {durationMinutes} min
          </span>
        )}
        <span className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5">
          <Globe className="h-3.5 w-3.5 text-blue-600" />
          {Intl.DateTimeFormat().resolvedOptions().timeZone}
        </span>
        {servicesCount !== undefined && (
          <span className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5">
            {servicesCount} {servicesCount === 1 ? "serviço" : "serviços"}
          </span>
        )}
      </div>
    </header>
  );
}
