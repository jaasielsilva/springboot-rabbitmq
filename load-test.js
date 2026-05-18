/**
 * Teste de carga / fluxo — webhook local
 *
 * Uso:
 *   node load-test.js              → fluxo realista (1 evento por pedido, em sequência)
 *   node load-test.js stress 20    → 20 pedidos em paralelo (só CONFIRMED)
 *   node load-test.js fluxo 5      → 5 pedidos com ciclo completo CONFIRMED → … → CONCLUDED
 */

const fetch = require('node-fetch');

const URL = process.env.WEBHOOK_URL || 'http://localhost:8080/webhook/ifood';

const FLUXO_COMPLETO = [
  'CONFIRMED',
  'SEPARATION_STARTED',
  'READY_TO_PICKUP',
  'CONCLUDED',
];

async function enviarEvento(orderId, code) {
  const res = await fetch(URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, orderId: String(orderId) }),
  });
  const body = await res.json().catch(() => ({}));
  return { ok: res.ok, status: res.status, body };
}

async function fluxoPedido(orderId, delayMs = 80) {
  for (const code of FLUXO_COMPLETO) {
    const { ok, body } = await enviarEvento(orderId, code);
    const msg = body.message || body.error || '';
    console.log(`  #${orderId} ${code} → ${ok ? 'OK' : 'ERRO'} ${msg}`);
    if (delayMs > 0) await new Promise((r) => setTimeout(r, delayMs));
  }
}

async function apenasNovo(orderId) {
  const { ok, body } = await enviarEvento(orderId, 'CONFIRMED');
  console.log(`  #${orderId} CONFIRMED → ${ok ? 'OK' : 'ERRO'} ${body.message || ''}`);
}

async function modoFluxo(quantidade, baseId = 4000) {
  console.log(`\n📦 Modo FLUXO — ${quantidade} pedido(s), sequência realista\n`);
  for (let i = 0; i < quantidade; i++) {
    const orderId = baseId + i;
    console.log(`Pedido ${orderId}:`);
    await fluxoPedido(orderId);
  }
  console.log('\n✅ Fluxo finalizado.\n');
}

async function modoStress(quantidade, baseId = 5000) {
  console.log(`\n🔥 Modo STRESS — ${quantidade} pedidos CONFIRMED em paralelo\n`);
  const ids = Array.from({ length: quantidade }, (_, i) => baseId + i);
  const results = await Promise.all(ids.map((id) => apenasNovo(id)));
  const ok = results.filter((r) => r?.ok !== false).length;
  console.log(`\n✅ ${ok}/${quantidade} pedidos novos enviados.\n`);
}

async function main() {
  const modo = (process.argv[2] || 'fluxo').toLowerCase();
  const qtd = parseInt(process.argv[3] || '3', 10);

  try {
    if (modo === 'stress') {
      await modoStress(qtd);
    } else if (modo === 'fluxo') {
      await modoFluxo(qtd);
    } else {
      console.log('Modos: fluxo | stress');
      console.log('  node load-test.js fluxo 5');
      console.log('  node load-test.js stress 20');
    }
  } catch (e) {
    console.error('Falha:', e.message);
    console.error('Spring Boot está rodando em', URL, '?');
    process.exit(1);
  }
}

main();
