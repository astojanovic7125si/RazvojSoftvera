package rs.raf.lms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import rs.raf.lms.academic.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course,Long> {
    Optional<Course> findByCode(String code);

    List<Course> findByTeachers_Id(Long userId);//Nastavnik vidi spisak svojih predmeta

    boolean existsByIdAndTeachers_Id(Long courseId, Long userId);//da li ovaj nastavnik drži ovaj predmet
}
