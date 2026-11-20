import { Calendar } from "lucide-react";
import { Link } from "react-router-dom";

export function PublicNavbar() {
  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex w-full items-center justify-start gap-4 px-4 py-3">
        <div className="flex items-center gap-2 text-blue-600">
          <Calendar className="h-5 w-5" />
          <span className="text-base font-bold tracking-tight text-slate-900">
            Scheduling APP
          </span>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/guest/magic-link"
            className="btn-secondary py-1.5 text-xs sm:text-sm"
          >
            Meus Agendamentos
          </Link>
        </div>
      </div>
    </header>
  );
}

export default PublicNavbar;
