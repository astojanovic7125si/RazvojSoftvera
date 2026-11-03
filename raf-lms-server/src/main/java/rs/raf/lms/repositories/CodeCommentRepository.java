package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.lms.review.CodeComment;

import java.util.List;

public interface CodeCommentRepository extends JpaRepository<CodeComment,Long> {
    List<CodeComment> findBySubmissionIdOrderByCreatedAtAsc(Long submissionId);
    //Nastavnik (ili student, posle ocenjivanja) otvara predaju i vidi komentare
}
