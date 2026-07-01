import React, { useState } from 'react';
import { LogIn, AlertCircle, Package } from 'lucide-react';
import { useAuth } from '../services/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState(null);
  const [entrando, setEntrando] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErro(null);
    setEntrando(true);
    const { error } = await login(email.trim(), senha);
    if (error) {
      setErro('E-mail ou senha inválidos.');
    }
    setEntrando(false);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <form onSubmit={handleSubmit} className="w-full max-w-sm bg-white rounded-xl shadow-sm border border-gray-200 p-8">
        <div className="flex flex-col items-center mb-6">
          <div className="bg-blue-50 p-3 rounded-xl mb-3">
            <Package className="text-blue-600" size={28} />
          </div>
          <h1 className="text-xl font-bold text-gray-900">Prevenção de Perdas</h1>
          <p className="text-sm text-gray-500">Entre para acessar o painel</p>
        </div>

        {erro && (
          <div className="bg-red-50 border-l-4 border-red-500 p-3 mb-4 rounded flex items-center gap-2 text-red-700 text-sm">
            <AlertCircle size={18} /> {erro}
          </div>
        )}

        <label className="text-xs font-bold text-gray-500 uppercase mb-1 block">E-mail</label>
        <input
          type="email"
          required
          autoFocus
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full border border-gray-300 rounded-lg px-3 py-2.5 mb-4 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="seuemail@mercadao.com"
        />

        <label className="text-xs font-bold text-gray-500 uppercase mb-1 block">Senha</label>
        <input
          type="password"
          required
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          className="w-full border border-gray-300 rounded-lg px-3 py-2.5 mb-6 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="••••••••"
        />

        <button
          type="submit"
          disabled={entrando}
          className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-500 disabled:opacity-60 text-white font-semibold py-2.5 rounded-lg transition-colors"
        >
          <LogIn size={18} /> {entrando ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </div>
  );
}
