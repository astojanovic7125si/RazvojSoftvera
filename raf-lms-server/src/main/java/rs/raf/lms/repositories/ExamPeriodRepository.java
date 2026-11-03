package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import rs.raf.lms.exam.ExamPeriod;

public interface ExamPeriodRepository extends JpaRepository<ExamPeriod,Long> {
    //Po id: findById(id) je ugrađen u JpaRepository. Vraća Optional<ExamPeriod>
    //Svi rokovi: findAll()
}
