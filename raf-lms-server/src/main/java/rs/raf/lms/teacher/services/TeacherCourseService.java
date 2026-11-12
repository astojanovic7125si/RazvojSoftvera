package rs.raf.lms.teacher.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.model.AppUser;
import rs.raf.lms.repositories.CourseRepository;
import rs.raf.lms.teacher.dto.response.TeacherCourseResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherCourseService {

    private final TeacherAccessService teacherAccessService;
    private final CourseRepository courseRepository;

    public List<TeacherCourseResponse> getMyCourses(String username) {
        AppUser teacher = teacherAccessService.requireTeacher(username);

        return courseRepository.findByTeachers_Id(teacher.getId())
                .stream()
                .map(course -> new TeacherCourseResponse(
                        course.getId(),
                        course.getCode(),
                        course.getName()
                ))
                .toList();
    }
}