package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import rs.raf.lms.exam.ExamRegistration;

import java.util.List;
import java.util.Optional;

public interface ExamRegistrationRepository extends JpaRepository<ExamRegistration,Long> {
    Optional<ExamRegistration> findByStudentIdAndExamSessionId(Long studentId,Long examSessionId);
    //ispis za studenta u sesiji kojoj odgovara,vraća njegovu prijavu (sa grupom i učionicom), a ako nije, vraća ništa.
    List<ExamRegistration> findByExamSessionId(Long examSessionId);
    // Sve prijave jednog termina (svaka nosi studenta, grupu i učionicu)
}
