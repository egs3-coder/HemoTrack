package br.com.hemotrack.service;

import br.com.hemotrack.excecao.RecursoNaoEncontradoException;
import br.com.hemotrack.excecao.RegraNegocioException;
import br.com.hemotrack.model.Hospital;
import br.com.hemotrack.repository.HospitalRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US09 - Cadastro de hospitais conveniados.
 *   - Nome, CNPJ e regiao sao obrigatorios; o CNPJ e unico e possui 14 digitos.
 *   - Hospital inativo nao pode ser escolhido em novas requisicoes.
 */
@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    public List<Hospital> listar() {
        return hospitalRepository.findAllByOrderByNomeAsc();
    }

    public List<Hospital> listarAtivos() {
        return hospitalRepository.findByAtivoTrueOrderByNomeAsc();
    }

    public Hospital buscar(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Hospital não encontrado"));
    }

    @Transactional
    public Hospital cadastrar(String nome, String cnpj, String regiao) {
        if (vazio(nome)) {
            throw new RegraNegocioException("O nome do hospital é obrigatório");
        }
        if (vazio(cnpj)) {
            throw new RegraNegocioException("O CNPJ do hospital é obrigatório");
        }
        if (vazio(regiao)) {
            throw new RegraNegocioException("A região do hospital é obrigatória");
        }
        String digitos = cnpj.replaceAll("\\D", "");
        if (digitos.length() != 14) {
            throw new RegraNegocioException("O CNPJ deve possuir 14 dígitos");
        }
        if (hospitalRepository.existsByCnpj(digitos)) {
            throw new RegraNegocioException("Já existe um hospital cadastrado com este CNPJ");
        }
        return hospitalRepository.save(new Hospital(nome.trim(), digitos, regiao.trim()));
    }

    @Transactional
    public Hospital alternarSituacao(Long id) {
        Hospital hospital = buscar(id);
        if (hospital.isAtivo()) {
            hospital.inativar();
        } else {
            hospital.ativar();
        }
        return hospitalRepository.save(hospital);
    }

    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
