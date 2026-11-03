package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.lms.review.Grade;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade,Long> {
    List<Grade> findByExamAttempt_ExamRegistration_Student_Id(Long studentId);
    //Sve ocene jednog studenta (poeni i feedback za svaki ispit)
    Optional<Grade> findByExamAttemptId(Long examAttemptId);
    //Ocena jednog pokušaja (nastavnik ocenjuje ili student gleda rezultat)
}
