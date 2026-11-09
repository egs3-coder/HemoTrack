package br.com.hemotrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.repository.HospitalRepository;
import br.com.hemotrack.service.HospitalService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US09 - Cadastro de hospitais conveniados")
class US09HospitaisTest {

    private HospitalRepository repositorio;
    private HospitalService service;

    @BeforeEach
    void preparar() {
        repositorio = mock(HospitalRepository.class);
        service = new HospitalService(repositorio);
        when(repositorio.save(any(Hospital.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    @DisplayName("Cenário 1: Cadastro de hospital com sucesso")
    void cadastroComSucesso() {
        Hospital hospital = service.cadastrar("Hospital Santa Clara", "12.345.678/0001-90", "Norte");
        when(repositorio.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(hospital));

        assertEquals("Ativo", hospital.getSituacao());
        assertEquals("12345678000190", hospital.getCnpj());
        assertEquals("12.345.678/0001-90", hospital.getCnpjFormatado());
        assertEquals(List.of(hospital), service.listarAtivos());
    }

    @Test
    @DisplayName("Cenário 2: CNPJ já cadastrado")
    void cnpjJaCadastrado() {
        when(repositorio.existsByCnpj("12345678000190")).thenReturn(true);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Clínica Santa Clara", "12.345.678/0001-90", "Norte"));

        assertEquals("Já existe um hospital cadastrado com este CNPJ", erro.getMessage());
        verify(repositorio, never()).save(any(Hospital.class));
    }

    @Test
    @DisplayName("Cenário 3: Hospital inativado")
    void hospitalInativado() {
        Hospital boaVista = Fixtures.boaVista();
        when(repositorio.findById(2L)).thenReturn(Optional.of(boaVista));

        service.alternarSituacao(2L);

        assertEquals("Inativo", boaVista.getSituacao());
        assertFalse(boaVista.isAtivo());
    }

    @Test
    @DisplayName("Regra: nome, CNPJ e região obrigatórios; CNPJ com 14 dígitos")
    void camposObrigatorios() {
        assertEquals("O nome do hospital é obrigatório", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("", "12.345.678/0001-90", "Norte")).getMessage());
        assertEquals("O CNPJ do hospital é obrigatório", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Hospital X", " ", "Norte")).getMessage());
        assertEquals("A região do hospital é obrigatória", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Hospital X", "12.345.678/0001-90", "")).getMessage());
        assertEquals("O CNPJ deve possuir 14 dígitos", assertThrows(RegraNegocioException.class,
                () -> service.cadastrar("Hospital X", "12.345.678/0001", "Norte")).getMessage());
    }
}
