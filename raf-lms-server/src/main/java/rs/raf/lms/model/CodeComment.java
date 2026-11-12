
package rs.raf.lms.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Length;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    // Menja se samo tekst; fajl i linija ostaju isti
    public void changeContent(String content) {
        this.content = content;
    }
}
