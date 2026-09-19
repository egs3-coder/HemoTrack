'use strict';
const principal = document.querySelector('#principal');
const modal = document.querySelector('#modal');
const formulario = document.querySelector('#formulario');
const tipos = ['O_NEG','O_POS','A_NEG','A_POS','B_NEG','B_POS','AB_NEG','AB_POS'];
const nomesTipos = ['O−','O+','A−','A+','B−','B+','AB−','AB+'];
const componentes = ['CONCENTRADO_HEMACIAS','PLAQUETAS','PLASMA'];
const nomesComponentes = ['Concentrado de hemácias','Plaquetas','Plasma'];
const nomeComponente = c => nomesComponentes[componentes.indexOf(c)] || escapar(c);
const paginas = ['visao','estoque','requisicoes','hospitais','historico','estruturas'];
const titulos = ['Visão geral','Estoque de bolsas','Requisições','Hospitais','Histórico','Estruturas de dados'];
let estado = {bolsas:[],requisicoes:[],hospitais:[],historico:[],arvore:null};
let pagina = 'visao', acaoFormulario = null, emEnvio = false, carregado = false;
const escapar = valor => String(valor).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const nomeTipo = tipo => nomesTipos[tipos.indexOf(tipo)] || escapar(tipo);
const hospital = id => estado.hospitais.find(h => h.id === id)?.nome || `Hospital ${id}`;
function aviso(mensagem) { const a=document.querySelector('#aviso');a.textContent=mensagem;a.style.display='block';clearTimeout(aviso.tempo);aviso.tempo=setTimeout(()=>a.style.display='none',5500); }
async function api(caminho,metodo='GET',dados) {
 const resposta=await fetch('/api'+caminho,{method:metodo,headers:{'Content-Type':'application/json'},body:dados===undefined?undefined:JSON.stringify(dados)});
 const corpo=await resposta.json();if(!resposta.ok)throw new Error(corpo.mensagem||'Não foi possível concluir a operação.');return corpo;
}
async function atualizar() {estado=await api('/estado');carregado=true;document.querySelector('#conexao').textContent='● Aplicação conectada';renderizar();}
function cabecalho(titulo,subtitulo,botoes='') {return `<div class="titulo"><div><span class="sobretitulo">HEMOCENTRO · UNIDADE 1</span><h1>${titulo}</h1><p class="subtitulo">${subtitulo}</p></div><div class="acoes">${botoes}</div></div>`;}
function botao(texto,acao,secundario=false) {return `<button class="botao ${secundario?'secundario':''}" data-acao="${acao}">${texto}</button>`;}
function vazio(titulo,texto,acao='',rotulo='') {return `<div class="vazio"><strong>${titulo}</strong><p>${texto}</p>${acao?botao(rotulo,acao):''}</div>`;}
function painel(titulo,subtitulo,conteudo,extra='') {return `<section class="painel"><div class="painel-topo"><div><h2>${titulo}</h2><p>${subtitulo}</p></div>${extra}</div>${conteudo}</section>`;}
function tabela(colunas,linhas) {return `<div class="tabela-scroll"><table><thead><tr>${colunas.map(c=>`<th>${c}</th>`).join('')}</tr></thead><tbody>${linhas.join('')}</tbody></table></div>`;}
function verVisao() {
 const total=estado.bolsas.length, pendentes=estado.requisicoes.reduce((n,r)=>n+r.quantidade,0);
 const metricas=[['Bolsas disponíveis',total,'unidades no estoque','▤'],['Requisições pendentes',estado.requisicoes.length,`${pendentes} bolsas solicitadas`,'⇄'],['Hospitais cadastrados',estado.hospitais.length,'unidades participantes','✚'],['Registros no histórico',estado.historico.length,'conclusões armazenadas','◷']];
 const blocos=tipos.map(t=>{const n=estado.bolsas.filter(b=>b.tipo===t).length;return `<div class="tipo"><span>${nomeTipo(t)}</span><strong>${n}</strong><small>bolsa${n===1?'':'s'} em estoque</small><div class="trilho"><i style="width:${total?100*n/total:0}%"></i></div></div>`;}).join('');
 const fila=estado.requisicoes.length?`<div class="lista-pedidos">${estado.requisicoes.slice(0,4).map((r,i)=>`<div class="pedido"><span class="numero">${i+1}</span><div class="detalhe"><strong>${escapar(hospital(r.hospitalId))}</strong><small>Requisição #${r.id} · ${r.quantidade} bolsa(s)</small></div><span class="etiqueta">${nomeTipo(r.tipo)}</span></div>`).join('')}</div>`:vazio('Nenhuma requisição pendente','Cadastre um hospital e adicione o primeiro pedido.');
 return cabecalho('Cada bolsa, sob controle.','Acompanhe o estoque e organize as requisições em um só lugar.',botao('+ Nova requisição','nova-requisicao',true)+botao('+ Cadastrar bolsa','nova-bolsa'))+
 `<div class="metricas">${metricas.map(m=>`<article class="metrica"><header>${m[0]}<span class="simbolo">${m[3]}</span></header><strong>${m[1]}</strong><small>${m[2]}</small></article>`).join('')}</div>`+
 `<div class="grade">${painel('Disponibilidade por tipo','Contagem das bolsas cadastradas',`<div class="painel-corpo tipos">${blocos}</div>`,'<span class="etiqueta verde">Estoque atual</span>')}${painel('Próximas requisições','Ordem de chegada preservada',fila,'<button class="texto-botao" data-pagina="requisicoes">Ver todas →</button>')}</div>`+
 `<div class="chamada"><div><h3>${estado.hospitais.length?'Explore como os dados são organizados':'Comece pelo cadastro de um hospital'}</h3><p>${estado.hospitais.length?'Veja lista, fila, pilha e árvore com os dados desta execução.':'Cadastre seus dados ou carregue um cenário de exemplo para testar a aplicação.'}</p></div>${estado.hospitais.length?'<button class="botao secundario" data-pagina="estruturas">Explorar estruturas →</button>':botao('Carregar exemplo','exemplo')}</div>`;
}
function verEstoque() {
 const linhas=estado.bolsas.map(b=>`<tr><td><strong>${escapar(b.codigo)}</strong><br><small>ID #${b.id}</small></td><td>${nomeComponente(b.hemocomponente)}</td><td><span class="etiqueta">${nomeTipo(b.tipo)}</span></td><td>${b.volumeMl} ml</td><td>${escapar(b.dataColeta)}</td><td>${escapar(b.dataValidade)}</td><td><span class="etiqueta verde">Disponível</span></td><td><button class="texto-botao" data-acao="remover-bolsa" data-id="${b.id}">Remover</button></td></tr>`);
 return cabecalho('Estoque de bolsas','Cadastre, consulte e remova unidades do estoque.',botao('+ Cadastrar bolsa','nova-bolsa'))+painel('Bolsas disponíveis',`${linhas.length} unidade(s) cadastrada(s)`,linhas.length?tabela(['Código / ID','Hemocomponente','Tipo','Volume','Coleta','Validade','Estado','Ação'],linhas):vazio('O estoque está vazio','Adicione uma bolsa para começar.','nova-bolsa','Cadastrar bolsa'),'<input class="buscar" aria-label="Consultar bolsa por ID" placeholder="Consultar ID da bolsa…" type="number" min="1" id="busca-bolsa"><button class="botao secundario" data-acao="buscar-bolsa">Consultar</button>');
}
function verRequisicoes() {
 const linhas=estado.requisicoes.map((r,i)=>`<tr><td><span class="numero">${i+1}</span></td><td><strong>#${r.id}</strong></td><td>${escapar(hospital(r.hospitalId))}</td><td><span class="etiqueta">${nomeTipo(r.tipo)}</span></td><td>${r.quantidade}</td><td>${nomeComponente(r.hemocomponente)}<br><small>${escapar(r.urgencia)}</small></td><td>${i===0?`<div class="acoes"><button class="botao" data-acao="concluir">Concluir</button><button class="texto-botao" data-acao="cancelar-requisicao" data-id="${r.id}">Cancelar</button></div>`:'<span class="etiqueta cinza">Aguardando a anterior</span>'}</td></tr>`);
 return cabecalho('Requisições hospitalares','A primeira requisição da fila é a próxima a ser processada.',botao('+ Nova requisição','nova-requisicao'))+painel('Fila de requisições',`${linhas.length} pedido(s) pendente(s)`,(linhas.length?tabela(['Posição','Pedido','Hospital','Tipo','Bolsas','Componente / urgência','Ação'],linhas):vazio('Nenhum pedido na fila','Cadastre um hospital e envie uma requisição.','nova-requisicao','Nova requisição'))+'<div class="explicacao">A conclusão usa bolsas selecionadas manualmente. Esta simulação não valida compatibilidade clínica.</div>');
}
function verHospitais() {
 const linhas=estado.hospitais.map(h=>`<tr><td><strong>#${h.id}</strong></td><td>${escapar(h.nome)}<br><small>${escapar(h.cidade)}</small></td><td>${estado.requisicoes.filter(r=>r.hospitalId===h.id).length}</td><td><button class="texto-botao" data-acao="remover-hospital" data-id="${h.id}">Remover</button></td></tr>`);
 return cabecalho('Hospitais','Organize as unidades que solicitam bolsas ao hemocentro.',botao('+ Cadastrar hospital','novo-hospital'))+painel('Hospitais cadastrados','Ordenados pelo identificador',linhas.length?tabela(['Identificador','Nome','Pedidos pendentes','Ação'],linhas):vazio('Nenhum hospital cadastrado','O cadastro é necessário antes de criar uma requisição.','novo-hospital','Cadastrar hospital'));
}
function verHistorico() {
 const linhas=estado.historico.map((o,i)=>`<tr><td><span class="etiqueta ${i===0?'':'cinza'}">${i===0?'Mais recente':'Anterior'}</span></td><td>#${o.requisicaoId}</td><td>${escapar(hospital(o.hospitalId))}</td><td>${o.quantidade}</td><td><span class="etiqueta">${nomeTipo(o.tipo)}</span></td><td>${i===0?`<button class="texto-botao" data-acao="remover-historico" data-id="${o.requisicaoId}">Remover registro</button>`:'—'}</td></tr>`);
 return cabecalho('Histórico de operações','Consulte os registros de conclusão, do mais recente ao mais antigo.')+painel('Conclusões registradas',`${linhas.length} registro(s) na pilha`,(linhas.length?tabela(['Ordem','Requisição','Hospital','Bolsas','Tipo solicitado','Ação'],linhas):vazio('O histórico está vazio','Conclua a primeira requisição para registrar uma operação.'))+'<div class="explicacao">Remover o registro do topo não desfaz a conclusão nem devolve bolsas ao estoque.</div>');
}
function nos(valores,rotulo) {return valores.length?`<div class="fluxo">${valores.map((v,i)=>`${i?'<span class="seta">→</span>':''}<div class="no"><small>${i===0?rotulo:'nó'}</small>#${v}</div>`).join('')}</div>`:vazio('Estrutura vazia','Cadastre dados para visualizar os encadeamentos.');}
function ramos(r,lado='Raiz') {return r?`<li><span class="ramo"><small>${lado}</small><b>#${r.hospital.id}</b> ${escapar(r.hospital.nome)}</span>${r.esquerda||r.direita?`<ul>${ramos(r.esquerda,'Esquerda')}${ramos(r.direita,'Direita')}</ul>`:''}</li>`:'';}
function verEstruturas() {
 return cabecalho('Estruturas em funcionamento','Visualização didática dos dados mantidos pelo servidor da aplicação.')+
 painel('Lista encadeada · estoque','Inserção no início; consulta e remoção por ID.',nos(estado.bolsas.map(b=>b.id),'início'))+
 `<div class="grade">${painel('Fila · requisições','FIFO: entra no fim e sai do início.',nos(estado.requisicoes.map(r=>r.id),'início'))}${painel('Pilha · histórico','LIFO: consulta e remoção pelo topo.',nos(estado.historico.map(o=>o.requisicaoId),'topo'))}</div>`+
 painel('Árvore binária de busca · hospitais','Menores à esquerda; maiores à direita. Percursos recursivos.',estado.arvore?`<div class="arvore"><ul>${ramos(estado.arvore)}</ul></div><div class="explicacao" id="resultado-percurso">Em-ordem: ${estado.hospitais.map(h=>h.id).join(' → ')}</div>`:vazio('Árvore vazia','Cadastre hospitais com IDs diferentes para visualizar a árvore.'),'<div class="acoes"><select id="ordem" aria-label="Ordem do percurso"><option value="EM_ORDEM">Em-ordem</option><option value="PRE_ORDEM">Pré-ordem</option><option value="POS_ORDEM">Pós-ordem</option></select><button class="botao secundario" data-acao="percorrer">Percorrer</button></div>');
}
function renderizar() { const telas=[verVisao,verEstoque,verRequisicoes,verHospitais,verHistorico,verEstruturas];principal.innerHTML=telas[paginas.indexOf(pagina)]();document.querySelector('#caminho').textContent=titulos[paginas.indexOf(pagina)];document.querySelectorAll('nav button').forEach(b=>b.classList.toggle('ativo',b.dataset.pagina===pagina)); }
function navegar(destino) {pagina=paginas.includes(destino)?destino:'visao';location.hash=pagina;if(carregado)renderizar();}
function campoNumero(nome,rotulo) {return `<label>${rotulo}<input name="${nome}" type="number" min="1" max="2147483647" step="1" required></label>`;}
function campoComponente(){return `<label>Hemocomponente<select name="hemocomponente">${componentes.map((c,i)=>`<option value="${c}">${nomesComponentes[i]}</option>`).join('')}</select></label>`;}
function campoTipo(){return `<label>Tipo sanguíneo informado<select name="tipo">${tipos.map((t,i)=>`<option value="${t}">${nomesTipos[i]}</option>`).join('')}</select></label>`;}
function abrir(titulo,campos,acao,rotulo='Salvar') {document.querySelector('#titulo-modal').textContent=titulo;document.querySelector('#campos').innerHTML=campos;document.querySelector('#erro-form').textContent='';document.querySelector('#salvar').textContent=rotulo;acaoFormulario=acao;modal.showModal();}
function fechar(){if(!emEnvio)modal.close();}
async function executar(acao,id) {
 if(acao==='nova-bolsa') abrir('Nova bolsa / lote',campoNumero('id','ID inicial da bolsa')+'<label>Código-base<input name="codigo" required maxlength="40" placeholder="Ex.: BOLSA-2026"></label>'+campoNumero('quantidade','Quantidade no lote (1 a 1000)')+campoComponente()+campoTipo()+campoNumero('volumeMl','Volume por bolsa (ml)')+'<label>Data de coleta<input type="date" name="dataColeta" required></label><label>Data de validade<input type="date" name="dataValidade" required></label><p class="ajuda">Os IDs serão sequenciais. Em lotes, os códigos recebem -001, -002 etc. Datas são metadados; não há FEFO.</p>',dados=>api('/bolsas/lote','POST',{quantidade:+dados.get('quantidade'),modelo:{id:+dados.get('id'),codigo:dados.get('codigo'),hemocomponente:dados.get('hemocomponente'),tipo:dados.get('tipo'),volumeMl:+dados.get('volumeMl'),dataColeta:dados.get('dataColeta'),dataValidade:dados.get('dataValidade')}}));
 if(acao==='novo-hospital') abrir('Cadastrar hospital',campoNumero('id','Identificador do hospital')+'<label>Nome do hospital<input name="nome" required maxlength="79" autocomplete="organization"></label><label>Cidade<input name="cidade" required maxlength="59"></label>',dados=>api('/hospitais','POST',{id:+dados.get('id'),nome:dados.get('nome'),cidade:dados.get('cidade')}));
 if(acao==='nova-requisicao') {
  if(!estado.hospitais.length){aviso('Cadastre um hospital antes de criar uma requisição.');navegar('hospitais');return;}
  abrir('Nova requisição',campoNumero('id','Identificador da requisição')+`<label>Hospital<select name="hospitalId">${estado.hospitais.map(h=>`<option value="${h.id}">${escapar(h.nome)} (#${h.id})</option>`).join('')}</select></label>`+campoComponente()+campoTipo()+campoNumero('quantidade','Quantidade de bolsas')+'<label>Urgência informada<select name="urgencia"><option>NORMAL</option><option>URGENTE</option><option>EMERGENCIA</option></select></label><p class="ajuda">A urgência não muda a ordem FIFO nesta unidade.</p>',d=>api('/requisicoes','POST',{id:+d.get('id'),hospitalId:+d.get('hospitalId'),tipo:d.get('tipo'),quantidade:+d.get('quantidade'),hemocomponente:d.get('hemocomponente'),urgencia:d.get('urgencia')}));
 }
 if(acao==='concluir') {
  const r=estado.requisicoes[0];if(!r)return;
  abrir(`Concluir requisição #${r.id}`,`<p class="ajuda">Selecione exatamente ${r.quantidade} bolsa(s). Seleção manual para simulação, sem validação clínica.</p><div class="opcoes-bolsas">${estado.bolsas.length?estado.bolsas.map(b=>`<label class="selecionar-bolsa"><input type="checkbox" name="bolsasIds" value="${b.id}">#${b.id} · ${nomeTipo(b.tipo)} · ${b.volumeMl} ml</label>`).join(''):'<p class="ajuda">Não há bolsas no estoque. Cancele e cadastre as unidades necessárias.</p>'}</div>`,d=>{const ids=d.getAll('bolsasIds').map(Number);if(ids.length!==r.quantidade)throw new Error(`Selecione exatamente ${r.quantidade} bolsa(s).`);return api('/requisicoes/concluir','POST',{requisicaoId:r.id,bolsasIds:ids});},'Concluir pedido');
 }
 const removiveis=['remover-bolsa','remover-hospital','cancelar-requisicao','remover-historico'];
 if(removiveis.includes(acao)) {
  const indice=removiveis.indexOf(acao), caminhos=[`/bolsas/${id}`,`/hospitais/${id}`,`/requisicoes/primeira/${id}`,`/historico/topo/${id}`];
  const descricoes=['A bolsa será retirada do estoque.','Hospitais referenciados por pedidos ou histórico não podem ser removidos.','Somente a primeira requisição será cancelada.','O registro será removido. Isso não desfaz a conclusão nem repõe o estoque.'];
  abrir('Confirmar operação',`<p class="ajuda">${descricoes[indice]}</p>`,()=>api(caminhos[indice],'DELETE'),'Confirmar');
 }
 if(acao==='buscar-bolsa'){const idBusca=+document.querySelector('#busca-bolsa').value;if(!Number.isInteger(idBusca)||idBusca<=0)throw new Error('Informe um ID positivo.');const b=await api('/bolsas/'+idBusca);aviso(`Bolsa #${b.id}: ${nomeTipo(b.tipo)}, ${b.volumeMl} ml, disponível.`);}
 if(acao==='percorrer'){const ordem=document.querySelector('#ordem').value;const lista=await api('/hospitais/percurso?ordem='+ordem);const alvo=document.querySelector('#resultado-percurso');if(alvo)alvo.textContent=ordem+': '+lista.map(h=>h.id).join(' → ');else aviso('A árvore está vazia.');}
 if(acao==='exemplo'){
  if(estado.hospitais.length||estado.bolsas.length||estado.requisicoes.length||estado.historico.length)throw new Error('O exemplo só pode ser carregado em uma execução vazia.');
  for(const h of [{id:50,nome:'Hospital Central'},{id:30,nome:'Hospital Municipal'},{id:70,nome:'Hospital Universitário'},{id:20,nome:'Hospital Norte'},{id:40,nome:'Hospital Sul'},{id:60,nome:'Hospital Regional'},{id:80,nome:'Hospital Leste'}])await api('/hospitais','POST',{...h,cidade:'Recife'});
  for(const b of [{id:101,tipo:'O_POS',volumeMl:450},{id:102,tipo:'A_POS',volumeMl:450},{id:103,tipo:'O_POS',volumeMl:450}])await api('/bolsas','POST',{...b,codigo:'B-'+b.id,hemocomponente:'CONCENTRADO_HEMACIAS',dataColeta:'2026-09-01',dataValidade:'2026-10-01'});
  await api('/requisicoes','POST',{id:1,hospitalId:50,tipo:'O_POS',quantidade:2,hemocomponente:'CONCENTRADO_HEMACIAS',urgencia:'NORMAL'});
  await api('/requisicoes','POST',{id:2,hospitalId:30,tipo:'A_POS',quantidade:1,hemocomponente:'CONCENTRADO_HEMACIAS',urgencia:'URGENTE'});await atualizar();aviso('Cenário de exemplo carregado.');
 }
}
document.addEventListener('click',async e=>{const alvo=e.target.closest('[data-pagina],[data-acao]');if(!alvo)return;if(alvo.dataset.pagina){navegar(alvo.dataset.pagina);return;}if(alvo.disabled)return;alvo.disabled=true;try{await executar(alvo.dataset.acao,+alvo.dataset.id);}catch(erro){aviso(erro.message);}finally{alvo.disabled=false;}});
formulario.addEventListener('submit',async e=>{e.preventDefault();if(emEnvio)return;emEnvio=true;document.querySelector('#salvar').disabled=true;try{await acaoFormulario(new FormData(formulario));await atualizar();modal.close();aviso('Operação concluída.');}catch(erro){document.querySelector('#erro-form').textContent=erro.message;}finally{emEnvio=false;document.querySelector('#salvar').disabled=false;}});
document.querySelector('#fechar-modal').onclick=fechar;document.querySelector('#cancelar-modal').onclick=fechar;modal.addEventListener('cancel',e=>{if(emEnvio)e.preventDefault();});
window.addEventListener('hashchange',()=>{pagina=paginas.includes(location.hash.slice(1))?location.hash.slice(1):'visao';if(carregado)renderizar();});
pagina=paginas.includes(location.hash.slice(1))?location.hash.slice(1):'visao';
atualizar().catch(erro=>{document.querySelector('#conexao').textContent='Sem conexão';principal.innerHTML=vazio('Não foi possível carregar a aplicação','Confirme que o Spring Boot está em execução e recarregue esta página.');aviso(erro.message);});
