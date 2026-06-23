const API_URL = 'http://localhost:8090/api';

function getToken() {
  return localStorage.getItem('bi_token');
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${API_URL}${path}`, { ...options, headers });

  if (response.status === 401) {
    localStorage.removeItem('bi_token');
    window.location.href = '/login';
    throw new Error('Sessao expirada');
  }

  if (!response.ok) {
    const texto = await response.text();
    throw new Error(texto || `Erro ${response.status}`);
  }

  if (response.status === 204) return null;
  return response.json();
}

export const api = {
  login: (email, senha) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ email, senha }) }),

  resultadoFinanceiro: (dataInicio, dataFim, filial) => {
    const params = new URLSearchParams({ dataInicio, dataFim, ...(filial ? { filial } : {}) });
    return request(`/dashboard/resultado-financeiro?${params}`);
  },

  listarDespesas: (inicio, fim) =>
    request(`/despesas?inicio=${inicio}&fim=${fim}`),

  registrarDespesa: (despesa) =>
    request('/despesas', { method: 'POST', body: JSON.stringify(despesa) }),

  removerDespesa: (id) =>
    request(`/despesas/${id}`, { method: 'DELETE' }),

  sugerirCategoria: (descricao) =>
    request(`/despesas/sugestao-categoria?descricao=${encodeURIComponent(descricao)}`),

  listarCategorias: () => request('/categorias-despesa'),
};
