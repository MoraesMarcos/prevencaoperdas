import React, { useState, useEffect } from 'react';
import { TicketPercent, RefreshCw, AlertCircle, Users, Building2, CheckCircle2, X, Search, Eye } from 'lucide-react';

import { API_BASE } from '../config';
const API = `${API_BASE}/api/rebaixa-parceria`;
const API_FORN = `${API_BASE}/api/fornecedores/buscar`;

const brl = (v) => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export default function RebaixaParceria() {
  const [itens, setItens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);
  const [lancando, setLancando] = useState(null); // ean em processamento
  const [modal, setModal] = useState(null); // { item } quando escolhendo fornecedor
  const [detalhe, setDetalhe] = useState(null); // item aberto na auditoria de linhas

  const carregar = async () => {
    setLoading(true);
    setErro(null);
    try {
      const r = await fetch(API);
      if (!r.ok) throw new Error('falha');
      setItens(await r.json());
    } catch {
      setErro('Não foi possível conectar ao Spring Boot na porta 8082.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { carregar(); }, []);

  const lancar = async (ean, responsavel, fornecedorId) => {
    setLancando(ean);
    try {
      const r = await fetch(`${API}/lancar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ean, responsavel, fornecedorId }),
      });
      const res = await r.json();
      alert(res.mensagem || (r.ok ? 'Lançado.' : 'Não foi possível lançar.'));
      setModal(null);
      await carregar();
    } catch {
      alert('Falha ao lançar.');
    } finally {
      setLancando(null);
    }
  };

  const pagarMercado = (it) => {
    if (!window.confirm('Confirmar lançamento da cobertura no mercado (descontar bonificação)?')) return;
    lancar(it.ean, 'MERCADO', null);
  };

  const totalPendente = itens.reduce((s, it) => s + Number(it.coberturaPendente || 0), 0);

  return (
    <>
      <header className="flex justify-between items-center mb-8 bg-white p-6 rounded-xl shadow-sm border border-gray-200">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <TicketPercent className="text-amber-600" size={32} />
            Rebaixa Parceria
          </h1>
          <p className="text-gray-500 mt-1">
            Vendas com desconto de parceria (Uniplus). Decida quem cobre: fornecedor ou mercado.
          </p>
        </div>
        <div className="flex items-center gap-4">
          <div className="text-right">
            <div className="text-xs text-gray-400 uppercase">Pendente total</div>
            <div className="text-2xl font-bold text-amber-600">{brl(totalPendente)}</div>
          </div>
          <button
            onClick={carregar}
            className="flex items-center gap-2 bg-blue-50 border border-blue-200 hover:bg-blue-100 text-blue-700 px-4 py-2 rounded-lg shadow-sm font-semibold"
          >
            <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
            Atualizar
          </button>
        </div>
      </header>

      {erro && (
        <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6 rounded shadow-sm flex items-center gap-2 text-red-700">
          <AlertCircle size={20} /><p className="font-medium">{erro}</p>
        </div>
      )}

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 text-gray-600 text-xs uppercase tracking-wider border-b border-gray-200">
                <th className="p-4 font-bold">Produto</th>
                <th className="p-4 font-bold">Fornecedor sugerido</th>
                <th className="p-4 font-bold text-center">Qtd</th>
                <th className="p-4 font-bold">Última venda</th>
                <th className="p-4 font-bold text-right">Cobertura</th>
                <th className="p-4 font-bold text-center">Quem paga?</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading && itens.length === 0 ? (
                <tr><td colSpan="6" className="p-8 text-center text-gray-400">
                  <RefreshCw size={24} className="animate-spin mx-auto mb-2 text-blue-500" /> Carregando...
                </td></tr>
              ) : itens.length === 0 ? (
                <tr><td colSpan="6" className="p-8 text-center text-gray-500 font-medium">
                  <CheckCircle2 size={22} className="mx-auto mb-2 text-emerald-500" />
                  Nenhuma cobertura de parceria pendente.
                </td></tr>
              ) : (
                itens.map((it) => (
                  <tr key={it.ean} className="hover:bg-amber-50/30 transition-colors">
                    <td className="p-4">
                      <div className="flex items-center gap-2">
                        <div>
                          <div className="font-bold text-gray-900">{it.produtoNome}</div>
                          <div className="text-xs text-gray-400">EAN {it.ean}</div>
                        </div>
                        <button
                          onClick={() => setDetalhe(it)}
                          title="Ver vendas que compõem esse valor"
                          className="text-gray-400 hover:text-amber-600 shrink-0"
                        >
                          <Eye size={16} />
                        </button>
                      </div>
                    </td>
                    <td className="p-4 text-sm text-gray-600">{it.fornecedorNome}</td>
                    <td className="p-4 text-center font-bold text-gray-700">
                      {Number(it.quantidade || 0).toLocaleString('pt-BR')}
                    </td>
                    <td className="p-4 text-sm text-gray-500">
                      {it.ultimaVenda ? new Date(it.ultimaVenda).toLocaleString('pt-BR') : '—'}
                    </td>
                    <td className="p-4 text-right font-bold text-amber-600 text-lg">{brl(it.coberturaPendente)}</td>
                    <td className="p-4">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => setModal({ item: it })}
                          disabled={lancando === it.ean}
                          className="flex items-center gap-1 text-xs bg-blue-600 hover:bg-blue-500 disabled:opacity-40 text-white px-3 py-2 rounded-lg font-semibold"
                        >
                          <Users size={14} /> Fornecedor
                        </button>
                        <button
                          onClick={() => pagarMercado(it)}
                          disabled={lancando === it.ean}
                          className="flex items-center gap-1 text-xs bg-emerald-600 hover:bg-emerald-500 disabled:opacity-40 text-white px-3 py-2 rounded-lg font-semibold"
                        >
                          <Building2 size={14} /> {lancando === it.ean ? '...' : 'Mercado'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <p className="text-xs text-gray-400 mt-4">
        Cobertura = (preço original − preço rebaixado) × qtd das vendas marcadas como REBAIXA PARCERIA (promoção 2) no Uniplus.
        Cada lançamento marca as vendas como processadas, então não são contadas de novo.
      </p>

      {modal && (
        <SeletorFornecedor
          item={modal.item}
          lancando={lancando === modal.item.ean}
          onFechar={() => setModal(null)}
          onEscolher={(fornId) => lancar(modal.item.ean, 'FORNECEDOR', fornId)}
        />
      )}

      {detalhe && (
        <DetalheLinhas item={detalhe} onFechar={() => setDetalhe(null)} />
      )}
    </>
  );
}

function DetalheLinhas({ item, onFechar }) {
  const [linhas, setLinhas] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    (async () => {
      setCarregando(true);
      setErro(null);
      try {
        const r = await fetch(`${API}/detalhes?ean=${encodeURIComponent(item.ean)}`);
        if (!r.ok) throw new Error();
        setLinhas(await r.json());
      } catch {
        setErro('Não foi possível carregar as vendas detalhadas.');
      } finally {
        setCarregando(false);
      }
    })();
  }, [item.ean]);

  const somaLinha = linhas.reduce((s, l) => s + Number(l.descontoLinha || 0), 0);

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={onFechar}>
      <div className="bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[85vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center p-5 border-b border-gray-200">
          <div>
            <h3 className="font-bold text-lg text-gray-900">Vendas que compõem a cobertura</h3>
            <p className="text-sm text-gray-500">{item.produtoNome} — EAN {item.ean}</p>
          </div>
          <button onClick={onFechar} className="text-gray-400 hover:text-gray-700"><X size={22} /></button>
        </div>

        <div className="overflow-y-auto flex-1">
          {carregando ? (
            <p className="p-8 text-center text-gray-400"><RefreshCw size={20} className="animate-spin mx-auto mb-2" /> Carregando...</p>
          ) : erro ? (
            <p className="p-8 text-center text-red-600">{erro}</p>
          ) : linhas.length === 0 ? (
            <p className="p-8 text-center text-gray-400">Nenhuma venda encontrada.</p>
          ) : (
            <table className="w-full text-left text-sm border-collapse">
              <thead className="sticky top-0 bg-gray-50">
                <tr className="text-xs uppercase text-gray-500 border-b">
                  <th className="p-3">Data/Hora</th>
                  <th className="p-3 text-right">Preço bruto</th>
                  <th className="p-3 text-right">Preço líquido</th>
                  <th className="p-3 text-center">Qtd</th>
                  <th className="p-3 text-right">Desconto unit.</th>
                  <th className="p-3 text-right">Desconto da linha</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {linhas.map((l, i) => (
                  <tr key={i} className="hover:bg-amber-50/30">
                    <td className="p-3 text-gray-600">
                      {l.dataHora ? new Date(l.dataHora).toLocaleString('pt-BR') : '—'}
                    </td>
                    <td className="p-3 text-right">{brl(l.precoBruto)}</td>
                    <td className="p-3 text-right">{brl(l.precoLiquido)}</td>
                    <td className="p-3 text-center font-semibold">{Number(l.quantidade || 0).toLocaleString('pt-BR')}</td>
                    <td className="p-3 text-right text-gray-600">{brl(l.descontoUnitario)}</td>
                    <td className="p-3 text-right font-bold text-amber-600">{brl(l.descontoLinha)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {!carregando && !erro && linhas.length > 0 && (
          <div className="p-4 border-t border-gray-200 flex justify-between items-center bg-gray-50">
            <span className="text-sm text-gray-500">{linhas.length} venda(s)</span>
            <span className="text-sm font-bold text-gray-700">
              Soma das linhas: <span className="text-amber-600">{brl(somaLinha)}</span>
            </span>
          </div>
        )}
      </div>
    </div>
  );
}

function SeletorFornecedor({ item, lancando, onFechar, onEscolher }) {
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

  // Pré-carrega o fornecedor sugerido (se houver) ao abrir.
  useEffect(() => {
    if (item.fornecedorId) buscar(item.fornecedorNome);
    else buscar('');
  }, []);

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={onFechar}>
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[80vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center p-5 border-b border-gray-200">
          <div>
            <h3 className="font-bold text-lg text-gray-900">Escolher fornecedor que paga</h3>
            <p className="text-sm text-gray-500">{item.produtoNome} · <span className="font-semibold text-amber-600">{brl(item.coberturaPendente)}</span></p>
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
                disabled={lancando}
                onClick={() => {
                  if (window.confirm(`Lançar ${brl(item.coberturaPendente)} na conta de ${f.nome}?`)) onEscolher(f.id);
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
