package rs.raf.lms.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import rs.raf.lms.user.AppUser;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final long TOKEN_VALIDITY_SECONDS = 4 * 60 * 60;

    private final JwtEncoder jwtEncoder;

    public String generateToken(AppUser user) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("raf-lms-server")
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_VALIDITY_SECONDS))
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}