import { NavLink } from "react-router-dom";
import { useAuth } from "../hooks";
import "./styles/Navbar.css";

export function Navbar() {
  const { provider, logout } = useAuth();

  return (
    <header className="navbar-container">
      <div className="navbar-content">
        <div className="navbar-brand">
          <span className="brand-logo">📅</span>
          <span className="brand-title">Scheduling</span>
        </div>

        <nav className="navbar-nav">
          <NavLink
            to="/dashboard"
            end
            className={({ isActive }) =>
              `nav-link ${isActive ? "active" : ""}`
            }
          >
            Visão Geral
          </NavLink>

          <NavLink
            to="/dashboard/event-types"
            className={({ isActive }) =>
              `nav-link ${isActive ? "active" : ""}`
            }
          >
            Tipos de Eventos
          </NavLink>
        </nav>

        <div className="navbar-user">
          <div className="user-info">
            <span className="user-name">{provider?.name}</span>
            <span className="user-slug">@{provider?.slug}</span>
          </div>

          <button type="button" className="btn-logout" onClick={logout} title="Sair da conta">
            Sair
          </button>
        </div>
      </div>
    </header>
  );
}

export default Navbar;

