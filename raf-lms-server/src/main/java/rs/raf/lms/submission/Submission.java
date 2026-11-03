
package rs.raf.lms.submission;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.raf.lms.assignment.Assignment;
import rs.raf.lms.exam.ExamAttempt;

import java.time.LocalDateTime;
//// Predaja je nepromenljiva (nema setera): AUTOSAVE i FINAL se samo dodaju, nikad ne menjaju.
@Entity
@Table(name = "submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_attempt_id", nullable = false)
    private ExamAttempt examAttempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    // AUTOSAVE = automatsko međusnimanje, FINAL = konačna predaja (važi poslednja FINAL)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SubmissionKind kind;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public Submission(ExamAttempt examAttempt,
                      Assignment assignment,
                      SubmissionKind kind,
                      String originalFileName,
                      String storageKey,
                      LocalDateTime submittedAt) {

        this.examAttempt = examAttempt;
        this.assignment = assignment;
        this.kind = kind;
        this.originalFileName = originalFileName;
        this.storageKey = storageKey;
        this.submittedAt = submittedAt;
    }
}
