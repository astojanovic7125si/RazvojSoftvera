package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.query.Param;
import rs.raf.lms.assignment.Assignment;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment,Long> {
    List<Assignment> findByExamSessionId(Long examSessionId);//Daj mi sve zadatke ovog termina.

    Optional<Assignment> findByIdAndExamSessionId(Long id, Long examSessionId);//Daj mi zadatak sa ovim id,
        // ali samo ako pripada ovom terminu

    @Query("select a from Assignment a where a.examSession.id = :sessionId "
            + "and (a.examGroup is null or a.examGroup.id = :groupId)")
    List<Assignment> findForStudent(@Param("sessionId") Long sessionId, @Param("groupId") Long groupId);
    //Koji zadaci iz ovog termina važe za studenta u grupi X
}
