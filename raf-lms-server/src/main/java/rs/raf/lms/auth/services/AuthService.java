package rs.raf.lms.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.auth.dto.request.LoginRequest;
import rs.raf.lms.auth.dto.response.LoginResponse;
import rs.raf.lms.model.AppUser;
import rs.raf.lms.repositories.AppUserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AppUserRepository appUserRepository; // pristup tabeli korisnika u bazi
    private final PasswordEncoder passwordEncoder;     // provera lozinke
    private final JwtService jwtService;               // pravljenje tokena

    public LoginResponse login(LoginRequest request) {

        AppUser user = authenticate(request);

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                user.getRole().name()
        );
    }

    private AppUser authenticate(LoginRequest request) {

        AppUser user = appUserRepository
                .findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException(
                        "Neispravno korisnicko ime ili lozinka"
                ));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Neispravno korisnicko ime ili lozinka"
            );
        }

        return user;
    }
}