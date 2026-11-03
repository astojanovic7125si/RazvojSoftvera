package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.lms.exam.ExamSession;

import java.util.List;
import java.util.Optional;

public interface ExamSessionRepository extends JpaRepository<ExamSession,Long> {
    List<ExamSession> findByCourseId(Long courseId);//Termini po predmetu

    List<ExamSession> findByExamPeriodId(Long examPeriodId);//Termini po roku

    List<ExamSession> findByCourseIdAndExamPeriodId(Long courseId, Long examPeriodId);
    //Termini jednog predmeta u jednom roku

    Optional<ExamSession> findByIdAndCourseId(Long id, Long courseId);
    //Provera da termin pripada predmetu (iz adrese)
    //vraća Optional, jer je par id termina i predmeta jedinstven,
    // i radi isto što i findByIdAndExamSessionId kod zadataka
}
