import { createClient } from '@supabase/supabase-js';

// URL e chave publica (anon) do projeto Supabase. Definir no ambiente de build
// (local: .env; Vercel: Project Settings > Environment Variables):
//   VITE_SUPABASE_URL=https://xxxxxxxx.supabase.co
//   VITE_SUPABASE_ANON_KEY=eyJhbGciOi...
// Ambos ficam em Supabase > Project Settings > API. A anon key é pública por
// design (protegida pelas regras de RLS do projeto), não é segredo de servidor.
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true, // mantém logado entre recarregamentos/reaberturas do navegador
    autoRefreshToken: true,
  },
});
