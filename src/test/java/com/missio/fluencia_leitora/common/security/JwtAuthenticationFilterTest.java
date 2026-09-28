package com.missio.fluencia_leitora.common.security;

import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * AUTH-06/AUTH-10: o filtro só popula o SecurityContext para token válido de
 * usuário ativo, com perfil/professorId lidos do banco; usuário inativado
 * depois da emissão e requisição sem header seguem sem autenticação.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, usuarioRepository);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest requisicaoComToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/turmas");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    void tokenValidoDeUsuarioAtivoPopulaContextoComDadosDoBanco() throws Exception {
        when(jwtService.validarERetornarUsuarioId("tok")).thenReturn(Optional.of(10L));
        Usuario professor = new Usuario("prof@escola.com", "hash", Perfil.PROFESSOR, 77L);
        ReflectionTestUtils.setField(professor, "id", 10L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(professor));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requisicaoComToken("tok"), new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(new UsuarioAutenticado(10L, Perfil.PROFESSOR, 77L), auth.getPrincipal());
        assertEquals(List.of("ROLE_PROFESSOR"),
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        assertNotNull(chain.getRequest());
    }

    @Test
    void tokenValidoDeUsuarioInativadoNaoPopulaContexto() throws Exception {
        Usuario inativo = new Usuario("coord@escola.com", "hash", Perfil.COORDENADOR, null);
        inativo.setAtivo(false);
        when(jwtService.validarERetornarUsuarioId("tok")).thenReturn(Optional.of(11L));
        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(inativo));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requisicaoComToken("tok"), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void semHeaderAuthorizationSegueSemAutenticacao() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/turmas"), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }
}
