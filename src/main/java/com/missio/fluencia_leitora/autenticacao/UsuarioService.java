package com.missio.fluencia_leitora.autenticacao;

import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.Perfil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AUTH-11/AUTH-12/AUTH-13: cadastro de usuários e redefinição de senha.
 *
 * <p>SPEC_DEVIATION: {@code criar} recebe os campos soltos em vez de um
 * {@code CriarUsuarioRequest}, porque os DTOs só são criados na T12 (mesmo
 * padrão de {@code ProfessorService}). {@code alterarSenha} usa o novo
 * {@link Usuario#redefinirSenha(String)}: a entidade não tinha como trocar o
 * hash nem desbloquear.
 */
@Service
public class UsuarioService {

    private static final int SENHA_MINIMA = 8;

    private final UsuarioRepository usuarioRepository;
    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository, ProfessorRepository professorRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.professorRepository = professorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Listagem para a tela de cadastro (frontend-web T34): por e-mail, sem senha nem hash. */
    @Transactional(readOnly = true)
    public List<UsuarioResumo> listar() {
        return usuarioRepository.findAllByOrderByEmailAsc().stream().map(UsuarioResumo::from).toList();
    }

    @Transactional
    public UsuarioResumo criar(String email, String senha, Perfil perfil, Long professorId) {
        validarSenha(senha);
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_DUPLICADO", "E-mail já está em uso");
        }
        validarProfessor(perfil, professorId);

        Usuario usuario = usuarioRepository.save(
                new Usuario(email, passwordEncoder.encode(senha), perfil, professorId));
        return UsuarioResumo.from(usuario);
    }

    @Transactional
    public void alterarSenha(Long id, String novaSenha) {
        validarSenha(novaSenha);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado"));
        usuario.redefinirSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.length() < SENHA_MINIMA) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "SENHA_INVALIDA", "A senha deve ter pelo menos 8 caracteres");
        }
    }

    private void validarProfessor(Perfil perfil, Long professorId) {
        boolean valido = perfil == Perfil.PROFESSOR
                ? professorId != null && professorRepository.findById(professorId).map(Professor::isAtivo).orElse(false)
                : professorId == null;
        if (!valido) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "PROFESSOR_ID_INVALIDO",
                    "PROFESSOR exige um professorId ativo; COORDENADOR não pode ter professorId");
        }
    }

    /** Dados do usuário devolvidos pela API - nunca inclui senha nem hash. */
    public record UsuarioResumo(Long id, String email, Perfil perfil, Long professorId, boolean ativo) {

        static UsuarioResumo from(Usuario usuario) {
            return new UsuarioResumo(
                    usuario.getId(), usuario.getEmail(), usuario.getPerfil(), usuario.getProfessorId(),
                    usuario.isAtivo());
        }
    }
}
