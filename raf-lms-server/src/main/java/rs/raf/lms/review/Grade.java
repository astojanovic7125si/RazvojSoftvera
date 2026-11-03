
package rs.raf.lms.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Length;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import rs.raf.lms.exam.ExamAttempt;
import rs.raf.lms.user.AppUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "grades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_attempt_id",
            nullable = false, unique = true)
    private ExamAttempt examAttempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "graded_by_user_id", nullable = false)
    private AppUser gradedBy;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal points;

    @Column(name = "feedback", length = Length.LONG32)
    private String feedback;

    @Column(name = "graded_at", nullable = false)
    private LocalDateTime gradedAt;

    public Grade(ExamAttempt examAttempt,
                 AppUser gradedBy,
                 BigDecimal points,
                 String feedback,
                 LocalDateTime gradedAt) {

        this.examAttempt = examAttempt;
        this.gradedBy = gradedBy;
        this.points = points;
        this.feedback = feedback;
        this.gradedAt = gradedAt;
    }
}
