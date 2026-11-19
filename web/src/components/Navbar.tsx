import { NavLink } from "react-router-dom";
import { useAuth } from "../hooks";

export function Navbar() {
  const { provider, logout } = useAuth();

  const navBtnClass = ({ isActive }: { isActive: boolean }) =>
    `rounded-lg px-3 py-1.5 text-sm font-medium transition duration-200 ${
      isActive
        ? "bg-white text-blue-600 shadow-sm"
        : "text-blue-100 hover:bg-blue-500 hover:text-white"
    }`

  return (
    <header className="bg-blue-600 text-white shadow-sm">
      <div className="mx-auto flex flex-row w-full justify-between px-4 py-3 items-center">
        <div className="content-center">
          <span className="text-lg font-bold tracking-tight">Scheduling APP</span>
        </div>

        <div className="flex flex-row gap-3 flex-nowrap">
          <nav className="flex flex-row gap-3 items-center">
            <NavLink to="/dashboard" className={navBtnClass} end>
              Visão Geral
            </NavLink>

            <NavLink to="/dashboard/event-types" className={navBtnClass}>
              Tipos de Eventos
            </NavLink>

            <NavLink to="/dashboard/schedules" className={navBtnClass}>
              Disponibilidade
            </NavLink>
          </nav>

          <div className="flex flex-row gap-2 flex-nowrap items-center gap-3">
            <div className="content-center flex items-center gap-1.5 text-sm">
              <span className="font-medium text-white">{provider?.name}</span>
              <span className="text-blue-300"> • </span> 
              <span className="rounded-md border border-blue-400/30 bg-blue-700/60 px-2 py-0.5 font-mono text-sm text-blue-100">/{provider?.slug}</span>
            </div>

            <button className="cursor-pointer rounded-lg border border-blue-400/40 bg-blue-700/50 px-3 py-1.5 text-sm font-medium text-white transition duration-200 hover:border-red-500 hover:bg-red-600" type="button" onClick={logout} title="Sair da conta">
              Sair
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}

export default Navbar;
