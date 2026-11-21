import { NavLink } from "react-router-dom";
import { useAuth } from "../hooks";
import { useState } from "react";
import { Menu, X } from "lucide-react";

export function Navbar() {
  const { provider, logout } = useAuth();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const closeMenu = () => setIsMenuOpen(false);

  const navBtnClass = ({ isActive }: { isActive: boolean }) =>
    `rounded-lg px-3 py-1.5 text-sm font-medium transition duration-200 ${
      isActive
        ? "bg-white text-blue-600 shadow-sm"
        : "text-blue-100 hover:bg-blue-500 hover:text-white"
    }`

  return (
    <header className="bg-blue-600 text-white shadow-sm">
      <div className={`mx-auto flex w-full justify-between px-4 py-3 ${isMenuOpen ? 'flex-col items-start md:flex-row md:items-center': 'flex-row items-center'}`}>
        <div className="flex flex-row justify-between w-full md:w-auto">
          <div className="content-center">
            <span className="text-lg font-bold tracking-tight">Scheduling APP</span>
          </div>
          
          <button
            type="button"
            onClick={() => setIsMenuOpen((prev) => !prev)}
            className="inline-flex cursor-pointer items-center justify-center rounded-lg p-2 text-blue-100 transition hover:bg-blue-500 hover:text-white md:hidden"
          >
            {isMenuOpen ?<X className="h-6 w-6"/> : <Menu className="h-6 w-6"/>}
          </button>
        </div>

        <div className="hidden flex-row gap-3 flex-nowrap md:flex">
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

            <button className="cursor-pointer rounded-lg border border-blue-400/40 bg-blue-700/50 px-3 py-1.5 text-sm font-medium text-white transition duration-200 hover:border-red-500 hover:bg-red-600" 
              type="button" 
              onClick={logout} 
              title="Sair da conta"
            >
              Sair
            </button>
          </div>
        </div>

        {isMenuOpen && (
          <div 
            className="border-t border-white bg-blue-600 px-4 w-full pt-2 pb-4 md:hidden"
          >
            <nav className="flex flex-col gap-2">
              <NavLink to="/dashboard" className={navBtnClass} onClick={closeMenu} end>
                Visão Geral
              </NavLink>
              <NavLink to="/dashboard/event-types" className={navBtnClass} onClick={closeMenu}>
                Tipos de Eventos
              </NavLink>
              <NavLink to="/dashboard/schedules" className={navBtnClass} onClick={closeMenu}>
                Disponibilidade
              </NavLink>
              <button
                className="cursor-pointer text-start rounded-lg border-2 border-red-400/40 bg-red-700/50 px-4 py-2 text-sm font-medium text-white transition duration-200 hover:border-red-500 hover:bg-red-600"
                type="button"
                onClick={logout}
                title="sair"
              >
                Sair
              </button>
            </nav>

            <div 
              className="mt-3 flex items-center justify-between border-t border-blue-500/60 pt-3"
            >
              <div
                className="flex flex-row gap-2 text-sm"
              >
                <span className="font-medium text-white">
                  {provider?.name}
                </span>
                <span className="w-fit rounded-md border border-blue-400/30 bg-blue-700/60 px-2 py-0.5 font-mono text-xs text-blue-100">
                  /{provider?.slug}
                </span>
              </div>

            </div>

          </div>
        )}
      </div>
    </header>
  );
}

export default Navbar;
