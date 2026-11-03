package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import rs.raf.lms.exam.ExamAttempt;

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt,Long> {
    Optional<ExamAttempt> findByExamRegistrationId(Long examRegistrationId);//svi registrovani

    List<ExamAttempt> findByExamRegistration_ExamSession_Id(Long examSessionId);
    //                (prijava pokušaja)  (njen termin)  (njegov broj)
    //nađi pokušaje čija prijava pripada terminu sa ovim brojem

}
