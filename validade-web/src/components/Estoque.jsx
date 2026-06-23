import React, { useState, useEffect } from 'react';
import { Package, AlertCircle, CheckCircle2, Clock, RefreshCw, AlertTriangle, TrendingUp } from 'lucide-react';

export default function Estoque() {
  const [itens, setItens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);
  const [incluirNovos, setIncluirNovos] = useState(true); // teste: mostra lotes com < 30 dias

  const carregarDados = async () => {
    setLoading(true);
    setErro(null);
    try {
      const response = await fetch(`http://localhost:8082/api/acompanhamento?incluirNovos=${incluirNovos}`);
      if (!response.ok) throw new Error('Falha ao buscar dados');
      setItens(await response.json());
    } catch (err) {
      console.error(err);
      setErro('Não foi possível conectar ao Spring Boot. Verifique se ele está a correr na porta 8082.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { carregarDados(); }, [incluirNovos]);

  const getSeveridadeColor = (sev) => {
    switch (sev) {
      case 'CRITICO': return 'bg-red-100 text-red-800 border-red-200';
      case 'ATENCAO': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'OBSERVAR': return 'bg-gray-100 text-gray-700 border-gray-300';
      case 'OK': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getSeveridadeIcon = (sev) => {
    switch (sev) {
      case 'CRITICO': return <AlertCircle size={16} className="text-red-600" />;
      case 'ATENCAO': return <AlertTriangle size={16} className="text-yellow-600" />;
      case 'OBSERVAR': return <Clock size={16} className="text-gray-500" />;
      case 'OK': return <CheckCircle2 size={16} className="text-green-600" />;
      default: return <Clock size={16} />;
    }
  };

  return (
    <>
      <header className="flex justify-between items-center mb-8 bg-white p-6 rounded-xl shadow-sm border border-gray-200">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <Package className="text-blue-600" size={32} />
            Validade & Giro
          </h1>
          <p className="text-gray-500 mt-1">Validade (FEFO) + giro real de vendas (Uniplus)</p>
        </div>

        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={incluirNovos}
              onChange={(e) => setIncluirNovos(e.target.checked)}
              className="w-4 h-4"
            />
            Incluir recém-cadastrados (&lt; 30 dias)
          </label>
          <button
            onClick={carregarDados}
            className="flex items-center gap-2 bg-blue-50 border border-blue-200 hover:bg-blue-100 text-blue-700 px-4 py-2 rounded-lg shadow-sm transition-all font-semibold"
          >
            <RefreshCw size={18} className={loading ? "animate-spin" : ""} />
            Atualizar Dados
          </button>
        </div>
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
                <th className="p-4 font-bold">Grupo</th>
                <th className="p-4 font-bold">Lote</th>
                <th className="p-4 font-bold text-center">Qtd</th>
                <th className="p-4 font-bold">Vencimento</th>
                <th className="p-4 font-bold text-center">Giro (30d / 90d)</th>
                <th className="p-4 font-bold text-center">Esgota em</th>
                <th className="p-4 font-bold">Recomendação</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading && itens.length === 0 ? (
                <tr>
                  <td colSpan="8" className="p-8 text-center text-gray-400">
                    <RefreshCw size={24} className="animate-spin mx-auto mb-2 text-blue-500" />
                    A conectar à base de dados...
                  </td>
                </tr>
              ) : itens.length === 0 ? (
                <tr>
                  <td colSpan="8" className="p-8 text-center text-gray-500 font-medium">
                    Tudo limpo! Não há lotes registados no momento.
                  </td>
                </tr>
              ) : (
                itens.map((it) => (
                  <tr key={it.loteId} className="hover:bg-blue-50/30 transition-colors">
                    <td className="p-4 font-bold text-gray-900">{it.produtoNome}</td>
                    <td className="p-4 text-gray-500 text-sm">{it.grupo}</td>
                    <td className="p-4 text-gray-600">{it.numeroLote}</td>
                    <td className="p-4 text-center font-bold text-lg text-gray-700">{it.quantidadeAtual}</td>
                    <td className="p-4">
                      <div className="flex flex-col">
                        <span className="font-medium">{new Date(it.dataVencimento).toLocaleDateString('pt-BR')}</span>
                        <span className={`text-xs font-bold ${it.diasParaVencer <= 30 ? 'text-red-500' : 'text-gray-500'}`}>
                          {it.diasParaVencer > 0 ? `Faltam ${it.diasParaVencer} dias` : 'VENCIDO!'}
                        </span>
                      </div>
                    </td>
                    <td className="p-4 text-center">
                      <div className="flex items-center justify-center gap-1 text-sm">
                        <TrendingUp size={14} className="text-blue-500" />
                        <span className="font-semibold">{it.velocidade30}/d</span>
                        <span className="text-gray-400">·</span>
                        <span className="text-gray-500">{it.velocidade90}/d</span>
                      </div>
                      <div className="text-xs text-gray-400 mt-0.5">{it.vendido30d} / {it.vendido90d} un.</div>
                    </td>
                    <td className="p-4 text-center font-bold text-gray-700">
                      {it.diasParaEsgotar == null ? '—' : `${it.diasParaEsgotar} d`}
                    </td>
                    <td className="p-4">
                      <span className={`flex items-center gap-1.5 w-fit px-3 py-1.5 rounded-full text-xs font-bold border shadow-sm ${getSeveridadeColor(it.severidade)}`}>
                        {getSeveridadeIcon(it.severidade)}
                        {it.recomendacao}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
