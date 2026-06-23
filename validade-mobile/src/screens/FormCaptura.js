import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, ScrollView, Image } from 'react-native';
import * as ImagePicker from 'expo-image-picker';

// ⚠️ ATENÇÃO: Substitua "192.168.1.X" pelo IP real do seu computador na rede Wi-Fi
// Não use 'localhost' aqui, senão o telemóvel não encontra o Spring Boot do computador!
const API_URL = 'http://localhost:8082/api/capturas';

export default function FormCaptura({ codigoBarras, onVoltar }) {
  const [loading, setLoading] = useState(false);
  const [fotos, setFotos] = useState([]); // data URIs base64
  const [form, setForm] = useState({
    codigoBarras: codigoBarras || '',
    nome: '',
    marca: '',
    categoria: '',
    numeroLote: '',
    quantidadeInicial: '',
    dataVencimento: '', // Formato AAAA-MM-DD
    criadoPor: 'Gerente (App Mobile)'
  });

  // Compressão agressiva (quality baixa) porque a foto vai em base64 no corpo do POST.
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
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...form,
          quantidadeInicial: parseInt(form.quantidadeInicial, 10),
          fotosUrls: fotos
        })
      });

      if (response.ok) {
        Alert.alert("Sucesso", "Lote registado com sucesso!");
        onVoltar(); // Volta para a câmara para ler o próximo
      } else {
        Alert.alert("Erro", "Falha ao gravar no servidor (Backend pode estar desligado).");
      }
    } catch (error) {
      Alert.alert("Erro de Ligação", "Não foi possível ligar ao Spring Boot. Confirmou o IP na variável API_URL?");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
       <Text style={styles.title}>Registar Validade</Text>

       <Text style={styles.label}>Código de Barras</Text>
       <TextInput style={[styles.input, styles.inputDisabled]} value={form.codigoBarras} editable={false} />

       <Text style={styles.label}>Nome do Produto</Text>
       <TextInput style={styles.input} value={form.nome} onChangeText={t => setForm({...form, nome: t})} placeholder="Ex: Refrigerante Cola 2L" />

       <View style={styles.row}>
         <View style={{flex: 1, marginRight: 10}}>
            <Text style={styles.label}>Marca</Text>
            <TextInput style={styles.input} value={form.marca} onChangeText={t => setForm({...form, marca: t})} placeholder="Ex: Coca-Cola" />
         </View>
         <View style={{flex: 1}}>
            <Text style={styles.label}>Categoria</Text>
            <TextInput style={styles.input} value={form.categoria} onChangeText={t => setForm({...form, categoria: t})} placeholder="Ex: Bebidas" />
         </View>
       </View>

       <View style={styles.row}>
         <View style={{flex: 1, marginRight: 10}}>
            <Text style={styles.label}>Lote</Text>
            <TextInput style={styles.input} onChangeText={t => setForm({...form, numeroLote: t})} placeholder="Ex: L123" />
         </View>
         <View style={{flex: 1}}>
            <Text style={styles.label}>Qtd. Total</Text>
            <TextInput style={styles.input} onChangeText={t => setForm({...form, quantidadeInicial: t})} keyboardType="numeric" placeholder="Ex: 50" />
         </View>
       </View>

       <Text style={styles.label}>Data de Vencimento (Ano-Mês-Dia)</Text>
       <TextInput style={styles.input} onChangeText={t => setForm({...form, dataVencimento: t})} placeholder="2026-12-31" />

       <Text style={styles.label}>Evidências (fotos)</Text>
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
  title: { fontSize: 24, fontWeight: 'bold', marginBottom: 25, color: '#1e40af' },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  label: { fontSize: 13, fontWeight: 'bold', marginBottom: 5, color: '#4b5563', textTransform: 'uppercase' },
  input: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#d1d5db', padding: 15, borderRadius: 10, marginBottom: 15, fontSize: 16 },
  inputDisabled: { backgroundColor: '#e5e7eb', color: '#6b7280' },
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
