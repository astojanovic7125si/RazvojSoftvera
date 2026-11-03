
package rs.raf.lms.exam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import rs.raf.lms.academic.Course;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_period_id", nullable = false)
    private ExamPeriod examPeriod;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "max_points", nullable = false, precision = 7, scale = 2)
    private BigDecimal maxPoints;
    //Ocena ne sme da pređe maksimum termina. Bez ove kolone server nema sa čim da je uporedi.

    public ExamSession(Course course, ExamPeriod examPeriod,
                       LocalDateTime startTime, LocalDateTime endTime, BigDecimal maxPoints) {
        this.course = course;
        this.examPeriod = examPeriod;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxPoints = maxPoints;
    }
}
