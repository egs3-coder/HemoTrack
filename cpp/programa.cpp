// Interface alternativa: terminal (iostream/stdio) e as mesmas telas via HTTP.
// As estruturas e os algoritmos continuam em ../c/rota_vital.c, com malloc/free.
#include <iostream>
#include <stdio.h>
#include <fstream>
#include <sstream>
#include <mutex>
#include <limits>
#include <climits>
#include <cstring>
#include <functional>
#include "terceiros/httplib.h"
#include "terceiros/json.hpp"
extern "C" {
#include "../c/rota_vital.h"
}
using Json = nlohmann::json;
struct Erro { int status; std::string mensagem; };
const char *tipos[] = {"O_NEG","O_POS","A_NEG","A_POS","B_NEG","B_POS","AB_NEG","AB_POS"};
const char *componentes[] = {"CONCENTRADO_HEMACIAS","PLAQUETAS","PLASMA"};
const char *urgencias[] = {"NORMAL","URGENTE","EMERGENCIA"};
int inteiro(const Json &j, const char *campo) {
    const auto &v = j.at(campo);
    if (!v.is_number_integer() || v.get<long long>() < INT_MIN || v.get<long long>() > INT_MAX)
        throw Erro{400,"Informe um inteiro válido."};
    return v.get<int>();
}
int enumerado(const Json &j, const char *campo, const char *const *opcoes, int tamanho) {
    std::string valor = j.at(campo).get<std::string>();
    for (int i = 0; i < tamanho; i++) if (valor == opcoes[i]) return i;
    throw Erro{400,"Valor de enumeração inválido."};
}
template<size_t N> void texto(const Json &j, const char *campo, char (&destino)[N]) {
    std::string valor = j.at(campo).get<std::string>();
    if (valor.size() >= N || valor.find('\0') != std::string::npos) throw Erro{400,"Texto excede o limite ou contém caractere nulo."};
    std::memcpy(destino, valor.c_str(), valor.size()+1);
}
Bolsa ler_bolsa(const Json &j) {
    Bolsa b{};b.id=inteiro(j,"id");b.volume_ml=inteiro(j,"volumeMl");
    b.tipo=static_cast<TipoSanguineo>(enumerado(j,"tipo",tipos,8));
    b.hemocomponente=static_cast<Hemocomponente>(enumerado(j,"hemocomponente",componentes,3));
    texto(j,"codigo",b.codigo);texto(j,"dataColeta",b.data_coleta);texto(j,"dataValidade",b.data_validade);return b;
}
Hospital ler_hospital(const Json &j) {Hospital h{};h.id=inteiro(j,"id");texto(j,"nome",h.nome);texto(j,"cidade",h.cidade);return h;}
Requisicao ler_requisicao(const Json &j) {
    Requisicao r{};r.id=inteiro(j,"id");r.hospital_id=inteiro(j,"hospitalId");r.quantidade=inteiro(j,"quantidade");
    r.tipo=static_cast<TipoSanguineo>(enumerado(j,"tipo",tipos,8));
    r.hemocomponente=static_cast<Hemocomponente>(enumerado(j,"hemocomponente",componentes,3));
    r.urgencia=static_cast<Urgencia>(enumerado(j,"urgencia",urgencias,3));return r;
}
Json json_bolsa(Bolsa b) {return {{"id",b.id},{"tipo",tipos[b.tipo]},{"volumeMl",b.volume_ml},{"codigo",b.codigo},{"hemocomponente",componentes[b.hemocomponente]},{"dataColeta",b.data_coleta},{"dataValidade",b.data_validade}};}
Json json_hospital(Hospital h) {return {{"id",h.id},{"nome",h.nome},{"cidade",h.cidade}};}
Json json_requisicao(Requisicao r) {return {{"id",r.id},{"hospitalId",r.hospital_id},{"tipo",tipos[r.tipo]},{"quantidade",r.quantidade},{"hemocomponente",componentes[r.hemocomponente]},{"urgencia",urgencias[r.urgencia]}};}
Json json_operacao(Operacao o) {return {{"requisicaoId",o.requisicao_id},{"hospitalId",o.hospital_id},{"tipo",tipos[o.tipo]},{"quantidade",o.quantidade}};}
Json json_arvore(const NoHospital *no) {
    if (!no) return nullptr;
    return {{"hospital",json_hospital(no->dado)},{"esquerda",json_arvore(no->esquerda)},{"direita",json_arvore(no->direita)}};
}
void visitar_hospital(Hospital hospital, void *contexto) {static_cast<Json*>(contexto)->push_back(json_hospital(hospital));}
void conferir(Resultado resultado) {
    if (resultado==SUCESSO)return;
    if (resultado==NAO_ENCONTRADO)throw Erro{404,"Registro não encontrado ou estrutura vazia."};
    if (resultado==DUPLICADO)throw Erro{409,"ID ou código duplicado."};
    if (resultado==SEM_MEMORIA)throw Erro{503,"Sem memória. Operação não realizada."};
    throw Erro{400,"Dados inválidos. Confira os campos e a quantidade de bolsas."};
}
class Aplicacao {
    RotaVital rota{};
    std::mutex trava;
    static int id_caminho(const std::string &caminho) {
        std::string trecho=caminho.substr(caminho.rfind('/')+1);size_t usados=0;
        int id=std::stoi(trecho,&usados);if(usados!=trecho.size())throw Erro{400,"ID inválido."};return id;
    }
    void validar_primeira(int esperado) {
        Requisicao r;conferir(fila_consultar(&rota.requisicoes,&r));
        if(r.id!=esperado)throw Erro{409,"A primeira requisição mudou. Atualize a tela."};
    }
public:
    ~Aplicacao(){rota_liberar(&rota);}
    Json responder(const std::string &metodo,const std::string &caminho,const Json &dados,int &status) {
        std::lock_guard<std::mutex> bloqueio(trava);status=200;
        if(metodo=="GET" && caminho=="/api/estado") {
            Json bolsas=Json::array(),fila=Json::array(),pilha=Json::array(),hospitais=Json::array();
            for(NoBolsa *n=rota.estoque.inicio;n;n=n->proximo)bolsas.push_back(json_bolsa(n->dado));
            for(NoRequisicao *n=rota.requisicoes.inicio;n;n=n->proximo)fila.push_back(json_requisicao(n->dado));
            for(NoOperacao *n=rota.historico.topo;n;n=n->proximo)pilha.push_back(json_operacao(n->dado));
            arvore_percorrer(&rota.hospitais,EM_ORDEM,visitar_hospital,&hospitais);
            return {{"bolsas",bolsas},{"requisicoes",fila},{"historico",pilha},{"hospitais",hospitais},{"arvore",json_arvore(rota.hospitais.raiz)}};
        }
        if(metodo=="POST" && caminho=="/api/bolsas") {Bolsa b=ler_bolsa(dados);conferir(lista_inserir(&rota.estoque,b));status=201;return json_bolsa(b);}
        if(metodo=="POST" && caminho=="/api/bolsas/lote") {
            Bolsa modelo=ler_bolsa(dados.at("modelo"));int quantidade=inteiro(dados,"quantidade");
            conferir(lista_inserir_lote(&rota.estoque,modelo,quantidade));status=201;Json saida=Json::array();
            for(int i=0;i<quantidade;i++){Bolsa b;conferir(lista_consultar(&rota.estoque,modelo.id+i,&b));saida.push_back(json_bolsa(b));}return saida;
        }
        if(caminho.rfind("/api/bolsas/",0)==0 && (metodo=="GET"||metodo=="DELETE")) {
            Bolsa b;int id=id_caminho(caminho);conferir(metodo=="GET"?lista_consultar(&rota.estoque,id,&b):lista_remover(&rota.estoque,id,&b));return json_bolsa(b);
        }
        if(metodo=="POST" && caminho=="/api/hospitais") {Hospital h=ler_hospital(dados);conferir(arvore_inserir(&rota.hospitais,h));status=201;return json_hospital(h);}
        if(metodo=="GET" && caminho.rfind("/api/hospitais/percurso",0)==0) {
            Ordem ordem=EM_ORDEM;std::string valor=dados.value("ordem",std::string("EM_ORDEM"));
            if(valor=="PRE_ORDEM")ordem=PRE_ORDEM;else if(valor=="POS_ORDEM")ordem=POS_ORDEM;else if(valor!="EM_ORDEM")throw Erro{400,"Ordem inválida."};
            Json lista=Json::array();arvore_percorrer(&rota.hospitais,ordem,visitar_hospital,&lista);return lista;
        }
        if(caminho.rfind("/api/hospitais/",0)==0 && (metodo=="GET"||metodo=="DELETE")) {
            Hospital h;int id=id_caminho(caminho);conferir(arvore_consultar(&rota.hospitais,id,&h));
            if(metodo=="DELETE") {Resultado r=rota_remover_hospital(&rota,id,&h);if(r==INVALIDO)throw Erro{409,"Hospital possui referências em pedidos ou histórico."};conferir(r);}return json_hospital(h);
        }
        if(metodo=="POST" && caminho=="/api/requisicoes") {Requisicao r=ler_requisicao(dados);conferir(rota_solicitar(&rota,r));status=201;return json_requisicao(r);}
        if(metodo=="GET" && caminho=="/api/requisicoes/primeira") {Requisicao r;conferir(fila_consultar(&rota.requisicoes,&r));return json_requisicao(r);}
        if(metodo=="DELETE" && caminho.rfind("/api/requisicoes/primeira/",0)==0) {validar_primeira(id_caminho(caminho));Requisicao r;conferir(fila_remover(&rota.requisicoes,&r));return json_requisicao(r);}
        if(metodo=="POST" && caminho=="/api/requisicoes/concluir") {
            int esperado=inteiro(dados,"requisicaoId");validar_primeira(esperado);
            const auto &valores=dados.at("bolsasIds");if(!valores.is_array())throw Erro{400,"Informe os IDs das bolsas."};
            Requisicao r;conferir(fila_consultar(&rota.requisicoes,&r));
            if(valores.size()!=static_cast<size_t>(r.quantidade))throw Erro{400,"Quantidade de IDs diferente da requisição."};
            // Buffer de transporte; nós do domínio continuam alocados por malloc no núcleo C.
            int *ids=static_cast<int*>(malloc(sizeof(int)*valores.size()));if(!ids)throw Erro{503,"Sem memória."};
            Resultado resultado;
            Operacao operacao;
            try {for(size_t i=0;i<valores.size();i++)ids[i]=inteiro(Json{{"id",valores[i]}},"id");resultado=rota_atender(&rota,esperado,ids,valores.size(),&operacao);}
            catch(...){free(ids);throw;}
            free(ids);conferir(resultado);return json_operacao(operacao);
        }
        if(metodo=="GET" && caminho=="/api/historico/topo") {Operacao o;conferir(pilha_consultar(&rota.historico,&o));return json_operacao(o);}
        if(metodo=="DELETE" && caminho.rfind("/api/historico/topo/",0)==0) {
            Operacao o;conferir(pilha_consultar(&rota.historico,&o));if(o.requisicao_id!=id_caminho(caminho))throw Erro{409,"O topo mudou. Atualize a tela."};
            conferir(pilha_remover(&rota.historico,&o));return json_operacao(o);
        }
        throw Erro{404,"Operação não encontrada."};
    }
};
std::string ler_texto(const std::string &mensagem) {std::cout<<mensagem;std::string valor;if(!std::getline(std::cin,valor))throw std::runtime_error("entrada encerrada");return valor;}
int ler_numero(const std::string &mensagem) {
    for(;;){std::string s=ler_texto(mensagem);std::istringstream entrada(s);int n;char sobra;if((entrada>>n)&&!(entrada>>sobra))return n;printf("Digite um inteiro valido.\n");}
}
std::string escolher(const char *const *valores,int quantidade) {
    for(int i=0;i<quantidade;i++) { std::cout<<i+1<<" "<<valores[i]<<"  "; }
    std::cout<<"\n";
    for(;;){int n=ler_numero("Escolha: ");if(n>=1&&n<=quantidade)return valores[n-1];std::cout<<"Opcao invalida.\n";}
}
void terminal(Aplicacao &app) {
    std::cout<<"HemoTrack / Rota Vital - Unidade 1 - C/C++\n";
    for(;;){
        printf("\n1 Hospital: cadastrar | 2 Bolsa/lote: cadastrar | 3 Estoque: listar\n4 Bolsa: consultar | 5 Bolsa: remover | 6 Requisicao: inserir\n7 Fila: consultar | 8 Concluir primeira | 9 Cancelar primeira\n10 Historico: listar | 11 Historico: retirar topo | 12 Hospitais: listar\n13 Hospital: consultar | 14 Hospital: remover | 15 Percurso recursivo\n16 Todas as estruturas | 0 Sair\n");
        int opcao;try{opcao=ler_numero("Opcao: ");}catch(...){break;}if(opcao==0)break;
        try {
            Json dados=Json::object();std::string metodo="GET",caminho="/api/estado";
            if(opcao==1){metodo="POST";caminho="/api/hospitais";dados["id"]=ler_numero("ID: ");dados["nome"]=ler_texto("Nome: ");dados["cidade"]=ler_texto("Cidade: ");}
            else if(opcao==2){metodo="POST";caminho="/api/bolsas/lote";Json b; b["id"]=ler_numero("ID inicial: ");b["codigo"]=ler_texto("Codigo-base: ");dados["quantidade"]=ler_numero("Quantidade (1 a 1000): ");b["hemocomponente"]=escolher(componentes,3);b["tipo"]=escolher(tipos,8);b["volumeMl"]=ler_numero("Volume ml: ");b["dataColeta"]=ler_texto("Coleta AAAA-MM-DD: ");b["dataValidade"]=ler_texto("Validade AAAA-MM-DD: ");dados["modelo"]=b;}
            else if(opcao==4||opcao==5){caminho="/api/bolsas/"+std::to_string(ler_numero("ID da bolsa: "));if(opcao==5)metodo="DELETE";}
            else if(opcao==6){metodo="POST";caminho="/api/requisicoes";dados["id"]=ler_numero("ID da requisicao: ");dados["hospitalId"]=ler_numero("ID do hospital: ");dados["hemocomponente"]=escolher(componentes,3);dados["tipo"]=escolher(tipos,8);dados["quantidade"]=ler_numero("Quantidade: ");dados["urgencia"]=escolher(urgencias,3);}
            else if(opcao==8){metodo="POST";caminho="/api/requisicoes/concluir";dados["requisicaoId"]=ler_numero("ID da primeira requisicao: ");int n=ler_numero("Quantos IDs de bolsas serao informados? ");if(n<1||n>1000)throw Erro{400,"Use de 1 a 1000 IDs."};dados["bolsasIds"]=Json::array();for(int i=0;i<n;i++)dados["bolsasIds"].push_back(ler_numero("ID da bolsa: "));}
            else if(opcao==9){metodo="DELETE";caminho="/api/requisicoes/primeira/"+std::to_string(ler_numero("ID da primeira requisicao: "));}
            else if(opcao==11){metodo="DELETE";caminho="/api/historico/topo/"+std::to_string(ler_numero("ID da requisicao no topo: "));}
            else if(opcao==13||opcao==14){caminho="/api/hospitais/"+std::to_string(ler_numero("ID do hospital: "));if(opcao==14)metodo="DELETE";}
            else if(opcao==15){caminho="/api/hospitais/percurso";const char *ordens[]={"PRE_ORDEM","EM_ORDEM","POS_ORDEM"};dados["ordem"]=escolher(ordens,3);}
            else if(opcao!=3&&opcao!=7&&opcao!=10&&opcao!=12&&opcao!=16){std::cout<<"Opcao invalida.\n";continue;}
            int status;Json saida=app.responder(metodo,caminho,dados,status);
            if(opcao==3)saida=saida.at("bolsas");else if(opcao==7)saida=saida.at("requisicoes");else if(opcao==10)saida=saida.at("historico");else if(opcao==12)saida=saida.at("hospitais");
            std::cout<<saida.dump(2)<<"\n";
        }catch(const Erro &e){std::cout<<"Erro "<<e.status<<": "<<e.mensagem<<"\n";}
        catch(const std::exception &e){if(!std::cin)break;std::cout<<"Entrada invalida: "<<e.what()<<"\n";}
    }
    std::cout<<"Programa encerrado. Memoria das estruturas sera liberada.\n";
}
int main(int argc,char **argv) {
    Aplicacao app;int porta=8081;std::string pasta="src/main/resources/static";
    for(int i=1;i<argc;i++){std::string arg=argv[i];if(arg=="--terminal"){terminal(app);return 0;}if(arg=="--porta"&&i+1<argc)porta=std::stoi(argv[++i]);else if(arg=="--static"&&i+1<argc)pasta=argv[++i];}
    httplib::Server servidor;
    servidor.set_payload_max_length(1024*1024);
    servidor.set_read_timeout(10,0);
    auto rota=[&](const httplib::Request &req,httplib::Response &res){
        try {Json dados=req.body.empty()?Json::object():Json::parse(req.body);if(req.has_param("ordem"))dados["ordem"]=req.get_param_value("ordem");int status;Json saida=app.responder(req.method,req.path,dados,status);res.status=status;res.set_content(saida.dump(),"application/json; charset=utf-8");}
        catch(const Erro &e){res.status=e.status;res.set_content(Json{{"mensagem",e.mensagem}}.dump(),"application/json; charset=utf-8");}
        catch(const std::exception &){res.status=400;res.set_content(Json{{"mensagem","JSON ou campos invalidos."}}.dump(),"application/json; charset=utf-8");}
    };
    servidor.Get(R"(/api/.*)",rota);servidor.Post(R"(/api/.*)",rota);servidor.Delete(R"(/api/.*)",rota);
    if(!servidor.set_mount_point("/",pasta)){std::cerr<<"Pasta de telas nao encontrada: "<<pasta<<"\n";return 1;}
    printf("HemoTrack U1 - nucleo C, interface C++\nAbra http://127.0.0.1:%d\n",porta);fflush(stdout);
    if(!servidor.listen("127.0.0.1",porta)){std::cerr<<"Nao foi possivel abrir a porta.\n";return 1;}
}
