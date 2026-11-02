
package rs.raf.lms.submission;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.raf.lms.assignment.Assignment;
import rs.raf.lms.exam.ExamAttempt;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
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

    protected Submission() {
    }

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

    public SubmissionKind getKind() {
        return kind;
    }

    public void setKind(SubmissionKind kind) {
        this.kind = kind;
    }

    public Long getId() {
        return id;
    }

    public ExamAttempt getExamAttempt() {
        return examAttempt;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
