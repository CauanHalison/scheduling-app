import { Outlet } from "react-router-dom";
import { PublicNavbar } from "../components";

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <PublicNavbar />
      <main className="mx-auto w-full fex-1 px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}