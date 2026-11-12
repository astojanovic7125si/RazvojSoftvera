package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.raf.lms.model.ExamAttempt;

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt,Long> {
    Optional<ExamAttempt> findByExamRegistrationId(Long examRegistrationId);//svi registrovani

    List<ExamAttempt> findByExamRegistration_ExamSession_Id(Long examSessionId);
    //                (prijava pokušaja)  (njen termin)  (njegov broj)
    //nađi pokušaje čija prijava pripada terminu sa ovim brojem

    // Pokušaj, ali samo ako njegov termin pripada ovom predmetu
    @Query("""
            select a from ExamAttempt a
              join fetch a.examRegistration r
              join fetch r.examSession es
              join fetch r.student st
              join fetch st.user
            where a.id = :attemptId
              and es.course.id = :courseId
            """)
    Optional<ExamAttempt> findByIdForCourse(
            @Param("attemptId") Long attemptId,
            @Param("courseId") Long courseId);

    // Pokušaj ulogovanog studenta u datom terminu
    @Query("""
            select a from ExamAttempt a
              join fetch a.examRegistration r
              join fetch r.examSession es
              join fetch r.student st
              join fetch st.user u
            where es.id = :sessionId
              and u.username = :username
            """)
    Optional<ExamAttempt> findByStudentUsernameAndSessionId(
            @Param("username") String username,
            @Param("sessionId") Long sessionId);
}
