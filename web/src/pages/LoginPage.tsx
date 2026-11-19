import { useState, type FormEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks";
import type { ApiErrorResponse, AuthRequest, RegisterProviderRequest } from "../types";

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
        setErrorMessage(
          apiError.message ||
            "Ocorreu um erro ao autenticar. Verifique os dados e tente novamente."
        );
      } else {
        setErrorMessage("Erro inesperado. Tente novamente.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-blue-100 flex items-center justify-center p-4">
      <section className="w-full max-w-md rounded-2xl border border-blue-100 bg-white p-6 sm:p-8 shadow-xl shadow-blue-900/5 flex flex-col gap-6">
        <header className="flex flex-col items-center text-center gap-2">
          <span className="badge-blue">Scheduling APP</span>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            {isRegister ? "Criar conta" : "Bem-vindo de volta"}
          </h1>
          <p className="text-sm text-slate-500">
            {isRegister
              ? "Cadastre seu perfil de prestador para gerenciar agendamentos"
              : "Entre com suas credenciais para acessar seu painel"}
          </p>
        </header>

        <div
          role="tablist"
          className="grid grid-cols-2 gap-1 rounded-xl bg-blue-50/80 p-1 border border-blue-100"
        >
          <button
            type="button"
            role="tab"
            aria-selected={!isRegister}
            className={`cursor-pointer rounded-lg py-2 text-sm font-semibold transition duration-200 ${
              !isRegister
                ? "bg-blue-600 text-white shadow-sm"
                : "text-blue-700 hover:bg-blue-100/60"
            }`}
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
            className={`cursor-pointer rounded-lg py-2 text-sm font-semibold transition duration-200 ${
              isRegister
                ? "bg-blue-600 text-white shadow-sm"
                : "text-blue-700 hover:bg-blue-100/60"
            }`}
            onClick={() => {
              setRegister(true);
              setErrorMessage(null);
            }}
          >
            Cadastrar
          </button>
        </div>

        {errorMessage && (
          <div role="alert" className="alert-error">
            {errorMessage}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {isRegister && (
            <>
              <div className="form-group">
                <label htmlFor="name" className="form-label">
                  Nome completo
                </label>
                <input
                  id="name"
                  name="name"
                  type="text"
                  className="input-field"
                  placeholder="Ex: João Silva"
                  value={formData.name}
                  onChange={handleChange}
                  disabled={loading}
                  required
                  maxLength={255}
                />
              </div>

              <div className="form-group">
                <label htmlFor="slug" className="form-label">
                  Slug
                </label>
                <div className="flex items-center overflow-hidden rounded-xl border border-slate-300 bg-white transition duration-300 focus-within:border-blue-600 focus-within:ring-3 focus-within:ring-blue-500/15">
                  <span className="shrink-0 select-none border-r border-slate-200 bg-slate-50 px-3 py-2.5 text-xs font-mono text-slate-500">
                    scheduling-app/users/
                  </span>
                  <input
                    id="slug"
                    name="slug"
                    type="text"
                    className="w-full px-3 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none disabled:bg-slate-100"
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
                </div>
              </div>
            </>
          )}

          <div className="form-group">
            <label htmlFor="email" className="form-label">
              E-mail
            </label>
            <input
              id="email"
              name="email"
              type="email"
              className="input-field"
              placeholder="seu@email.com"
              value={formData.email}
              onChange={handleChange}
              disabled={loading}
              required
              maxLength={255}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password" className="form-label">
              Senha
            </label>
            <input
              id="password"
              name="password"
              type="password"
              className="input-field"
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
              <label htmlFor="timeZone" className="form-label">
                Fuso horário
              </label>
              <input
                id="timeZone"
                name="timeZone"
                type="text"
                className="input-field"
                placeholder="America/Sao_Paulo"
                value={formData.timeZone}
                onChange={handleChange}
                disabled={loading}
                required
                maxLength={50}
              />
            </div>
          )}

          <button type="submit" className="btn-primary w-full mt-2" disabled={loading}>
            {loading
              ? "Processando..."
              : isRegister
                ? "Criar conta e entrar"
                : "Entrar"}
          </button>
        </form>

        <footer className="border-t border-slate-100 pt-4 text-center text-sm text-slate-600">
          {isRegister ? (
            <span>
              Já possui uma conta?{" "}
              <button
                type="button"
                className="cursor-pointer font-semibold text-blue-600 hover:text-blue-700 hover:underline"
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
                className="cursor-pointer font-semibold text-blue-600 hover:text-blue-700 hover:underline"
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
