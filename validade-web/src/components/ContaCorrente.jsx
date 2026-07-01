import React, { useState, useEffect } from 'react';
import { Users, RefreshCw, Plus, Check, X, ArrowLeft, Download, Printer, Search, Gift } from 'lucide-react';
import { API_BASE } from '../config';

const BASE = `${API_BASE}/api`;
const TIPOS = ['TROCA', 'REBAIXA', 'AVARIA', 'NEGOCIACAO'];
const moeda = (v) => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export default function ContaCorrente() {
  const [fornecedores, setFornecedores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);
  const [detalhe, setDetalhe] = useState(null);
  const [modalAbrir, setModalAbrir] = useState(false);

  const carregar = async () => {
    setLoading(true); setErro(null);
    try {
      const r = await fetch(`${BASE}/conta-corrente`);
      if (!r.ok) throw new Error();
      setFornecedores(await r.json());
    } catch { setErro('Não foi possível carregar. O backend está na porta 8082?'); }
    finally { setLoading(false); }
  };

  useEffect(() => { carregar(); }, []);

  const abrirFornecedor = async (fornecedorId, nome) => {
    const [ledgerResp, prodResp] = await Promise.all([
      fetch(`${BASE}/conta-corrente/${fornecedorId}`),
      fetch(`${BASE}/fornecedores/${fornecedorId}/produtos`),
    ]);
    const ledger = await ledgerResp.json();
    const produtos = await prodResp.json();
    setDetalhe({ ...ledger, fornecedorId, fornecedorNome: ledger.fornecedorNome || nome, produtos });
    setModalAbrir(false);
  };

  const recarregarDetalhe = () => abrirFornecedor(detalhe.fornecedorId, detalhe.fornecedorNome);

  if (detalhe) {
    return <Detalhe detalhe={detalhe} onVoltar={() => { setDetalhe(null); carregar(); }} recarregar={recarregarDetalhe} />;
  }

  return (
    <div>
      <header className="flex justify-between items-center mb-8 bg-white p-6 rounded-xl shadow-sm border border-gray-200">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <Users className="text-blue-600" size={32} /> Conta Corrente — Fornecedores
          </h1>
          <p className="text-gray-500 mt-1">Trocas, rebaixas, avarias e negociações por fornecedor</p>
        </div>
        <div className="flex gap-3">
          <button onClick={() => setModalAbrir(true)} className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg font-semibold">
            <Plus size={18} /> Abrir fornecedor
          </button>
          <button onClick={carregar} className="flex items-center gap-2 bg-blue-50 border border-blue-200 hover:bg-blue-100 text-blue-700 px-4 py-2 rounded-lg font-semibold">
            <RefreshCw size={18} className={loading ? 'animate-spin' : ''} /> Atualizar
          </button>
        </div>
      </header>

      {erro && <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6 rounded text-red-700">{erro}</div>}

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="w-full text-left">
          <thead>
            <tr className="bg-gray-50 text-gray-600 text-xs uppercase tracking-wider border-b border-gray-200">
              <th className="p-4 font-bold">Fornecedor</th>
              <th className="p-4 font-bold text-center">Itens em aberto</th>
              <th className="p-4 font-bold text-right">Saldo a acertar</th>
              <th className="p-4 font-bold text-center">Ação</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {fornecedores.length === 0 ? (
              <tr><td colSpan="4" className="p-8 text-center text-gray-500">Nenhum fornecedor com conta. Use "Abrir fornecedor".</td></tr>
            ) : fornecedores.map((f) => (
              <tr key={f.fornecedorId} className="hover:bg-blue-50/30">
                <td className="p-4 font-bold text-gray-900">{f.fornecedorNome}</td>
                <td className="p-4 text-center">{f.trocasAtivas}</td>
                <td className="p-4 text-right font-bold text-red-600">{moeda(f.saldoAtivo)}</td>
                <td className="p-4 text-center">
                  <button onClick={() => abrirFornecedor(f.fornecedorId, f.fornecedorNome)} className="text-blue-600 font-semibold">Abrir</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAbrir && <ModalAbrir onFechar={() => setModalAbrir(false)} onEscolher={abrirFornecedor} />}
    </div>
  );
}

function Detalhe({ detalhe, onVoltar, recarregar }) {
  const [filtro, setFiltro] = useState('');
  const [sel, setSel] = useState(null); // produto selecionado para adicionar
  const [tipo, setTipo] = useState('TROCA');
  const [qtd, setQtd] = useState('1');
  const [valor, setValor] = useState('');
  const [valorAbater, setValorAbater] = useState('');

  const selecionar = (p) => {
    setSel(p); setTipo('TROCA'); setQtd('1');
    setValor(p.custoUnitario != null ? String(p.custoUnitario) : '');
  };

  const adicionar = async () => {
    const valorFinal = parseFloat(valor) * (parseFloat(qtd) || 1);
    await fetch(`${BASE}/conta-corrente/lancamento`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fornecedorId: detalhe.fornecedorId, tipo, valor: valorFinal,
        ean: sel.ean, produtoNome: sel.nome, descricao: `${qtd}x ${sel.nome}`, criadoPor: 'Web',
      }),
    });
    setSel(null); recarregar();
  };

  const finalizar = async (id) => { await fetch(`${BASE}/conta-corrente/lancamento/${id}/finalizar`, { method: 'POST' }); recarregar(); };
  const remover = async (id) => { await fetch(`${BASE}/conta-corrente/lancamento/${id}`, { method: 'DELETE' }); recarregar(); };

  const abater = async () => {
    if (!valorAbater) return;
    await fetch(`${BASE}/conta-corrente/${detalhe.fornecedorId}/abater`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ valor: parseFloat(valorAbater) }),
    });
    setValorAbater(''); recarregar();
  };

  const abaterBonificacao = async () => {
    const disp = Number(detalhe.bonificacaoDisponivel || 0);
    if (disp <= 0) { alert('Sem crédito de bonificação disponível.'); return; }
    if (!window.confirm(`Usar até ${moeda(disp)} de bonificação para abater as trocas em aberto (FIFO)?`)) return;
    await fetch(`${BASE}/conta-corrente/${detalhe.fornecedorId}/abater-bonificacao`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}), // sem valor = usa todo o crédito disponível até cobrir as trocas
    });
    recarregar();
  };

  const produtosFiltrados = (detalhe.produtos || []).filter(p =>
    p.nome.toUpperCase().includes(filtro.toUpperCase()));

  return (
    <div>
      <button onClick={onVoltar} className="flex items-center gap-1 text-blue-600 mb-4 font-semibold print:hidden">
        <ArrowLeft size={18} /> Voltar
      </button>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
        <div className="flex flex-wrap justify-between items-start gap-4">
          <h2 className="text-2xl font-bold text-gray-900">{detalhe.fornecedorNome}</h2>
          <div className="flex gap-8">
            <div className="text-right">
              <p className="text-xs text-gray-400 uppercase">Crédito bonificação</p>
              <p className="text-2xl font-bold text-emerald-600">{moeda(detalhe.bonificacaoDisponivel)}</p>
            </div>
            <div className="text-right">
              <p className="text-xs text-gray-400 uppercase">Saldo a acertar</p>
              <p className="text-3xl font-bold text-red-600">{moeda(detalhe.saldoAtivo)}</p>
            </div>
          </div>
        </div>
        <div className="flex flex-wrap gap-3 mt-4 print:hidden">
          <a href={`${BASE}/conta-corrente/${detalhe.fornecedorId}/historico.csv`}
             className="flex items-center gap-2 bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-lg font-semibold">
            <Download size={18} /> Baixar CSV
          </a>
          <button onClick={() => window.print()} className="flex items-center gap-2 bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-lg font-semibold">
            <Printer size={18} /> Imprimir / PDF
          </button>
          <button onClick={abaterBonificacao} disabled={Number(detalhe.bonificacaoDisponivel || 0) <= 0}
            className="flex items-center gap-2 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white px-4 py-2 rounded-lg font-semibold">
            <Gift size={18} /> Abater com bonificação
          </button>
          <div className="flex items-center gap-2 ml-auto">
            <input type="number" value={valorAbater} onChange={(e) => setValorAbater(e.target.value)}
              placeholder="Valor a abater" className="border rounded-lg px-3 py-2 w-40" />
            <button onClick={abater} className="bg-green-600 hover:bg-green-500 text-white px-4 py-2 rounded-lg font-semibold">Abater (FIFO)</button>
          </div>
        </div>
      </div>

      {/* Produtos do fornecedor (Uniplus) */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 mb-6 overflow-hidden print:hidden">
        <div className="px-4 py-3 bg-gray-50 border-b flex items-center justify-between">
          <h3 className="font-bold text-gray-700">Produtos do fornecedor ({produtosFiltrados.length})</h3>
          <div className="flex items-center gap-2 bg-white border rounded-lg px-2">
            <Search size={16} className="text-gray-400" />
            <input value={filtro} onChange={(e) => setFiltro(e.target.value)} placeholder="Filtrar produto..."
              className="py-1.5 outline-none text-sm" />
          </div>
        </div>

        {sel && (
          <div className="p-4 bg-blue-50 border-b flex flex-wrap items-end gap-3">
            <div className="flex-1 min-w-[200px]">
              <p className="text-xs text-gray-500">Produto</p>
              <p className="font-bold text-gray-900">{sel.nome}</p>
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">Tipo</label>
              <select value={tipo} onChange={(e) => setTipo(e.target.value)} className="border rounded-lg px-3 py-2">
                {TIPOS.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">Qtd</label>
              <input type="number" value={qtd} onChange={(e) => setQtd(e.target.value)} className="border rounded-lg px-3 py-2 w-20" />
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">Valor unit.</label>
              <input type="number" value={valor} onChange={(e) => setValor(e.target.value)} className="border rounded-lg px-3 py-2 w-28" />
            </div>
            <div className="text-sm text-gray-600">
              Total: <b>{moeda((parseFloat(valor) || 0) * (parseFloat(qtd) || 1))}</b>
            </div>
            <button onClick={adicionar} className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg font-semibold">Adicionar</button>
            <button onClick={() => setSel(null)} className="text-gray-500 px-2">cancelar</button>
          </div>
        )}

        <div className="max-h-80 overflow-y-auto">
          <table className="w-full text-left text-sm">
            <tbody className="divide-y divide-gray-100">
              {produtosFiltrados.slice(0, 200).map((p) => (
                <tr key={p.produtoId} className="hover:bg-blue-50/40 cursor-pointer" onClick={() => selecionar(p)}>
                  <td className="px-4 py-2 font-medium text-gray-800">{p.nome}</td>
                  <td className="px-4 py-2 text-gray-400 text-xs">{p.grupo}</td>
                  <td className="px-4 py-2 text-right font-semibold text-gray-700">{moeda(p.custoUnitario)}</td>
                  <td className="px-4 py-2 text-right"><Plus size={16} className="text-blue-500 inline" /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Secao titulo="Em aberto" itens={detalhe.ativas} onFinalizar={finalizar} onRemover={remover} ativo />
      <Secao titulo="Histórico (finalizados)" itens={detalhe.finalizadas} ativo={false} />
    </div>
  );
}

function Secao({ titulo, itens, onFinalizar, onRemover, ativo }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 mb-6 overflow-hidden">
      <h3 className="px-4 py-3 font-bold text-gray-700 bg-gray-50 border-b">{titulo}</h3>
      {(!itens || itens.length === 0) ? (
        <p className="p-4 text-gray-400 text-sm">Nada por aqui.</p>
      ) : (
        <table className="w-full text-left text-sm">
          <tbody className="divide-y divide-gray-100">
            {itens.map((l) => (
              <tr key={l.id}>
                <td className="p-3 text-xs text-gray-500 whitespace-nowrap">
                  {l.criadoEm ? new Date(l.criadoEm).toLocaleDateString('pt-BR') : '—'}
                </td>
                <td className="p-3">
                  <span className={`px-2 py-0.5 rounded text-xs font-bold ${l.sinal > 0 ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>{l.tipo}</span>
                </td>
                <td className="p-3 text-gray-600">{l.produtoNome || l.descricao || '—'}</td>
                <td className="p-3 font-bold text-gray-700">{moeda(l.valor)}</td>
                <td className="p-3 text-xs text-gray-500">
                  {l.valorAbatido > 0 && `abatido ${moeda(l.valorAbatido)} · falta ${moeda(l.restante)}`}
                </td>
                {ativo && (
                  <td className="p-3 text-right print:hidden">
                    <div className="flex gap-2 justify-end">
                      <button onClick={() => onFinalizar(l.id)} title="Finalizar" className="text-green-600"><Check size={18} /></button>
                      <button onClick={() => onRemover(l.id)} title="Remover" className="text-gray-400 hover:text-red-500"><X size={18} /></button>
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function ModalAbrir({ onFechar, onEscolher }) {
  const [busca, setBusca] = useState('');
  const [resultados, setResultados] = useState([]);

  const buscar = async () => {
    const r = await fetch(`${BASE}/fornecedores/buscar?termo=${encodeURIComponent(busca)}`);
    setResultados(await r.json());
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl w-full max-w-lg p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-bold text-gray-900">Abrir fornecedor</h3>
          <button onClick={onFechar}><X size={22} className="text-gray-400" /></button>
        </div>
        <div className="flex gap-2 mb-2">
          <input value={busca} onChange={(e) => setBusca(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && buscar()}
            className="flex-1 border rounded-lg px-3 py-2" placeholder="Nome ou CNPJ (ex: NESTLE)" autoFocus />
          <button onClick={buscar} className="bg-blue-600 text-white px-4 rounded-lg font-semibold">Buscar</button>
        </div>
        <div className="max-h-72 overflow-y-auto">
          {resultados.map((r) => (
            <button key={r.id} onClick={() => onEscolher(r.id, r.nome)}
              className="block w-full text-left px-3 py-2 hover:bg-gray-100 rounded">
              <span className="font-medium">{r.nome}</span>
              <span className="text-xs text-gray-400 ml-2">{r.cnpjcpf}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
