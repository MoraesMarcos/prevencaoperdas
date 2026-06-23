import { useEffect, useState } from 'react';
import { RefreshCw, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { api } from '../services/api';
import CardResultado from '../components/CardResultado';

function primeiroDiaDoMes() {
  const hoje = new Date();
  return new Date(hoje.getFullYear(), hoje.getMonth(), 1).toISOString().slice(0, 10);
}
function hojeISO() {
  return new Date().toISOString().slice(0, 10);
}

export default function Financeira() {
  const [dataInicio, setDataInicio] = useState(primeiroDiaDoMes());
  const [dataFim, setDataFim] = useState(hojeISO());
  const [resultado, setResultado] = useState(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);

  const carregar = async () => {
    setCarregando(true);
    setErro(null);
    try {
      const dados = await api.resultadoFinanceiro(dataInicio, dataFim);
      setResultado(dados);
    } catch (err) {
      setErro(err.message || 'Falha ao carregar dados financeiros.');
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => { carregar(); }, []);

  const acimaDoIdeal = resultado && resultado.percentualDespesasSobreFaturamento > resultado.limiteIdealPercentual;
  const acimaDoTeto = resultado && resultado.percentualDespesasSobreFaturamento > resultado.tetoPercentual;

  return (
    <div>
      <div className="flex flex-wrap items-end gap-3 mb-6">
        <div>
          <label className="block text-xs text-slate-400 mb-1">Data início</label>
          <input type="date" value={dataInicio} onChange={(e) => setDataInicio(e.target.value)}
            className="bg-slate-800 border border-slate-700 rounded px-3 py-2 text-sm" />
        </div>
        <div>
          <label className="block text-xs text-slate-400 mb-1">Data fim</label>
          <input type="date" value={dataFim} onChange={(e) => setDataFim(e.target.value)}
            className="bg-slate-800 border border-slate-700 rounded px-3 py-2 text-sm" />
        </div>
        <button onClick={carregar} className="flex items-center gap-2 bg-teal-700 hover:bg-teal-600 px-4 py-2 rounded text-sm font-medium transition-colors">
          <RefreshCw size={16} className={carregando ? 'animate-spin' : ''} /> Atualizar
        </button>
      </div>

      {erro && <div className="bg-red-900/40 border border-red-800 text-red-300 p-4 rounded mb-6">{erro}</div>}

      {resultado && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <CardResultado titulo="Faturamento Líquido" valor={resultado.faturamentoLiquido} />
            <CardResultado titulo="CMV Real (custo congelado na venda)" valor={resultado.cmvReal} />
            <CardResultado titulo="Resultado Bruto" valor={resultado.resultadoBruto} destaque />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <CardResultado titulo="Total Despesas (período)" valor={resultado.totalDespesas} />
            <CardResultado titulo="Resultado Líquido" valor={resultado.resultadoLiquido} destaque />
            <CardResultado titulo="Despesas / Faturamento" valor={resultado.percentualDespesasSobreFaturamento} sufixo="%" />
          </div>

          <div className={`flex items-center gap-2 p-4 rounded-lg border text-sm ${
            acimaDoTeto ? 'bg-red-900/30 border-red-800 text-red-300'
            : acimaDoIdeal ? 'bg-amber-900/30 border-amber-800 text-amber-300'
            : 'bg-emerald-900/30 border-emerald-800 text-emerald-300'
          }`}>
            {acimaDoTeto || acimaDoIdeal ? <AlertTriangle size={18} /> : <CheckCircle2 size={18} />}
            Despesas em {resultado.percentualDespesasSobreFaturamento}% do faturamento — limite ideal {resultado.limiteIdealPercentual}%, teto {resultado.tetoPercentual}%.
          </div>
        </>
      )}
    </div>
  );
}
