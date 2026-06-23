import React, { useState, useEffect } from 'react';
import { Package, AlertCircle, CheckCircle2, Clock, RefreshCw, AlertTriangle } from 'lucide-react';

export default function App() {
  const [lotes, setLotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);

  const carregarDados = async () => {
    setLoading(true);
    setErro(null);
    try {
      // Como o painel web corre no mesmo PC do backend, podemos usar localhost
      const response = await fetch('http://localhost:8082/api/capturas');
      if (!response.ok) throw new Error('Falha ao buscar dados');
      
      const data = await response.json();
      setLotes(data);
    } catch (err) {
      console.error(err);
      setErro('Não foi possível conectar ao Spring Boot. Verifique se ele está a correr na porta 8081.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, []);

  const getStatusColor = (status) => {
    switch (status) {
      case 'CRITICO': return 'bg-red-100 text-red-800 border-red-200';
      case 'ATENCAO': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'NORMAL': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'CRITICO': return <AlertCircle size={16} className="text-red-600" />;
      case 'ATENCAO': return <AlertTriangle size={16} className="text-yellow-600" />;
      case 'NORMAL': return <CheckCircle2 size={16} className="text-green-600" />;
      default: return <Clock size={16} />;
    }
  };

  return (
    <div className="min-h-screen text-gray-800 font-sans p-6">
      <div className="max-w-6xl mx-auto">
        
        <header className="flex justify-between items-center mb-8 bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
              <Package className="text-blue-600" size={32} />
              Dashboard Prevenção de Perdas
            </h1>
            <p className="text-gray-500 mt-1">Monitorização FEFO - First Expired, First Out</p>
          </div>
          
          <button 
            onClick={carregarDados}
            className="flex items-center gap-2 bg-blue-50 border border-blue-200 hover:bg-blue-100 text-blue-700 px-4 py-2 rounded-lg shadow-sm transition-all font-semibold"
          >
            <RefreshCw size={18} className={loading ? "animate-spin" : ""} />
            Atualizar Dados
          </button>
        </header>

        {erro && (
          <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6 rounded shadow-sm">
            <div className="flex items-center gap-2 text-red-700">
              <AlertCircle size={20} />
              <p className="font-medium">{erro}</p>
            </div>
          </div>
        )}

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 text-gray-600 text-xs uppercase tracking-wider border-b border-gray-200">
                  <th className="p-4 font-bold">Produto</th>
                  <th className="p-4 font-bold">Cód. Barras</th>
                  <th className="p-4 font-bold">Lote</th>
                  <th className="p-4 font-bold text-center">Volume</th>
                  <th className="p-4 font-bold">Vencimento</th>
                  <th className="p-4 font-bold">Ação Sugerida</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {loading && lotes.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="p-8 text-center text-gray-400">
                      <RefreshCw size={24} className="animate-spin mx-auto mb-2 text-blue-500" />
                      A conectar à base de dados...
                    </td>
                  </tr>
                ) : lotes.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="p-8 text-center text-gray-500 font-medium">
                      Tudo limpo! Não há lotes registados no momento.
                    </td>
                  </tr>
                ) : (
                  lotes.map((lote) => (
                    <tr key={lote.loteId} className="hover:bg-blue-50/30 transition-colors">
                      <td className="p-4 font-bold text-gray-900">{lote.produtoNome}</td>
                      <td className="p-4 text-gray-500 font-mono text-xs">{lote.codigoBarras}</td>
                      <td className="p-4 text-gray-600">{lote.numeroLote}</td>
                      <td className="p-4 text-center font-bold text-lg text-gray-700">{lote.quantidadeAtual}</td>
                      <td className="p-4">
                        <div className="flex flex-col">
                          <span className="font-medium">{new Date(lote.dataVencimento).toLocaleDateString('pt-PT')}</span>
                          <span className={`text-xs font-bold ${lote.diasParaVencer <= 30 ? 'text-red-500' : 'text-gray-500'}`}>
                            {lote.diasParaVencer > 0 ? `Faltam ${lote.diasParaVencer} dias` : 'VENCIDO!'}
                          </span>
                        </div>
                      </td>
                      <td className="p-4">
                        <span className={`flex items-center gap-1.5 w-fit px-3 py-1.5 rounded-full text-xs font-bold border shadow-sm ${getStatusColor(lote.status)}`}>
                          {getStatusIcon(lote.status)}
                          {lote.status === 'CRITICO' ? 'BAIXAR PREÇO' : lote.status}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  );
}
