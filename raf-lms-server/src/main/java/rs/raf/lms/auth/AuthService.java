package rs.raf.lms.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import rs.raf.lms.auth.dto.LoginRequest;
import rs.raf.lms.auth.dto.LoginResponse;
import rs.raf.lms.repositories.AppUserRepository;
import rs.raf.lms.user.AppUser;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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