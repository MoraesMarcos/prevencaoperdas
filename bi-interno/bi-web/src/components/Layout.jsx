import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { DollarSign, ShoppingCart, ShieldAlert, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const abas = [
  { to: '/financeira', label: 'Financeira', icon: DollarSign },
  { to: '/comercial', label: 'Comercial', icon: ShoppingCart },
  { to: '/prevencao-perdas', label: 'Prevenção de Perdas', icon: ShieldAlert },
];

export default function Layout() {
  const { usuario, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-950">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <h1 className="text-lg font-bold text-teal-400">BI Interno</h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-400">{usuario?.nome}</span>
            <button onClick={handleLogout} className="flex items-center gap-1 text-sm text-slate-400 hover:text-red-400 transition-colors">
              <LogOut size={16} /> Sair
            </button>
          </div>
        </div>
        <nav className="max-w-6xl mx-auto px-6 flex gap-2">
          {abas.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                  isActive
                    ? 'border-teal-400 text-teal-400'
                    : 'border-transparent text-slate-400 hover:text-slate-200'
                }`
              }
            >
              <Icon size={16} /> {label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="max-w-6xl mx-auto px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
