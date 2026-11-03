
package rs.raf.lms.exam;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import rs.raf.lms.user.Student;

@Entity
@Table(
        name = "exam_registrations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exam_registration_student_session",
                columnNames = {"student_id", "exam_session_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_group_id")
    private ExamGroup examGroup;

    @Column(name = "classroom", length = 50)
    private String classroom;

    public ExamRegistration(Student student, ExamSession examSession) {
        this.student = student;
        this.examSession = examSession;
    }
}
//Zadatak traži da se rešenja biraju i po učionici, a učionica se čuva po studentu.