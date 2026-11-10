package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.raf.lms.model.ExamRegistration;

import java.util.List;
import java.util.Optional;

public interface ExamRegistrationRepository extends JpaRepository<ExamRegistration,Long> {
    Optional<ExamRegistration> findByStudentIdAndExamSessionId(Long studentId,Long examSessionId);
    //ispis za studenta u sesiji kojoj odgovara,vraća njegovu prijavu (sa grupom i učionicom), a ako nije, vraća ništa.
    List<ExamRegistration> findByExamSessionId(Long examSessionId);
    // Sve prijave jednog termina (svaka nosi studenta, grupu i učionicu)

    @Query("""
            select r from ExamRegistration r
            join fetch r.student s
            join fetch s.user
            left join fetch r.examGroup
            where r.examSession.id = :sessionId
            order by s.indexNumber
            """)
    List<ExamRegistration> findAllWithDetailsBySessionId(@Param("sessionId") Long sessionId);
}
