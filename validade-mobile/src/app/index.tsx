import { useState } from 'react';

import FormCaptura from '@/screens/FormCaptura';
import ScannerScreen from '@/screens/ScannerScreen';

export default function HomeScreen() {
  // Código de barras lido pelo scanner. Enquanto for null, mostra a câmera;
  // quando um código é lido, abre o formulário de captura do lote.
  const [codigoBarras, setCodigoBarras] = useState<string | null>(null);

  if (codigoBarras) {
    return (
      <FormCaptura
        codigoBarras={codigoBarras}
        onVoltar={() => setCodigoBarras(null)}
      />
    );
  }

  return <ScannerScreen onScan={(data: string) => setCodigoBarras(data)} />;
}
