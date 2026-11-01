
package rs.raf.lms.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.hibernate.Length;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import rs.raf.lms.submission.Submission;
import rs.raf.lms.user.AppUser;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_comments")
public class CodeComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "content", nullable = false, length = Length.LONG32)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CodeComment() {
    }

    public CodeComment(Submission submission,
                       AppUser author,
                       String filePath,
                       int lineNumber,
                       String content,
                       LocalDateTime createdAt) {

        this.submission = submission;
        this.author = author;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Submission getSubmission() {
        return submission;
    }

    public AppUser getAuthor() {
        return author;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
