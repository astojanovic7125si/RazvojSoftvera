package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.lms.model.Student;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student,Long> {
    Optional<Student> findByIndexNumber(String indexNumber);
    Optional<Student> findByUserUsername(String username);
}
