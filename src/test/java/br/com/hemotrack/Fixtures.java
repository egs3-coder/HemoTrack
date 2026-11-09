package br.com.hemotrack;

import br.com.hemotrack.model.EntidadeBase;
import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.model.Requisicao;
import br.com.hemotrack.model.Rota;
import br.com.hemotrack.model.TipoSanguineo;
import br.com.hemotrack.model.Urgencia;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

/** Dados de apoio usados pelos testes das historias. */
final class Fixtures {

    private Fixtures() {
    }

    /** Simula o id gerado pelo banco. */
    static <T extends EntidadeBase> T comId(T entidade, long id) {
        try {
            Field campo = EntidadeBase.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidade, id);
            return entidade;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static Hospital santaClara() {
        return comId(new Hospital("Hospital Santa Clara", "12345678000190", "Norte"), 1L);
    }

    static Hospital boaVista() {
        return comId(new Hospital("Hospital Boa Vista", "98765432000110", "Sul"), 2L);
    }

    static Requisicao requisicao(TipoSanguineo tipo, int quantidade) {
        return new Requisicao(santaClara(), Hemocomponente.CONCENTRADO_HEMACIAS, tipo, quantidade, Urgencia.EMERGENCIA);
    }

    static Requisicao emTransporte(TipoSanguineo tipo) {
        Requisicao requisicao = requisicao(tipo, 1);
        requisicao.marcarAlocada();
        requisicao.registrarRota(new Rota(List.of("Hemocentro Central", "Hospital Boa Vista", "Hospital Santa Clara"), 18, 9));
        requisicao.despachar(LocalDateTime.now());
        return requisicao;
    }
}
