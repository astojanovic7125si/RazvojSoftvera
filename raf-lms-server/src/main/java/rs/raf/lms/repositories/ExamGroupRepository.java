package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import rs.raf.lms.exam.ExamGroup;

import java.util.List;
import java.util.Optional;

public interface ExamGroupRepository extends JpaRepository<ExamGroup,Long> {
    List<ExamGroup> findByExamSessionId(Long examSessionId);//Nastavnik vidi grupe termina

    Optional<ExamGroup> findByExamSessionIdAndName(Long examSessionId, String name);
    //student je u grupi A, a tražim tu grupu u tom terminu
    //ako nastavnik šalje spisak studenata sa imenima grupa

    Optional<ExamGroup> findByIdAndExamSessionId(Long id, Long sessionId);
    //Proverava da grupa stvarno pripada terminu (pri prijavi studenta i pri zadatku)
    //čuva pravilo da se student ili zadatak ne vežu za grupu iz tuđeg termin
}
