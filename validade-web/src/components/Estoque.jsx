import React, { useState, useEffect } from 'react';
import { Package, AlertCircle, CheckCircle2, Clock, RefreshCw, AlertTriangle, TrendingUp, TicketPercent, PackageCheck, Users, X, Search } from 'lucide-react';
import { API_BASE } from '../config';

const API_FORN = `${API_BASE}/api/fornecedores/buscar`;
const brl = (v) => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export default function Estoque() {
  const [itens, setItens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);
  const [incluirNovos, setIncluirNovos] = useState(true); // teste: mostra lotes com < 30 dias
  const [gerando, setGerando] = useState(null); // loteId em processamento
  const [seletor, setSeletor] = useState(null); // item aguardando escolha de fornecedor

  // Faz a chamada de gerar-rebaixa; fornecedorId opcional. Retorna true se criou.
  const chamarGerar = async (loteId, fornecedorId) => {
    const r = await fetch(`${API_BASE}/api/acompanhamento/${loteId}/gerar-rebaixa`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(fornecedorId ? { fornecedorId } : {}),
    });
    const res = await r.json().catch(() => ({}));
    return { ok: r.ok, res };
  };

  const gerarRebaixa = async (item) => {
    setGerando(item.loteId);
    try {
      const { ok, res } = await chamarGerar(item.loteId, null);
      if (!ok) {
        const msg = res.message || res.mensagem || '';
        // Só mostra alerta se o erro NÃO for sobre fornecedor (ex.: lote não encontrado).
        // Qualquer outro caso (sem fornecedor, ou backend sem mensagem) → abre o seletor.
        if (msg && !/fornecedor/i.test(msg)) {
          alert(msg);
          return;
        }
        setSeletor(item);
        return;
      }
      alert(res.mensagem || 'Rebaixa criada.');
      await carregarDados();
    } catch {
      alert('Falha ao gerar rebaixa.');
    } finally {
      setGerando(null);
    }
  };

  const gerarComFornecedor = async (loteId, fornecedorId) => {
    setGerando(loteId);
    try {
      const { ok, res } = await chamarGerar(loteId, fornecedorId);
      alert(res.mensagem || res.message || (ok ? 'Rebaixa criada.' : 'Não foi possível.'));
      setSeletor(null);
      await carregarDados();
    } catch {
      alert('Falha ao gerar rebaixa.');
    } finally {
      setGerando(null);
    }
  };

  const carregarDados = async () => {
    setLoading(true);
    setErro(null);
    try {
      const response = await fetch(`${API_BASE}/api/acompanhamento?incluirNovos=${incluirNovos}`);
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
      case 'VENDIDO': return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      case 'CRITICO': return 'bg-red-100 text-red-800 border-red-200';
      case 'ATENCAO': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'OBSERVAR': return 'bg-gray-100 text-gray-700 border-gray-300';
      case 'OK': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getSeveridadeIcon = (sev) => {
    switch (sev) {
      case 'VENDIDO': return <PackageCheck size={16} className="text-emerald-600" />;
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
                itens.map((it) => {
                  const vendido = it.severidade === 'VENDIDO';
                  return (
                  <tr key={it.loteId} className={`transition-colors ${vendido ? 'bg-gray-50 opacity-60 line-through decoration-gray-400' : 'hover:bg-blue-50/30'}`}>
                    <td className="p-4 font-bold text-gray-900">{it.produtoNome}</td>
                    <td className="p-4 text-gray-500 text-sm">{it.grupo}</td>
                    <td className="p-4 text-gray-600">{it.numeroLote}</td>
                    <td className="p-4 text-center">
                      <div className="font-bold text-lg text-gray-900">{it.quantidadeRestante}</div>
                      <div className="text-xs text-gray-400 leading-tight">
                        {it.quantidadeAtual} inicial
                      </div>
                      {it.vendidoDesdeCaptura > 0 && (
                        <div className="text-xs text-emerald-600 font-medium leading-tight">
                          −{it.vendidoDesdeCaptura} vendidos
                        </div>
                      )}
                    </td>
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
                      {(it.status === 'CRITICO' || it.status === 'ATENCAO') && (
                        it.rebaixaGerada ? (
                          <span className="mt-2 flex items-center gap-1 text-xs text-emerald-600 font-semibold">
                            <CheckCircle2 size={14} /> Rebaixa gerada
                          </span>
                        ) : (
                          <button
                            onClick={() => gerarRebaixa(it)}
                            disabled={gerando === it.loteId}
                            className="mt-2 flex items-center gap-1 text-xs bg-amber-500 hover:bg-amber-400 disabled:opacity-60 text-white px-3 py-1.5 rounded-lg font-semibold"
                          >
                            <TicketPercent size={14} /> {gerando === it.loteId ? 'Gerando...' : 'Gerar rebaixa'}
                          </button>
                        )
                      )}
                    </td>
                  </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {seletor && (
        <SeletorFornecedor
          item={seletor}
          gerando={gerando === seletor.loteId}
          onFechar={() => setSeletor(null)}
          onEscolher={(fornId) => gerarComFornecedor(seletor.loteId, fornId)}
        />
      )}
    </>
  );
}

function SeletorFornecedor({ item, gerando, onFechar, onEscolher }) {
  const [termo, setTermo] = useState('');
  const [lista, setLista] = useState([]);
  const [buscando, setBuscando] = useState(false);

  const buscar = async (t) => {
    setBuscando(true);
    try {
      const r = await fetch(`${API_FORN}?termo=${encodeURIComponent(t)}`);
      setLista(r.ok ? await r.json() : []);
    } catch {
      setLista([]);
    } finally {
      setBuscando(false);
    }
  };

  useEffect(() => { buscar(''); }, []);

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={onFechar}>
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[80vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center p-5 border-b border-gray-200">
          <div>
            <h3 className="font-bold text-lg text-gray-900">Escolher fornecedor da rebaixa</h3>
            <p className="text-sm text-gray-500">
              {item.produtoNome} — lote {item.numeroLote} · {item.quantidadeAtual} un.
            </p>
            <p className="text-xs text-gray-500 mt-1">
              O custo é buscado no Uniplus (preço de custo do produto / última venda / última entrada).
              Só fica R$ 0,00 se o produto não existir no Uniplus (cadastro de teste).
            </p>
          </div>
          <button onClick={onFechar} className="text-gray-400 hover:text-gray-700"><X size={22} /></button>
        </div>

        <div className="p-5 border-b border-gray-100">
          <div className="flex items-center gap-2 bg-gray-50 border border-gray-200 rounded-lg px-3">
            <Search size={16} className="text-gray-400" />
            <input
              autoFocus
              value={termo}
              onChange={(e) => { setTermo(e.target.value); buscar(e.target.value); }}
              placeholder="Buscar fornecedor por nome ou CNPJ..."
              className="flex-1 bg-transparent py-2.5 outline-none text-sm"
            />
            {buscando && <RefreshCw size={14} className="animate-spin text-gray-400" />}
          </div>
        </div>

        <div className="overflow-y-auto flex-1">
          {lista.length === 0 ? (
            <p className="p-6 text-center text-gray-400 text-sm">Nenhum fornecedor encontrado.</p>
          ) : (
            lista.map((f) => (
              <button
                key={f.id}
                disabled={gerando}
                onClick={() => {
                  if (window.confirm(`Gerar rebaixa na conta de ${f.nome}?`)) onEscolher(f.id);
                }}
                className="w-full text-left px-5 py-3 hover:bg-blue-50 border-b border-gray-50 disabled:opacity-50 flex justify-between items-center"
              >
                <div>
                  <div className="font-semibold text-gray-800">{f.nome}</div>
                  <div className="text-xs text-gray-400">{f.cnpjcpf || '—'}</div>
                </div>
                <Users size={16} className="text-blue-500" />
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
