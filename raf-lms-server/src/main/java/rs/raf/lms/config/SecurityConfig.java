package rs.raf.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration//fabrika sa podesavanjima
public class SecurityConfig {
    @Bean//ono sto se vrati je objekat kojim Spring upravlja
        //Napravi ga jednom i daje svakom kome zatreba
    public PasswordEncoder passwordEncoder() {
        //PasswordEncoder – to je zvanični Spring interfejs (šablon) zadužen za rad sa lozinkama.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        //DelegatingPasswordEncoder->
        // za novu lozinku: koristi trenutno najbolje standarde
        //za proveru stare lozinke: pogleda kako je ta lozinka ranije bila u bazi.Ako je sačuvana preko starog algoritma,
        // on će znati da upotrebi taj stari algoritam da je proveri; ako je nova, koristiće novi.
    }
}
//Seeder: passwordEncoder.encode("lozinka123") → heš → AppUser.passwordHash → baza
//Prijava: korisnik pošalje username + lozinku
//            AuthService: nađe AppUser po username-u
//                         passwordEncoder.matches(lozinka, user.getPasswordHash())
//                            tačno   →  napravi token (JWT)
//                            netačno →  401 Unauthorized
