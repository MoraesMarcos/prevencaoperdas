import React, { useState, useEffect } from 'react';
import { Images, RefreshCw, Search, ImageOff } from 'lucide-react';

const BASE = 'http://localhost:8082/api';

const STATUS_COR = {
  CRITICO: 'bg-red-100 text-red-700 border-red-200',
  ATENCAO: 'bg-yellow-100 text-yellow-700 border-yellow-200',
  NORMAL: 'bg-green-100 text-green-700 border-green-200',
  VENCIDO: 'bg-gray-200 text-gray-700 border-gray-300',
};

export default function EstoqueFotos() {
  const [itens, setItens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState(null);
  const [filtro, setFiltro] = useState('');
  const [soComFoto, setSoComFoto] = useState(false);

  const carregar = async () => {
    setLoading(true); setErro(null);
    try {
      const r = await fetch(`${BASE}/estoque-fotos`);
      if (!r.ok) throw new Error();
      setItens(await r.json());
    } catch { setErro('Não foi possível carregar. Backend na porta 8082?'); }
    finally { setLoading(false); }
  };

  useEffect(() => { carregar(); }, []);

  const filtrados = itens
    .filter(it => !soComFoto || (it.fotos && it.fotos.length > 0))
    .filter(it => it.produtoNome.toUpperCase().includes(filtro.toUpperCase()) || (it.grupo || '').toUpperCase().includes(filtro.toUpperCase()));

  // Agrupa por grupo
  const grupos = {};
  for (const it of filtrados) {
    (grupos[it.grupo] = grupos[it.grupo] || []).push(it);
  }
  const gruposOrdenados = Object.keys(grupos).sort();

  return (
    <div>
      <header className="flex flex-wrap justify-between items-center gap-4 mb-8 bg-white p-6 rounded-xl shadow-sm border border-gray-200">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <Images className="text-blue-600" size={32} /> Estoque por Grupo (fotos)
          </h1>
          <p className="text-gray-500 mt-1">Como o estoque está, com as evidências capturadas no app</p>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer select-none">
            <input type="checkbox" checked={soComFoto} onChange={(e) => setSoComFoto(e.target.checked)} className="w-4 h-4" />
            Só com foto
          </label>
          <div className="flex items-center gap-2 bg-white border rounded-lg px-2">
            <Search size={16} className="text-gray-400" />
            <input value={filtro} onChange={(e) => setFiltro(e.target.value)} placeholder="Filtrar produto/grupo..." className="py-2 outline-none text-sm" />
          </div>
          <button onClick={carregar} className="flex items-center gap-2 bg-blue-50 border border-blue-200 hover:bg-blue-100 text-blue-700 px-4 py-2 rounded-lg font-semibold">
            <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </header>

      {erro && <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6 rounded text-red-700">{erro}</div>}
      {!loading && filtrados.length === 0 && !erro && (
        <p className="text-center text-gray-500 mt-10">Nenhum lote para mostrar.</p>
      )}

      {gruposOrdenados.map((grupo) => (
        <section key={grupo} className="mb-8">
          <h2 className="text-lg font-bold text-gray-700 mb-3 flex items-center gap-2">
            {grupo}
            <span className="text-xs font-normal bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">{grupos[grupo].length}</span>
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {grupos[grupo].map((it) => (
              <div key={it.loteId} className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                <div className="aspect-square bg-gray-100 flex items-center justify-center overflow-hidden">
                  {it.fotos && it.fotos.length > 0 ? (
                    <img src={it.fotos[0]} alt={it.produtoNome} className="w-full h-full object-cover" />
                  ) : (
                    <ImageOff size={32} className="text-gray-300" />
                  )}
                </div>
                <div className="p-3">
                  <p className="text-sm font-bold text-gray-800 leading-tight line-clamp-2" title={it.produtoNome}>{it.produtoNome}</p>
                  <div className="flex items-center justify-between mt-2">
                    <span className="text-xs text-gray-500">Qtd {it.quantidadeAtual} · Lote {it.numeroLote}</span>
                  </div>
                  <div className="flex items-center justify-between mt-2">
                    <span className={`text-xs font-bold px-2 py-0.5 rounded-full border ${STATUS_COR[it.status] || ''}`}>{it.status}</span>
                    <span className={`text-xs font-semibold ${it.diasParaVencer <= 30 ? 'text-red-500' : 'text-gray-500'}`}>
                      {it.diasParaVencer > 0 ? `${it.diasParaVencer}d` : 'venc.'}
                    </span>
                  </div>
                  {it.fotos && it.fotos.length > 1 && (
                    <p className="text-xs text-blue-500 mt-1">+{it.fotos.length - 1} foto(s)</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
