import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, ScrollView, Image, ActivityIndicator, Platform } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import DateTimePicker from '@react-native-community/datetimepicker';

// URL do backend. Em produção (APK), defina EXPO_PUBLIC_API_URL no build (eas.json)
// apontando para o Render, ex.: https://validade-api.onrender.com
// Local por USB (adb reverse): http://localhost:8082
const API_BASE = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8082';
const BASE_URL = `${API_BASE}/api`;
const API_CAPTURAS = `${BASE_URL}/capturas`;
const API_BUSCAR = `${BASE_URL}/produtos/buscar`;
const API_PROXIMO_LOTE = `${BASE_URL}/capturas/proximo-lote`;

function formatarDataISO(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function formatarDataBR(isoString) {
  if (!isoString) return '';
  const [y, m, d] = isoString.split('-');
  return `${d}/${m}/${y}`;
}

export default function FormCaptura({ codigoBarras, onVoltar }) {
  const [loading, setLoading] = useState(false);
  const [fotos, setFotos] = useState([]); // data URIs base64

  // Busca do produto no Uniplus
  const [buscando, setBuscando] = useState(true);
  const [statusBusca, setStatusBusca] = useState('buscando'); // buscando | encontrado | novo
  const [opcoes, setOpcoes] = useState([]); // quando a digitacao acha varios

  // Calendário da validade
  const [mostrarCalendario, setMostrarCalendario] = useState(false);
  const [dataSelecionada, setDataSelecionada] = useState(new Date());

  const [form, setForm] = useState({
    codigoBarras: codigoBarras || '',
    nome: '',
    grupo: '',
    numeroLote: '',
    quantidadeInicial: '',
    dataVencimento: '', // Formato AAAA-MM-DD
    criadoPor: 'Gerente (App Mobile)'
  });

  // Ao abrir, busca o produto no Uniplus pelo EAN (>=8 digitos) ou pelos ultimos digitos,
  // e ja busca o proximo numero de lote sequencial para esse produto.
  useEffect(() => {
    buscarProduto(codigoBarras);
    buscarProximoLote(codigoBarras);
  }, [codigoBarras]);

  const buscarProximoLote = async (codigo) => {
    if (!codigo) return;
    try {
      const resposta = await fetch(`${API_PROXIMO_LOTE}?codigoBarras=${encodeURIComponent(codigo)}`);
      const dados = await resposta.json();
      if (dados.numeroLote) {
        setForm(f => ({ ...f, numeroLote: String(dados.numeroLote) }));
      }
    } catch (e) {
      console.error(e);
    }
  };

  const onMudarData = (event, dataEscolhida) => {
    setMostrarCalendario(Platform.OS === 'ios'); // iOS mantem aberto ate confirmar, Android fecha sozinho
    if (event.type === 'set' && dataEscolhida) {
      setDataSelecionada(dataEscolhida);
      setForm(f => ({ ...f, dataVencimento: formatarDataISO(dataEscolhida) }));
    }
  };

  const buscarProduto = async (codigo) => {
    if (!codigo) { setBuscando(false); setStatusBusca('novo'); return; }
    setBuscando(true);
    try {
      const param = codigo.length >= 8 ? `ean=${encodeURIComponent(codigo)}` : `ultimos=${encodeURIComponent(codigo)}`;
      const resposta = await fetch(`${API_BUSCAR}?${param}`);
      const dados = await resposta.json();

      if (!dados.encontrado || dados.produtos.length === 0) {
        setStatusBusca('novo');
      } else if (dados.produtos.length === 1) {
        aplicarProduto(dados.produtos[0]);
      } else {
        setOpcoes(dados.produtos); // deixa o usuario escolher
        setStatusBusca('encontrado');
      }
    } catch (e) {
      console.error(e);
      setStatusBusca('novo'); // sem conexao com o backend -> trata como novo
    } finally {
      setBuscando(false);
    }
  };

  const aplicarProduto = (produto) => {
    setForm(f => ({ ...f, codigoBarras: produto.ean || f.codigoBarras, nome: produto.nome, grupo: produto.grupo }));
    setOpcoes([]);
    setStatusBusca('encontrado');
  };

  const opcoesImagem = { quality: 0.3, base64: true, allowsEditing: false };

  const adicionarFoto = (asset) => {
    if (!asset?.base64) return;
    setFotos(anterior => [...anterior, `data:image/jpeg;base64,${asset.base64}`]);
  };

  const tirarFoto = async () => {
    const permissao = await ImagePicker.requestCameraPermissionsAsync();
    if (!permissao.granted) {
      Alert.alert('Permissão', 'Precisamos da câmara para tirar a foto do produto.');
      return;
    }
    const resultado = await ImagePicker.launchCameraAsync(opcoesImagem);
    if (!resultado.canceled) adicionarFoto(resultado.assets[0]);
  };

  const escolherDaGaleria = async () => {
    const resultado = await ImagePicker.launchImageLibraryAsync({ ...opcoesImagem, mediaTypes: ['images'] });
    if (!resultado.canceled) adicionarFoto(resultado.assets[0]);
  };

  const removerFoto = (indice) => {
    setFotos(anterior => anterior.filter((_, i) => i !== indice));
  };

  const salvarDados = async () => {
    if (!form.nome.trim()) {
      Alert.alert("Atenção", "Preencha o Nome do Produto.");
      return;
    }
    if (!form.numeroLote || !form.quantidadeInicial || !form.dataVencimento) {
      Alert.alert("Atenção", "Preencha o Lote, Quantidade e a Data de Vencimento.");
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(API_CAPTURAS, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          codigoBarras: form.codigoBarras,
          nome: form.nome,
          categoria: form.grupo, // grupo do Uniplus guardado no campo categoria
          numeroLote: form.numeroLote,
          dataVencimento: form.dataVencimento,
          criadoPor: form.criadoPor,
          quantidadeInicial: parseInt(form.quantidadeInicial, 10),
          fotosUrls: fotos
        })
      });

      if (response.ok) {
        Alert.alert("Sucesso", "Lote registado com sucesso!");
        onVoltar();
      } else {
        Alert.alert("Erro", "Falha ao gravar no servidor (Backend pode estar desligado).");
      }
    } catch (error) {
      Alert.alert("Erro de Ligação", "Não foi possível ligar ao Spring Boot. Confirmou o IP/backend?");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  // Banner de status da busca no Uniplus
  const renderStatus = () => {
    if (buscando) {
      return (
        <View style={[styles.banner, styles.bannerBuscando]}>
          <ActivityIndicator color="#1e40af" />
          <Text style={styles.bannerTexto}>Buscando no Uniplus...</Text>
        </View>
      );
    }
    if (statusBusca === 'encontrado' && opcoes.length === 0) {
      return (
        <View style={[styles.banner, styles.bannerOk]}>
          <Text style={styles.bannerTextoOk}>✓ Produto encontrado no Uniplus</Text>
        </View>
      );
    }
    if (statusBusca === 'novo') {
      return (
        <View style={[styles.banner, styles.bannerNovo]}>
          <Text style={styles.bannerTextoNovo}>Produto novo — digite o nome abaixo</Text>
        </View>
      );
    }
    return null;
  };

  // Quando a digitacao dos ultimos digitos acha varios produtos
  const renderOpcoes = () => {
    if (opcoes.length === 0) return null;
    return (
      <View style={styles.opcoesBox}>
        <Text style={styles.label}>Vários produtos com esse final — escolha:</Text>
        {opcoes.map((p, i) => (
          <TouchableOpacity key={i} style={styles.opcaoItem} onPress={() => aplicarProduto(p)}>
            <Text style={styles.opcaoNome}>{p.nome}</Text>
            <Text style={styles.opcaoInfo}>{p.ean} · {p.grupo}</Text>
          </TouchableOpacity>
        ))}
      </View>
    );
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
       <Text style={styles.title}>Registar Validade</Text>

       {renderStatus()}
       {renderOpcoes()}

       <Text style={styles.label}>Código de Barras</Text>
       <TextInput style={[styles.input, styles.inputDisabled]} value={form.codigoBarras} editable={false} />

       <Text style={styles.label}>Nome do Produto</Text>
       <TextInput style={styles.input} value={form.nome} onChangeText={t => setForm({...form, nome: t})} placeholder="Ex: Leite Condensado Moça 395g" />

       <Text style={styles.label}>Grupo</Text>
       <TextInput style={[styles.input, styles.inputDisabled]} value={form.grupo} editable={false} placeholder="(vem do Uniplus)" />

       <View style={styles.row}>
         <View style={{flex: 1, marginRight: 10}}>
            <Text style={styles.label}>Lote (automático)</Text>
            <TextInput style={[styles.input, styles.inputDisabled]} value={form.numeroLote} editable={false} placeholder="..." />
         </View>
         <View style={{flex: 1}}>
            <Text style={styles.label}>Qtd. Total</Text>
            <TextInput style={styles.input} onChangeText={t => setForm({...form, quantidadeInicial: t})} keyboardType="numeric" placeholder="Ex: 50" />
         </View>
       </View>

       <Text style={styles.label}>Data de Vencimento</Text>
       <TouchableOpacity style={styles.inputData} onPress={() => setMostrarCalendario(true)}>
         <Text style={form.dataVencimento ? styles.inputDataTexto : styles.inputDataPlaceholder}>
           {form.dataVencimento ? formatarDataBR(form.dataVencimento) : 'Toque para escolher a data'}
         </Text>
       </TouchableOpacity>
       {mostrarCalendario && (
         <DateTimePicker
           value={dataSelecionada}
           mode="date"
           display={Platform.OS === 'ios' ? 'inline' : 'calendar'}
           minimumDate={new Date()}
           onChange={onMudarData}
         />
       )}

       <Text style={styles.label}>Evidências (fotos) — opcional</Text>
       <View style={styles.fotoBotoes}>
         <TouchableOpacity style={styles.btnFoto} onPress={tirarFoto}>
           <Text style={styles.btnFotoTexto}>📷 Tirar Foto</Text>
         </TouchableOpacity>
         <TouchableOpacity style={styles.btnFoto} onPress={escolherDaGaleria}>
           <Text style={styles.btnFotoTexto}>🖼️ Galeria</Text>
         </TouchableOpacity>
       </View>

       {fotos.length > 0 && (
         <ScrollView horizontal style={styles.fotoLista} showsHorizontalScrollIndicator={false}>
           {fotos.map((uri, i) => (
             <View key={i} style={styles.fotoItem}>
               <Image source={{ uri }} style={styles.fotoThumb} />
               <TouchableOpacity style={styles.fotoRemover} onPress={() => removerFoto(i)}>
                 <Text style={styles.fotoRemoverTexto}>✕</Text>
               </TouchableOpacity>
             </View>
           ))}
         </ScrollView>
       )}

       <TouchableOpacity style={styles.btnSalvar} onPress={salvarDados} disabled={loading}>
         <Text style={styles.btnText}>{loading ? "A Guardar..." : "Salvar Lote no Sistema"}</Text>
       </TouchableOpacity>

       <TouchableOpacity style={styles.btnVoltar} onPress={onVoltar} disabled={loading}>
         <Text style={styles.btnTextVoltar}>Cancelar e Voltar</Text>
       </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 20, paddingTop: 30, flexGrow: 1 },
  title: { fontSize: 24, fontWeight: 'bold', marginBottom: 16, color: '#1e40af' },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  label: { fontSize: 13, fontWeight: 'bold', marginBottom: 5, color: '#4b5563', textTransform: 'uppercase' },
  input: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#d1d5db', padding: 15, borderRadius: 10, marginBottom: 15, fontSize: 16 },
  inputDisabled: { backgroundColor: '#e5e7eb', color: '#374151' },
  inputData: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#d1d5db', padding: 15, borderRadius: 10, marginBottom: 15 },
  inputDataTexto: { fontSize: 16, color: '#111827', fontWeight: 'bold' },
  inputDataPlaceholder: { fontSize: 16, color: '#9ca3af' },
  banner: { flexDirection: 'row', alignItems: 'center', gap: 10, padding: 12, borderRadius: 10, marginBottom: 16 },
  bannerBuscando: { backgroundColor: '#dbeafe' },
  bannerOk: { backgroundColor: '#d1fae5' },
  bannerNovo: { backgroundColor: '#fef3c7' },
  bannerTexto: { color: '#1e40af', fontWeight: 'bold' },
  bannerTextoOk: { color: '#065f46', fontWeight: 'bold' },
  bannerTextoNovo: { color: '#92400e', fontWeight: 'bold' },
  opcoesBox: { marginBottom: 16 },
  opcaoItem: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#d1d5db', borderRadius: 10, padding: 12, marginBottom: 8 },
  opcaoNome: { fontSize: 15, fontWeight: 'bold', color: '#111827' },
  opcaoInfo: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  fotoBotoes: { flexDirection: 'row', gap: 12, marginBottom: 12 },
  btnFoto: { flex: 1, backgroundColor: '#0d9488', padding: 14, borderRadius: 10, alignItems: 'center' },
  btnFotoTexto: { color: '#fff', fontWeight: 'bold', fontSize: 15 },
  fotoLista: { marginBottom: 10 },
  fotoItem: { marginRight: 10, position: 'relative' },
  fotoThumb: { width: 90, height: 90, borderRadius: 10, borderWidth: 1, borderColor: '#d1d5db' },
  fotoRemover: { position: 'absolute', top: -8, right: -8, backgroundColor: '#ef4444', width: 24, height: 24, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  fotoRemoverTexto: { color: '#fff', fontWeight: 'bold', fontSize: 13 },
  btnSalvar: { backgroundColor: '#2563eb', padding: 18, borderRadius: 10, alignItems: 'center', marginTop: 15, elevation: 2 },
  btnText: { color: '#fff', fontWeight: 'bold', fontSize: 16 },
  btnVoltar: { padding: 15, alignItems: 'center', marginTop: 10 },
  btnTextVoltar: { color: '#ef4444', fontWeight: 'bold', fontSize: 16 }
});
