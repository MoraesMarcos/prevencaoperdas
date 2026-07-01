import React, { useState } from 'react';
import { Package, Users, Building2, Images, TicketPercent, LogOut } from 'lucide-react';
import Estoque from './components/Estoque';
import ContaCorrente from './components/ContaCorrente';
import ContaMercado from './components/ContaMercado';
import EstoqueFotos from './components/EstoqueFotos';
import RebaixaParceria from './components/RebaixaParceria';
import Login from './pages/Login';
import { useAuth } from './services/AuthContext';

const ABAS = [
  { id: 'estoque', label: 'Validade & Giro', icon: Package },
  { id: 'fotos', label: 'Estoque (fotos)', icon: Images },
  { id: 'parceria', label: 'Rebaixa Parceria', icon: TicketPercent },
  { id: 'fornecedores', label: 'Conta Fornecedor', icon: Users },
  { id: 'mercado', label: 'Conta Mercado', icon: Building2 },
];

export default function App() {
  const [aba, setAba] = useState('estoque');
  const { usuario, carregando, logout } = useAuth();

  if (carregando) {
    return <div className="min-h-screen flex items-center justify-center text-gray-400">Carregando...</div>;
  }

  if (!usuario) {
    return <Login />;
  }

  return (
    <div className="min-h-screen text-gray-800 font-sans">
      <nav className="bg-white border-b border-gray-200 px-6">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex gap-2">
            {ABAS.map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setAba(id)}
                className={`flex items-center gap-2 px-4 py-4 text-sm font-semibold border-b-2 transition-colors ${
                  aba === id ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-800'
                }`}
              >
                <Icon size={18} /> {label}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-3 text-sm text-gray-500">
            <span>{usuario.email}</span>
            <button
              onClick={logout}
              title="Sair"
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg hover:bg-gray-100 text-gray-500 hover:text-red-600 font-medium"
            >
              <LogOut size={16} /> Sair
            </button>
          </div>
        </div>
      </nav>

      <div className="p-6">
        <div className="max-w-7xl mx-auto">
          {aba === 'estoque' && <Estoque />}
          {aba === 'fotos' && <EstoqueFotos />}
          {aba === 'parceria' && <RebaixaParceria />}
          {aba === 'fornecedores' && <ContaCorrente />}
          {aba === 'mercado' && <ContaMercado />}
        </div>
      </div>
    </div>
  );
}
