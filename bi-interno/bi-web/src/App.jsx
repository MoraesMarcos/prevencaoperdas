import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import Login from './pages/Login';
import Financeira from './pages/Financeira';
import Comercial from './pages/Comercial';
import PrevencaoPerdas from './pages/PrevencaoPerdas';

function RotaProtegida({ children }) {
  const { usuario } = useAuth();
  return usuario ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <RotaProtegida>
                <Layout />
              </RotaProtegida>
            }
          >
            <Route index element={<Navigate to="/financeira" replace />} />
            <Route path="financeira" element={<Financeira />} />
            <Route path="comercial" element={<Comercial />} />
            <Route path="prevencao-perdas" element={<PrevencaoPerdas />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
