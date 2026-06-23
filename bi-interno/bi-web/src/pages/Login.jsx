import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LogIn } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState(null);
  const [carregando, setCarregando] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      await login(email, senha);
      navigate('/financeira');
    } catch (err) {
      setErro('Email ou senha invalidos.');
    } finally {
      setCarregando(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-900">
      <form onSubmit={handleSubmit} className="bg-slate-800 p-8 rounded-xl shadow-lg w-full max-w-sm border border-slate-700">
        <div className="flex items-center gap-2 mb-6 text-teal-400">
          <LogIn size={24} />
          <h1 className="text-xl font-bold text-white">BI Interno</h1>
        </div>

        {erro && (
          <div className="bg-red-900/40 text-red-300 text-sm p-3 rounded mb-4 border border-red-800">
            {erro}
          </div>
        )}

        <label className="block text-sm text-slate-300 mb-1">Email</label>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full mb-4 px-3 py-2 rounded bg-slate-900 border border-slate-700 text-white focus:outline-none focus:border-teal-500"
          required
        />

        <label className="block text-sm text-slate-300 mb-1">Senha</label>
        <input
          type="password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          className="w-full mb-6 px-3 py-2 rounded bg-slate-900 border border-slate-700 text-white focus:outline-none focus:border-teal-500"
          required
        />

        <button
          type="submit"
          disabled={carregando}
          className="w-full bg-teal-600 hover:bg-teal-500 disabled:opacity-60 text-white font-semibold py-2 rounded transition-colors"
        >
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </div>
  );
}
