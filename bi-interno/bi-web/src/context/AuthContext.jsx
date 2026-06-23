import { createContext, useContext, useState } from 'react';
import { api } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(() => {
    const salvo = localStorage.getItem('bi_usuario');
    return salvo ? JSON.parse(salvo) : null;
  });

  const login = async (email, senha) => {
    const resposta = await api.login(email, senha);
    localStorage.setItem('bi_token', resposta.token);
    const dadosUsuario = { nome: resposta.nome, email: resposta.email, papel: resposta.papel };
    localStorage.setItem('bi_usuario', JSON.stringify(dadosUsuario));
    setUsuario(dadosUsuario);
  };

  const logout = () => {
    localStorage.removeItem('bi_token');
    localStorage.removeItem('bi_usuario');
    setUsuario(null);
  };

  return (
    <AuthContext.Provider value={{ usuario, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
