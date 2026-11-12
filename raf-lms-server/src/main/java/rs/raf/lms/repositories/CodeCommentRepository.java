package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.lms.model.CodeComment;

import java.util.List;
import java.util.Optional;

public interface CodeCommentRepository extends JpaRepository<CodeComment,Long> {
    List<CodeComment> findBySubmissionIdOrderByCreatedAtAsc(Long submissionId);
    //Nastavnik (ili student, posle ocenjivanja) otvara predaju i vidi komentare

    // Komentar ali samo ako pripada baš toj predaji
    Optional<CodeComment> findByIdAndSubmissionId(Long id, Long submissionId);
}
