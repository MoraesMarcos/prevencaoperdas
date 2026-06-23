import React, { useState } from 'react';
import { Text, View, StyleSheet, TouchableOpacity, Modal, TextInput, KeyboardAvoidingView, Platform } from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';

export default function ScannerScreen({ onScan }) {
  const [permission, requestPermission] = useCameraPermissions();
  const [scanned, setScanned] = useState(false);
  const [modalDigitar, setModalDigitar] = useState(false);
  const [codigoDigitado, setCodigoDigitado] = useState('');

  // Verifica se o utilizador deu permissão para usar a câmara
  if (!permission) return <View />;
  if (!permission.granted) {
    return (
      <View style={styles.containerPermissao}>
        <Text style={styles.textoMensagem}>Precisamos da sua permissão para usar a câmara e ler os códigos.</Text>
        <TouchableOpacity style={styles.botao} onPress={requestPermission}>
          <Text style={styles.textoBotao}>Conceder Permissão</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.botaoSecundario} onPress={() => setModalDigitar(true)}>
          <Text style={styles.textoBotaoSecundario}>Digitar código manualmente</Text>
        </TouchableOpacity>
        {renderModalDigitar()}
      </View>
    );
  }

  // Função disparada quando um código de barras passa na câmara
  const handleBarCodeScanned = ({ data }) => {
    setScanned(true);
    onScan(data); // Envia o código lido para o fluxo de captura

    // Volta a ativar o scanner após 2 segundos caso o utilizador cancele
    setTimeout(() => setScanned(false), 2000);
  };

  const confirmarDigitado = () => {
    const codigo = codigoDigitado.trim();
    if (!codigo) return;
    setModalDigitar(false);
    setCodigoDigitado('');
    onScan(codigo); // Mesmo caminho do scan: segue para o formulário
  };

  function renderModalDigitar() {
    return (
      <Modal visible={modalDigitar} transparent animationType="fade" onRequestClose={() => setModalDigitar(false)}>
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
          style={styles.modalFundo}>
          <View style={styles.modalCaixa}>
            <Text style={styles.modalTitulo}>Digitar código de barras</Text>
            <TextInput
              style={styles.modalInput}
              value={codigoDigitado}
              onChangeText={setCodigoDigitado}
              keyboardType="number-pad"
              placeholder="Ex: 7891234567890"
              autoFocus
            />
            <View style={styles.modalBotoes}>
              <TouchableOpacity style={styles.modalBtnCancelar} onPress={() => { setModalDigitar(false); setCodigoDigitado(''); }}>
                <Text style={styles.modalBtnCancelarTexto}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.modalBtnConfirmar} onPress={confirmarDigitado}>
                <Text style={styles.modalBtnConfirmarTexto}>Confirmar</Text>
              </TouchableOpacity>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    );
  }

  return (
    <View style={styles.container}>
      <CameraView
        style={styles.camera}
        facing="back"
        barcodeScannerSettings={{
          barcodeTypes: ["ean13", "ean8", "qr", "upc_a", "upc_e"],
        }}
        onBarcodeScanned={scanned ? undefined : handleBarCodeScanned}
      />
      <View style={styles.overlay}>
        <Text style={styles.instrucao}>Aponte a câmara para o código de barras</Text>
        <TouchableOpacity style={styles.botaoDigitar} onPress={() => setModalDigitar(true)}>
          <Text style={styles.botaoDigitarTexto}>Digitar código manualmente</Text>
        </TouchableOpacity>
      </View>
      {renderModalDigitar()}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  camera: { flex: 1 },
  containerPermissao: { flex: 1, justifyContent: 'center', padding: 20, alignItems: 'center' },
  textoMensagem: { textAlign: 'center', fontSize: 16, marginBottom: 20 },
  botao: { backgroundColor: '#2563eb', padding: 15, borderRadius: 8 },
  textoBotao: { color: 'white', fontWeight: 'bold' },
  botaoSecundario: { marginTop: 16, padding: 12 },
  textoBotaoSecundario: { color: '#2563eb', fontWeight: 'bold', fontSize: 15 },
  overlay: {
    position: 'absolute', bottom: 50, left: 0, right: 0, alignItems: 'center', gap: 14
  },
  instrucao: {
    backgroundColor: 'white', padding: 15, borderRadius: 8, fontSize: 16, fontWeight: 'bold', overflow: 'hidden', elevation: 5
  },
  botaoDigitar: {
    backgroundColor: '#1e40af', paddingVertical: 12, paddingHorizontal: 22, borderRadius: 8, elevation: 5
  },
  botaoDigitarTexto: { color: 'white', fontWeight: 'bold', fontSize: 15 },
  // Modal
  modalFundo: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'center', padding: 24 },
  modalCaixa: { backgroundColor: 'white', borderRadius: 14, padding: 20 },
  modalTitulo: { fontSize: 18, fontWeight: 'bold', color: '#1e3a8a', marginBottom: 16 },
  modalInput: { borderWidth: 1, borderColor: '#d1d5db', borderRadius: 10, padding: 14, fontSize: 16, marginBottom: 18 },
  modalBotoes: { flexDirection: 'row', justifyContent: 'flex-end', gap: 12 },
  modalBtnCancelar: { paddingVertical: 12, paddingHorizontal: 18 },
  modalBtnCancelarTexto: { color: '#6b7280', fontWeight: 'bold', fontSize: 15 },
  modalBtnConfirmar: { backgroundColor: '#2563eb', paddingVertical: 12, paddingHorizontal: 22, borderRadius: 8 },
  modalBtnConfirmarTexto: { color: 'white', fontWeight: 'bold', fontSize: 15 },
});
