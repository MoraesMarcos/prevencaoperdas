import React, { useState, useEffect } from 'react';
import { Building2, RefreshCw, Plus, X, Trash2, Gift, TrendingDown } from 'lucide-react';

const BASE = 'http://localhost:8082/api';
const USOS = ['AVARIA_LOJA', 'TROCA_PRECO', 'OUTRO'];
const moeda = (v) => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

function primeiroDiaAno() { return new Date(new Date().getFullYear(), 0, 1).toISOString().slice(0, 10); }
function hojeISO() { return new Date().toISOString().slice(0, 10); }

export default function ContaMercado() {
  const [conta, setConta] = useState(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);
  const [inicio, setInicio] = useState(primeiroDiaAno());
  const [fim, setFim] = useState(hojeISO());
  const [modal, setModal] = useState(false);

  const carregar = async () => {
    setLoading(true); setErro(null);
    try {
      const r = await fetch(`${BASE}/conta-mercado?inicio=${inicio}&fim=${fim}`);
      if (!r.ok) throw new Error();
      setConta(await r.json());
    } catch { setErro('Não foi possível carregar. O backend está na porta 8082?'); }
    finally { setLoading(false); }
  };

  useEffect(() => { carregar(); }, [inicio, fim]);

  const remover = async (id) => {
    await fetch(`${BASE}/conta-mercado/lancamento/${id}`, { method: 'DELETE' });
    carregar();
  };

  return (
    <div>
      <header className="flex flex-wrap justify-between items-center gap-4 mb-8 bg-white p-6 rounded-xl shadow-sm border border-gray-200">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <Building2 className="text-blue-600" size={32} /> Conta Corrente do Mercado
          </h1>
          <p className="text-gray-500 mt-1">Crédito de bonificações (1910) − avarias da loja e trocas de preço</p>
        </div>
        <div className="flex items-end gap-2">
          <div>
            <label className="block text-xs text-gray-500 mb-1">Início</label>
            <input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} className="border rounded-lg px-3 py-2" />
          </div>
          <div>
            <label className="block text-xs text-gray-500 mb-1">Fim</label>
            <input type="date" value={fim} onChange={(e) => setFim(e.target.value)} className="border rounded-lg px-3 py-2" />
          </div>
          <button onClick={() => setModal(true)} className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg font-semibold">
            <Plus size={18} /> Lançamento
          </button>
          <button onClick={carregar} className="flex items-center gap-2 bg-blue-50 border border-blue-200 hover:bg-blue-100 text-blue-700 px-4 py-2 rounded-lg font-semibold">
            <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </header>

      {erro && <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6 rounded text-red-700">{erro}</div>}

      {conta && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <Card titulo="Crédito (Bonificações 1910)" valor={conta.totalBonificacoes} cor="text-emerald-600" icon={<Gift size={20} className="text-emerald-500" />} />
            <Card titulo="Usos (avaria / troca preço)" valor={conta.totalUsos} cor="text-red-600" icon={<TrendingDown size={20} className="text-red-500" />} />
            <Card titulo="Saldo disponível" valor={conta.saldo} cor="text-blue-700" destaque />
          </div>

          {/* Lançamentos manuais */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 mb-6 overflow-hidden">
            <h3 className="px-4 py-3 font-bold text-gray-700 bg-gray-50 border-b">Lançamentos do mercado (manuais)</h3>
            {conta.lancamentos.length === 0 ? (
              <p className="p-4 text-gray-400 text-sm">Nenhum lançamento manual.</p>
            ) : (
              <table className="w-full text-left text-sm">
                <tbody className="divide-y divide-gray-100">
                  {conta.lancamentos.map((l) => (
                    <tr key={l.id}>
                      <td className="p-3 text-gray-500">{l.data}</td>
                      <td className="p-3">
                        <span className={`px-2 py-0.5 rounded text-xs font-bold ${l.sinal > 0 ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>{l.tipo}</span>
                      </td>
                      <td className="p-3 text-gray-600">{l.descricao || '—'}</td>
                      <td className={`p-3 font-bold ${l.sinal > 0 ? 'text-emerald-600' : 'text-red-600'}`}>{l.sinal > 0 ? '+' : '−'} {moeda(l.valor)}</td>
                      <td className="p-3 text-right"><button onClick={() => remover(l.id)} className="text-gray-400 hover:text-red-500"><Trash2 size={16} /></button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* Bonificações do Uniplus */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <h3 className="px-4 py-3 font-bold text-gray-700 bg-gray-50 border-b">Bonificações recebidas — Uniplus (CFOP 1910/1910a)</h3>
            <div className="max-h-96 overflow-y-auto">
              <table className="w-full text-left text-sm">
                <thead className="sticky top-0 bg-gray-50">
                  <tr className="text-gray-500 text-xs uppercase border-b">
                    <th className="p-3">Data</th><th className="p-3">Fornecedor</th><th className="p-3">Produto</th>
                    <th className="p-3 text-center">Qtd</th><th className="p-3 text-right">Crédito</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {conta.bonificacoes.length === 0 ? (
                    <tr><td colSpan="5" className="p-6 text-center text-gray-400">Nenhuma bonificação no período.</td></tr>
                  ) : conta.bonificacoes.map((b, i) => (
                    <tr key={i} className="hover:bg-emerald-50/40">
                      <td className="p-3 text-gray-500">{new Date(b.data).toLocaleDateString('pt-BR')}</td>
                      <td className="p-3 text-gray-700">{b.fornecedor}</td>
                      <td className="p-3 text-gray-600">{b.produto}</td>
                      <td className="p-3 text-center">{Number(b.quantidade)}</td>
                      <td className="p-3 text-right font-semibold text-emerald-600">{moeda(b.valor)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {modal && <ModalLancamento onFechar={() => setModal(false)} onSalvo={() => { setModal(false); carregar(); }} />}
    </div>
  );
}

function Card({ titulo, valor, cor, destaque, icon }) {
  return (
    <div className={`rounded-xl border p-5 ${destaque ? 'bg-blue-50 border-blue-200' : 'bg-white border-gray-200'}`}>
      <div className="flex items-center gap-2 mb-2">{icon}<p className="text-xs uppercase text-gray-400">{titulo}</p></div>
      <p className={`text-2xl font-bold ${cor}`}>{moeda(valor)}</p>
    </div>
  );
}

function ModalLancamento({ onFechar, onSalvo }) {
  const [tipo, setTipo] = useState('AVARIA_LOJA');
  const [valor, setValor] = useState('');
  const [descricao, setDescricao] = useState('');
  const [data, setData] = useState(hojeISO());

  const salvar = async () => {
    if (!valor) { alert('Informe o valor.'); return; }
    await fetch(`${BASE}/conta-mercado/lancamento`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tipo, valor: parseFloat(valor), descricao, data }),
    });
    onSalvo();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl w-full max-w-md p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-bold text-gray-900">Lançamento do mercado</h3>
          <button onClick={onFechar}><X size={22} className="text-gray-400" /></button>
        </div>
        <div className="mb-3">
          <label className="block text-sm font-semibold text-gray-600 mb-1">Tipo</label>
          <select value={tipo} onChange={(e) => setTipo(e.target.value)} className="w-full border rounded-lg px-3 py-2">
            <option value="CREDITO_MANUAL">CRÉDITO MANUAL (+)</option>
            {USOS.map(t => <option key={t} value={t}>{t} (−)</option>)}
          </select>
        </div>
        <div className="grid grid-cols-2 gap-3 mb-3">
          <div>
            <label className="block text-sm font-semibold text-gray-600 mb-1">Valor (R$)</label>
            <input type="number" value={valor} onChange={(e) => setValor(e.target.value)} className="w-full border rounded-lg px-3 py-2" placeholder="0,00" />
          </div>
          <div>
            <label className="block text-sm font-semibold text-gray-600 mb-1">Data</label>
            <input type="date" value={data} onChange={(e) => setData(e.target.value)} className="w-full border rounded-lg px-3 py-2" />
          </div>
        </div>
        <div className="mb-5">
          <label className="block text-sm font-semibold text-gray-600 mb-1">Descrição</label>
          <input value={descricao} onChange={(e) => setDescricao(e.target.value)} className="w-full border rounded-lg px-3 py-2" placeholder="Ex: avaria gôndola, rebaixa de preço..." />
        </div>
        <div className="flex justify-end gap-3">
          <button onClick={onFechar} className="px-4 py-2 text-gray-600 font-semibold">Cancelar</button>
          <button onClick={salvar} className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2 rounded-lg font-semibold">Salvar</button>
        </div>
      </div>
    </div>
  );
}
