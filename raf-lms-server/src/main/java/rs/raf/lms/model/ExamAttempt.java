
package rs.raf.lms.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_registration_id",
            nullable = false, unique = true)
    private ExamRegistration examRegistration;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public ExamAttempt(ExamRegistration examRegistration,
                       LocalDateTime startedAt) {
        this.examRegistration = examRegistration;
        this.startedAt = startedAt;
    }
    // Poziva se pri svakoj konačnoj predaji: finishedAt = vreme poslednje FINAL predaje
    public void markFinished(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
