package br.com.hemotrack.algoritmo;

import br.com.hemotrack.model.Hemocomponente;
import br.com.hemotrack.model.TipoSanguineo;

/**
 * ALGORITMO 4 (U2) - Compatibilidade ABO/Rh por MATRIZ 8x8.
 *
 * A matriz e indexada por [doador][receptor], na ordem do enum TipoSanguineo:
 *   0=O-  1=O+  2=A-  3=A+  4=B-  5=B+  6=AB-  7=AB+
 * O indice de cada tipo e o ordinal() do enum, entao a consulta e um acesso direto
 * MATRIZ[doador.ordinal()][receptor.ordinal()]: nenhum laco, nenhuma comparacao de antigeno.
 *
 * Duas matrizes, porque o hemocomponente muda a regra:
 *  - HEMACIAS (concentrado de hemacias e plaquetas): a bolsa leva ANTIGENOS. O doador nao pode
 *    ter antigeno (A, B, Rh) que o receptor nao tenha. O- e doador universal; AB+ e receptor universal.
 *  - PLASMA (plasma fresco congelado e crioprecipitado): a bolsa leva ANTICORPOS, entao a regra se
 *    inverte. AB e doador universal de plasma; O so recebe plasma de qualquer um. O Rh nao conta.
 *
 * DIDATICA: nao substitui protocolo clinico/imuno-hematologico.
 */
public final class MatrizCompatibilidade {

    public static final int N_TIPOS = 8;

    // 1 = compativel, 0 = incompativel.          [doador][receptor]
    private static final int[][] HEMACIAS = {
        //            receptor: O-  O+  A-  A+  B-  B+ AB- AB+
        /* doador O-  */      { 1,  1,  1,  1,  1,  1,  1,  1 },
        /* doador O+  */      { 0,  1,  0,  1,  0,  1,  0,  1 },
        /* doador A-  */      { 0,  0,  1,  1,  0,  0,  1,  1 },
        /* doador A+  */      { 0,  0,  0,  1,  0,  0,  0,  1 },
        /* doador B-  */      { 0,  0,  0,  0,  1,  1,  1,  1 },
        /* doador B+  */      { 0,  0,  0,  0,  0,  1,  0,  1 },
        /* doador AB- */      { 0,  0,  0,  0,  0,  0,  1,  1 },
        /* doador AB+ */      { 0,  0,  0,  0,  0,  0,  0,  1 },
    };

    private static final int[][] PLASMA = {
        //            receptor: O-  O+  A-  A+  B-  B+ AB- AB+
        /* doador O-  */      { 1,  1,  0,  0,  0,  0,  0,  0 },
        /* doador O+  */      { 1,  1,  0,  0,  0,  0,  0,  0 },
        /* doador A-  */      { 1,  1,  1,  1,  0,  0,  0,  0 },
        /* doador A+  */      { 1,  1,  1,  1,  0,  0,  0,  0 },
        /* doador B-  */      { 1,  1,  0,  0,  1,  1,  0,  0 },
        /* doador B+  */      { 1,  1,  0,  0,  1,  1,  0,  0 },
        /* doador AB- */      { 1,  1,  1,  1,  1,  1,  1,  1 },
        /* doador AB+ */      { 1,  1,  1,  1,  1,  1,  1,  1 },
    };

    private MatrizCompatibilidade() {
    }

    /** Escolhe a matriz do hemocomponente: O(1), um if. */
    private static int[][] matrizDe(Hemocomponente componente) {
        if (componente == Hemocomponente.PLASMA || componente == Hemocomponente.CRIOPRECIPITADO) {
            return PLASMA;
        }
        return HEMACIAS;
    }

    /** O(1): um acesso [doador][receptor] na matriz. */
    public static boolean compativel(Hemocomponente componente, TipoSanguineo doador, TipoSanguineo receptor) {
        return matrizDe(componente)[doador.ordinal()][receptor.ordinal()] == 1;
    }

    /** Regra das hemacias (sem informar o componente). O(1). */
    public static boolean compativel(TipoSanguineo doador, TipoSanguineo receptor) {
        return HEMACIAS[doador.ordinal()][receptor.ordinal()] == 1;
    }

    /**
     * Doadores aceitos pelo receptor: percorre a COLUNA do receptor (8 linhas).
     * O(8) = O(1), porque o numero de tipos sanguineos e fixo.
     * O resultado sai na ordem do enum (O-, O+, A-, ...).
     */
    public static TipoSanguineo[] doadoresCompativeis(Hemocomponente componente, TipoSanguineo receptor) {
        int[][] matriz = matrizDe(componente);
        TipoSanguineo[] tipos = TipoSanguineo.values();
        int quantidade = 0;
        for (int doador = 0; doador < N_TIPOS; doador++) {
            quantidade += matriz[doador][receptor.ordinal()];
        }
        TipoSanguineo[] doadores = new TipoSanguineo[quantidade];
        int n = 0;
        for (int doador = 0; doador < N_TIPOS; doador++) {
            if (matriz[doador][receptor.ordinal()] == 1) {
                doadores[n++] = tipos[doador];
            }
        }
        return doadores;
    }
}
