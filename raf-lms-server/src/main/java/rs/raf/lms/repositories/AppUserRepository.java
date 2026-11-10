package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.lms.model.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser ,Long> {
    Optional<AppUser> findByUsername(String username);//da ne bi bilo duplikata-optional
    Boolean existsByUsername(String username);
}
