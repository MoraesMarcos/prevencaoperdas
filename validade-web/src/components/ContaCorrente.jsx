import React, { useState, useEffect } from 'react';
import { Users, RefreshCw, Plus, MessageCircle, Check, X, ArrowLeft } from 'lucide-react';

const BASE = 'http://localhost:8082/api';

const TIPOS = ['TROCA', 'REBAIXA', 'AVARIA', 'NEGOCIACAO', 'PAGAMENTO'];
const moeda = (v) => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export default function ContaCorrente() {
  const [fornecedores, setFornecedores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);
  const [detalhe, setDetalhe] = useState(null); // fornecedor selecionado
  const [modalAberto, setModalAberto] = useState(false);

  const carregar = async () => {
    setLoading(true);
    setErro(null);
    try {
      const r = await fetch(`${BASE}/conta-corrente`);
      if (!r.ok) throw new Error();
      setFornecedores(await r.json());
    } catch {
      setErro('Não foi possível carregar. O backend está na porta 8082?');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { carregar(); }, []);

  const abrirDetalhe = async (fornecedorId) => {
    const r = await fetch(`${BASE}/conta-corrente/${fornecedorId}`);
    setDetalhe(await r.json());
  };

  const finalizar = async (id) => {
    await fetch(`${BASE}/conta-corrente/lancamento/${id}/finalizar`, { method: 'POST' });
    abrirDetalhe(detalhe.fornecedorId);
    carregar();
  };

  const remover = async (id) => {
    await fetch(`${BASE}/conta-corrente/lancamento/${id}`, { method: 'DELETE' });
    abrirDetalhe(detalhe.fornecedorId);
    carregar();
  };

  const enviarWhatsapp = (forn) => {
    const numero = (forn.whatsapp || '').replace(/\D/g, '');
    if (!numero) { alert('Fornecedor sem WhatsApp cadastrado no Uniplus.'); return; }
    const num = numero.startsWith('55') ? numero : `55${numero}`;
    const texto = `Olá ${forn.fornecedorNome}, sobre a conta corrente: saldo atual de ${moeda(forn.saldoAtivo)} a acertar (trocas/rebaixas/negociação).`;
    window.open(`https://wa.me/${num}?text=${encodeURIComponent(texto)}`, '_blank');
  };

  // ---------- DETALHE ----------
  if (detalhe) {
    return (
      <div>
        <button onClick={() => setDetalhe(null)} className="flex items-center gap-1 text-blue-600 mb-4 font-semibold">
          <ArrowLeft size={18} /> Voltar
        </button>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">{detalhe.fornecedorNome}</h2>
              <p className="text-gray-500 text-sm">WhatsApp: {detalhe.whatsapp || '—'} · {detalhe.email || ''}</p>
            </div>
            <div className="text-right">
              <p className="text-xs text-gray-400 uppercase">Saldo a acertar</p>
              <p className="text-3xl font-bold text-red-600">{moeda(detalhe.saldoAtivo)}</p>
            </div>
          </div>
          <div className="flex gap-3 mt-4">
            <button onClick={() => enviarWhatsapp(detalhe)} className="flex items-center gap-2 bg-green-600 hover:bg-green-500 text-white px-4 py-2 rounded-lg font-semibold">
              <MessageCircle size={18} /> Enviar WhatsApp
            </button>
            <button onClick={() => setModalAberto(true)} className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg font-semibold">
              <Plus size={18} /> Novo lançamento
            </button>
          </div>
        </div>

        <Secao titulo="Trocas / lançamentos ATIVOS" itens={detalhe.ativas} onFinalizar={finalizar} onRemover={remover} ativo />
        <Secao titulo="Histórico (finalizados)" itens={detalhe.finalizadas} ativo={false} />

        {modalAberto && (
          <ModalLancamento
            fornecedorFixo={{ id: detalhe.fornecedorId, nome: detalhe.fornecedorNome }}
            onFechar={() => setModalAberto(false)}
            onSalvo={() => { setModalAberto(false); abrirDetalhe(detalhe.fornecedorId); carregar(); }}
          />
        )}
      </div>
    );
  }

  // ---------- LISTA ----------
  return (
    <div>
      <header className="flex justify-between items-center mb-8 bg-white p-6 rounded-xl shadow-sm border border-gray-200">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <Users className="text-blue-600" size={32} />
            Conta Corrente — Fornecedores
          </h1>
          <p className="text-gray-500 mt-1">Trocas, rebaixas e negociações a acertar com cada fornecedor</p>
        </div>
        <div className="flex gap-3">
          <button onClick={() => setModalAberto(true)} className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg font-semibold">
            <Plus size={18} /> Novo lançamento
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
              <th className="p-4 font-bold">WhatsApp</th>
              <th className="p-4 font-bold text-center">Lançamentos ativos</th>
              <th className="p-4 font-bold text-right">Saldo a acertar</th>
              <th className="p-4 font-bold text-center">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {fornecedores.length === 0 ? (
              <tr><td colSpan="5" className="p-8 text-center text-gray-500">Nenhum lançamento ainda. Use "Novo lançamento".</td></tr>
            ) : fornecedores.map((f) => (
              <tr key={f.fornecedorId} className="hover:bg-blue-50/30">
                <td className="p-4 font-bold text-gray-900">{f.fornecedorNome}</td>
                <td className="p-4 text-gray-600">{f.whatsapp || '—'}</td>
                <td className="p-4 text-center">{f.trocasAtivas}</td>
                <td className="p-4 text-right font-bold text-red-600">{moeda(f.saldoAtivo)}</td>
                <td className="p-4 text-center">
                  <div className="flex gap-2 justify-center">
                    <button onClick={() => abrirDetalhe(f.fornecedorId)} className="text-blue-600 font-semibold">Ver</button>
                    <button onClick={() => enviarWhatsapp(f)} className="text-green-600"><MessageCircle size={18} /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <ModalLancamento
          onFechar={() => setModalAberto(false)}
          onSalvo={() => { setModalAberto(false); carregar(); }}
        />
      )}
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
                <td className="p-3">
                  <span className={`px-2 py-0.5 rounded text-xs font-bold ${l.sinal > 0 ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>{l.tipo}</span>
                </td>
                <td className="p-3 text-gray-600">{l.produtoNome || l.descricao || '—'}</td>
                <td className={`p-3 font-bold ${l.sinal > 0 ? 'text-red-600' : 'text-green-600'}`}>
                  {l.sinal > 0 ? '+' : '−'} {moeda(l.valor)}
                </td>
                {ativo && (
                  <td className="p-3 text-right">
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

function ModalLancamento({ fornecedorFixo, onFechar, onSalvo }) {
  const [busca, setBusca] = useState('');
  const [resultados, setResultados] = useState([]);
  const [fornecedor, setFornecedor] = useState(fornecedorFixo || null);
  const [tipo, setTipo] = useState('TROCA');
  const [valor, setValor] = useState('');
  const [produtoNome, setProdutoNome] = useState('');
  const [descricao, setDescricao] = useState('');
  const [salvando, setSalvando] = useState(false);

  const buscar = async () => {
    const r = await fetch(`${BASE}/fornecedores/buscar?termo=${encodeURIComponent(busca)}`);
    setResultados(await r.json());
  };

  const salvar = async () => {
    if (!fornecedor || !valor) { alert('Escolha o fornecedor e informe o valor.'); return; }
    setSalvando(true);
    try {
      await fetch(`${BASE}/conta-corrente/lancamento`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fornecedorId: fornecedor.id,
          tipo,
          valor: parseFloat(valor),
          produtoNome,
          descricao,
          criadoPor: 'Web',
        }),
      });
      onSalvo();
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl w-full max-w-lg p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-bold text-gray-900">Novo lançamento</h3>
          <button onClick={onFechar}><X size={22} className="text-gray-400" /></button>
        </div>

        {!fornecedor ? (
          <div className="mb-4">
            <label className="block text-sm font-semibold text-gray-600 mb-1">Buscar fornecedor (Uniplus)</label>
            <div className="flex gap-2">
              <input value={busca} onChange={(e) => setBusca(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && buscar()}
                className="flex-1 border rounded-lg px-3 py-2" placeholder="Nome ou CNPJ" />
              <button onClick={buscar} className="bg-blue-600 text-white px-4 rounded-lg font-semibold">Buscar</button>
            </div>
            <div className="mt-2 max-h-48 overflow-y-auto">
              {resultados.map((r) => (
                <button key={r.id} onClick={() => setFornecedor({ id: r.id, nome: r.nome })}
                  className="block w-full text-left px-3 py-2 hover:bg-gray-100 rounded">
                  <span className="font-medium">{r.nome}</span>
                  <span className="text-xs text-gray-400 ml-2">{r.whatsapp || 'sem whatsapp'}</span>
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="mb-4 p-3 bg-blue-50 rounded-lg flex justify-between items-center">
            <span className="font-semibold text-blue-900">{fornecedor.nome}</span>
            {!fornecedorFixo && <button onClick={() => setFornecedor(null)} className="text-sm text-blue-600">trocar</button>}
          </div>
        )}

        <div className="grid grid-cols-2 gap-3 mb-3">
          <div>
            <label className="block text-sm font-semibold text-gray-600 mb-1">Tipo</label>
            <select value={tipo} onChange={(e) => setTipo(e.target.value)} className="w-full border rounded-lg px-3 py-2">
              {TIPOS.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-semibold text-gray-600 mb-1">Valor (R$)</label>
            <input type="number" value={valor} onChange={(e) => setValor(e.target.value)} className="w-full border rounded-lg px-3 py-2" placeholder="0,00" />
          </div>
        </div>
        <div className="mb-3">
          <label className="block text-sm font-semibold text-gray-600 mb-1">Produto (opcional)</label>
          <input value={produtoNome} onChange={(e) => setProdutoNome(e.target.value)} className="w-full border rounded-lg px-3 py-2" placeholder="Ex: Leite Moça 395g" />
        </div>
        <div className="mb-5">
          <label className="block text-sm font-semibold text-gray-600 mb-1">Descrição (opcional)</label>
          <input value={descricao} onChange={(e) => setDescricao(e.target.value)} className="w-full border rounded-lg px-3 py-2" placeholder="Ex: vencido, avaria..." />
        </div>

        <div className="flex justify-end gap-3">
          <button onClick={onFechar} className="px-4 py-2 text-gray-600 font-semibold">Cancelar</button>
          <button onClick={salvar} disabled={salvando} className="bg-blue-600 hover:bg-blue-500 disabled:opacity-60 text-white px-5 py-2 rounded-lg font-semibold">
            {salvando ? 'Salvando...' : 'Salvar'}
          </button>
        </div>
      </div>
    </div>
  );
}
