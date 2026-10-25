import { useAuth } from "../hooks";
import "./styles/LoginPage.css";

export function DashboardPage() {
  const { provider, logout } = useAuth();

  if (!provider) {
    return null;
  }

  return (
    <main className="login-container">
      <section className="login-card">
        <header className="login-header">
          <span className="login-badge">Painel do Prestador</span>
          <h1>Olá, {provider.name}!</h1>
          <p>Sessão iniciada com sucesso.</p>
        </header>

        <div className="login-form">
          <div className="form-group">
            <label>E-mail</label>
            <div className="form-input">{provider.email}</div>
          </div>
          <div className="form-group">
            <label>Slug</label>
            <div className="form-input">{provider.slug}</div>
          </div>
          <div className="form-group">
            <label>Fuso horário</label>
            <div className="form-input">{provider.timeZone}</div>
          </div>

          <button type="button" className="login-submit" onClick={logout}>
            Sair da conta
          </button>
        </div>
      </section>
    </main>
  );
}

export default DashboardPage;

