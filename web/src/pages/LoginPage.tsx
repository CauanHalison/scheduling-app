import { useState, type FormEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks";
import type { ApiErrorResponse, AuthRequest, RegisterProviderRequest } from "../types";
import "./styles/LoginPage.css";

interface LocationState {
  from?: {
    pathname: string;
  };
}

export function LoginPage() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as LocationState | null;
  const redirectPath = state?.from?.pathname || "/dashboard";
  const [isRegister, setRegister] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [formData, setFormData] = useState<RegisterProviderRequest>({
    name: "",
    email: "",
    password: "",
    slug: "",
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setLoading(true);

    try {
      if (isRegister) {
        await register(formData);
      }

      const payloadLogin: AuthRequest = {
        email: formData.email,
        password: formData.password,
      };

      await login(payloadLogin);
      navigate(redirectPath, { replace: true });
    } catch (error: unknown) {
      if (typeof error === "object" && error != null) {
        const apiError: ApiErrorResponse = error as ApiErrorResponse;
        console.error(apiError);
        setErrorMessage(apiError.message || "Ocorreu um erro ao autenticar. Verifique os dados e tente novamente.");
      } else {
        setErrorMessage("Erro inesperado. Tente novamente.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-container">
      <section className="login-card">
        <header className="login-header">
          <span className="login-badge">Scheduling API</span>
          <h1>{isRegister ? "Criar conta" : "Bem-vindo de volta"}</h1>
          <p>
            {isRegister
              ? "Cadastre seu perfil de prestador para gerenciar agendamentos"
              : "Entre com suas credenciais para acessar seu painel"}
          </p>
        </header>

        <div className="login-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={!isRegister}
            className={`login-tab ${!isRegister ? "active" : ""}`}
            onClick={() => {
              setRegister(false);
              setErrorMessage(null);
            }}
          >
            Entrar
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={isRegister}
            className={`login-tab ${isRegister ? "active" : ""}`}
            onClick={() => {
              setRegister(true);
              setErrorMessage(null);
            }}
          >
            Cadastrar
          </button>
        </div>

        {errorMessage && (
          <div className="login-error" role="alert">
            {errorMessage}
          </div>
        )}

        <form className="login-form" onSubmit={handleSubmit}>
          {isRegister && (
            <>
              <div className="form-group">
                <label htmlFor="name">Nome completo</label>
                <input
                  id="name"
                  name="name"
                  type="text"
                  className="form-input"
                  placeholder="Ex: João Silva"
                  value={formData.name}
                  onChange={handleChange}
                  disabled={loading}
                  required
                  maxLength={255}
                />
              </div>

              <div className="form-group">
                <label htmlFor="slug">Identificador (Slug)</label>
                <input
                  id="slug"
                  name="slug"
                  type="text"
                  className="form-input"
                  placeholder="ex: joao-silva"
                  value={formData.slug}
                  onChange={handleChange}
                  disabled={loading}
                  required
                  minLength={3}
                  maxLength={50}
                  pattern="^[a-z0-9]+(-[a-z0-9]+)*$"
                  title="Apenas letras minúsculas, números e hífens (ex: minha-clinica)"
                />
                <span className="form-hint">
                  Usado no link público da sua agenda (apenas letras minúsculas, números e hífens).
                </span>
              </div>
            </>
          )}

          <div className="form-group">
            <label htmlFor="email">E-mail</label>
            <input
              id="email"
              name="email"
              type="email"
              className="form-input"
              placeholder="seu@email.com"
              value={formData.email}
              onChange={handleChange}
              disabled={loading}
              required
              maxLength={255}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Senha</label>
            <input
              id="password"
              name="password"
              type="password"
              className="form-input"
              placeholder="••••••••"
              value={formData.password}
              onChange={handleChange}
              disabled={loading}
              required
              minLength={6}
              maxLength={72}
            />
          </div>

          {isRegister && (
            <div className="form-group">
              <label htmlFor="timeZone">Fuso horário</label>
              <input
                id="timeZone"
                name="timeZone"
                type="text"
                className="form-input"
                placeholder="America/Sao_Paulo"
                value={formData.timeZone}
                onChange={handleChange}
                disabled={loading}
                required
                maxLength={50}
              />
            </div>
          )}

          <button type="submit" className="login-submit" disabled={loading}>
            {loading
              ? "Processando..."
              : isRegister
                ? "Criar conta e entrar"
                : "Entrar"}
          </button>
        </form>

        <footer className="login-footer">
          {isRegister ? (
            <span>
              Já possui uma conta?{" "}
              <button
                type="button"
                className="login-link-btn"
                onClick={() => {
                  setRegister(false);
                  setErrorMessage(null);
                }}
              >
                Fazer login
              </button>
            </span>
          ) : (
            <span>
              Ainda não tem conta?{" "}
              <button
                type="button"
                className="login-link-btn"
                onClick={() => {
                  setRegister(true);
                  setErrorMessage(null);
                }}
              >
                Cadastre-se
              </button>
            </span>
          )}
        </footer>
      </section>
    </main>
  );
}

export default LoginPage;
