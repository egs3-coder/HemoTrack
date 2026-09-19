"""Compara as duas aplicações reais. Inicie Java em 8080 e C/C++ em 8081, ambos vazios."""
import json,urllib.request,urllib.error,sys
from pathlib import Path
BASES=sys.argv[1:] or ['http://127.0.0.1:8080','http://127.0.0.1:8081']
contador=0

def chamar(base,metodo,caminho,dados=None):
    corpo=None if dados is None else json.dumps(dados).encode()
    pedido=urllib.request.Request(base+'/api'+caminho,corpo,{'Content-Type':'application/json'},method=metodo)
    try:
        with urllib.request.urlopen(pedido,timeout=15) as r:return r.status,json.load(r)
    except urllib.error.HTTPError as e:return e.code,json.load(e)

def passo(metodo,caminho,dados=None,esperado=200):
    global contador
    resultados=[chamar(base,metodo,caminho,dados) for base in BASES]
    for codigo,corpo in resultados:assert codigo==esperado,(metodo,caminho,codigo,corpo,esperado)
    if esperado<400:assert resultados[0][1]==resultados[1][1],(caminho,resultados)
    estados=[chamar(base,'GET','/estado')[1] for base in BASES]
    assert estados[0]==estados[1],(metodo,caminho,'estados divergentes')
    contador+=1
    return resultados[0][1]

def bolsa(id,codigo=None,tipo='O_POS'):
    return dict(id=id,codigo=codigo or f'B-{id}',tipo=tipo,volumeMl=450,hemocomponente='CONCENTRADO_HEMACIAS',dataColeta='2026-09-01',dataValidade='2026-10-01')

def pedido(id,hospital=50,quantidade=2,urgencia='NORMAL'):
    return dict(id=id,hospitalId=hospital,tipo='A_NEG',quantidade=quantidade,hemocomponente='PLASMA',urgencia=urgencia)

assert all(chamar(b,'GET','/estado')[1]['hospitais']==[] for b in BASES),'Inicie ambos com estado vazio.'
passo('GET','/requisicoes/primeira',esperado=404)
passo('GET','/historico/topo',esperado=404)
for id in [50,30,70,20,40,60,80]:passo('POST','/hospitais',dict(id=id,nome=f'Hospital {id}',cidade='Recife'),201)
passo('POST','/hospitais',dict(id=50,nome='Duplicado',cidade='Recife'),409)
for ordem in ['PRE_ORDEM','EM_ORDEM','POS_ORDEM']:passo('GET','/hospitais/percurso?ordem='+ordem)
for id in [20,30,50]:passo('DELETE',f'/hospitais/{id}')
passo('GET','/hospitais/50',esperado=404)
passo('GET','/hospitais/60')
passo('POST','/bolsas',bolsa(102),201)
passo('POST','/bolsas/lote',dict(modelo=bolsa(100),quantidade=3),409)
passo('POST','/bolsas/lote',dict(modelo=bolsa(200),quantidade=3),201)
passo('POST','/bolsas',bolsa(300,'B-200-003'),409)
passo('POST','/bolsas',{**bolsa(300),'dataColeta':'2026-02-30'},400)
passo('POST','/bolsas',{**bolsa(300),'dataValidade':'2026-08-01'},400)
passo('POST','/bolsas',{**bolsa(300),'volumeMl':0},400)
passo('POST','/bolsas',{**bolsa(300),'hemocomponente':'INVALIDO'},400)
passo('POST','/requisicoes',pedido(1,999),404)
passo('POST','/requisicoes',pedido(1,60),201)
passo('POST','/requisicoes',pedido(2,70,1,'EMERGENCIA'),201)
passo('POST','/requisicoes',pedido(1,60),409)
passo('GET','/requisicoes/primeira')
passo('DELETE','/hospitais/60',esperado=409)
passo('POST','/requisicoes/concluir',dict(requisicaoId=2,bolsasIds=[102]),409)
for ids in [[102],[102,102],[102,999]]:
    passo('POST','/requisicoes/concluir',dict(requisicaoId=1,bolsasIds=ids),404 if 999 in ids else 400)
passo('POST','/requisicoes/concluir',dict(requisicaoId=1,bolsasIds=[102,200]))
passo('GET','/historico/topo')
passo('POST','/requisicoes',pedido(1,60),409)
passo('DELETE','/hospitais/60',esperado=409)
passo('POST','/requisicoes/concluir',dict(requisicaoId=2,bolsasIds=[201]))
passo('DELETE','/historico/topo/1',esperado=409)
passo('DELETE','/historico/topo/2')
passo('GET','/historico/topo')
passo('DELETE','/historico/topo/1')
passo('DELETE','/hospitais/60')
passo('POST','/requisicoes',pedido(3,70,1),201)
passo('DELETE','/requisicoes/primeira/999',esperado=409)
passo('DELETE','/requisicoes/primeira/3')
passo('GET','/bolsas/202')
passo('DELETE','/bolsas/202')
passo('GET','/bolsas/202',esperado=404)
for id in [40,70,80]:passo('DELETE',f'/hospitais/{id}')
assert passo('GET','/estado')==dict(bolsas=[],requisicoes=[],historico=[],hospitais=[],arvore=None)
mensagem=f'{contador} operações HTTP comparadas: mesmos códigos esperados, mesmos dados de sucesso e mesmo estado após cada operação.\n'
Path(__file__).with_name('resultado_equivalencia.txt').write_text(mensagem,encoding='utf-8')
print(mensagem)
