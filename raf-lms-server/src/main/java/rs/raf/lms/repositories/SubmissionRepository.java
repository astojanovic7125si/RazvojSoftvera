package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Za jedan termin: koliko RAZLIČITIH zadataka je svaki pokušaj predao (samo FINAL).
    @Query("""
            select s.examAttempt.id as attemptId,
                   count(distinct s.assignment.id) as assignmentCount
            from Submission s
            where s.examAttempt.examRegistration.examSession.id = :sessionId
              and s.kind = :kind
            group by s.examAttempt.id
            """)
    List<FinalSubmissionCount> countAssignmentsByAttempt(
            @Param("sessionId") Long sessionId,
            @Param("kind") SubmissionKind kind);
    interface FinalSubmissionCount {
        Long getAttemptId();
        Long getAssignmentCount();
    }

    // Sve predaje jedne vrste za predmet, sa opcionim filterima null = ne filtrira
    // Redosled: student, zadatak, pa NAJNOVIJA predaja prva
    @Query("""
            select s from Submission s
              join fetch s.assignment a
              join fetch s.examAttempt at
              join fetch at.examRegistration r
              join fetch r.student st
              join fetch st.user
              join fetch r.examSession es
              join fetch es.examPeriod ep
              left join fetch r.examGroup g
            where es.course.id = :courseId
              and s.kind = :kind
              and (:periodId is null or ep.id = :periodId)
              and (:sessionId is null or es.id = :sessionId)
              and (:groupId is null or g.id = :groupId)
              and (:classroom is null or r.classroom = :classroom)
              and (:indexNumber is null or st.indexNumber = :indexNumber)
            order by st.indexNumber, a.id, s.submittedAt desc, s.id desc
            """)
    List<Submission> findForReview(
            @Param("courseId") Long courseId,
            @Param("kind") SubmissionKind kind,
            @Param("periodId") Long periodId,
            @Param("sessionId") Long sessionId,
            @Param("groupId") Long groupId,
            @Param("classroom") String classroom,
            @Param("indexNumber") String indexNumber);

    // Jedna predaja, ali samo ako pripada ovom predmetu (inače 404, bez curenja tuđih podataka)
    @Query("""
            select s from Submission s
              join fetch s.assignment a
              join fetch s.examAttempt at
              join fetch at.examRegistration r
              join fetch r.student st
              join fetch r.examSession es
            where s.id = :submissionId
              and es.course.id = :courseId
            """)
    Optional<Submission> findByIdForCourse(
            @Param("submissionId") Long submissionId,
            @Param("courseId") Long courseId);

    // Sve predaje jednog pokušaja date vrste, za isti zadatak najnovija ide prva
    @Query("""
            select s from Submission s
              join fetch s.assignment a
            where s.examAttempt.id = :attemptId
              and s.kind = :kind
            order by a.id, s.submittedAt desc, s.id desc
            """)
    List<Submission> findByAttemptAndKind(
            @Param("attemptId") Long attemptId,
            @Param("kind") SubmissionKind kind);
}
