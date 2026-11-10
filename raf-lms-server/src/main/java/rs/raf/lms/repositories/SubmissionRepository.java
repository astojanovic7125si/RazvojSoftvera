package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.lms.model.Submission;
import rs.raf.lms.model.SubmissionKind;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission,Long> {
    // Tvoja: sve predaje jednog pokušaja jedne vrste (sve FINAL ili sve AUTOSAVE)
    List<Submission> findByExamAttemptIdAndKind(Long examAttemptId, SubmissionKind kind);
    // Poslednja predaja date vrste za JEDAN zadatak (za ocenjivanje i monitoring)
    Optional<Submission> findFirstByExamAttemptIdAndAssignmentIdAndKindOrderBySubmittedAtDesc(
            Long examAttemptId, Long assignmentId, SubmissionKind kind);
    // Sve predaje pokušaja, od najstarije do najnovije (istorija)
    List<Submission> findByExamAttemptIdOrderBySubmittedAtAsc(Long examAttemptId);
    boolean existsByAssignment_Id(Long assignmentId);
}
