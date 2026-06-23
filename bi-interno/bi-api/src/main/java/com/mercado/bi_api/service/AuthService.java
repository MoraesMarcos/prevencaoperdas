package com.mercado.bi_api.service;

import com.mercado.bi_api.dto.BootstrapAdminRequestDTO;
import com.mercado.bi_api.dto.LoginRequestDTO;
import com.mercado.bi_api.dto.LoginResponseDTO;
import com.mercado.bi_api.entity.PapelUsuario;
import com.mercado.bi_api.entity.Usuario;
import com.mercado.bi_api.repository.UsuarioRepository;
import com.mercado.bi_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * So funciona enquanto nao existir nenhum usuario cadastrado.
     * Depois do primeiro admin criado, este endpoint passa a recusar sempre.
     */
    public void bootstrapAdmin(BootstrapAdminRequestDTO dto) {
        if (usuarioRepository.count() > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ja existe usuario cadastrado");
        }

        usuarioRepository.save(Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senhaHash(passwordEncoder.encode(dto.getSenha()))
                .papel(PapelUsuario.ADMIN)
                .build());
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou senha invalidos"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou senha invalidos");
        }

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getPapel().name());

        return LoginResponseDTO.builder()
                .token(token)
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .papel(usuario.getPapel().name())
                .build();
    }
}
