export default function CardResultado({ titulo, valor, destaque = false, sufixo = '' }) {
  const formatado = typeof valor === 'number'
    ? valor.toLocaleString('pt-BR', { style: sufixo === '%' ? 'decimal' : 'currency', currency: 'BRL', minimumFractionDigits: 2 })
    : valor;

  return (
    <div className={`rounded-xl border p-5 ${destaque ? 'bg-teal-950/40 border-teal-700' : 'bg-slate-800 border-slate-700'}`}>
      <p className="text-xs uppercase tracking-wide text-slate-400 mb-2">{titulo}</p>
      <p className={`text-2xl font-bold ${destaque ? 'text-teal-300' : 'text-white'}`}>
        {formatado}{sufixo}
      </p>
    </div>
  );
}
