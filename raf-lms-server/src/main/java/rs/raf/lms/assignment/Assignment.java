
package rs.raf.lms.assignment;

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
import lombok.Setter;
import org.hibernate.Length;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import rs.raf.lms.exam.ExamGroup;
import rs.raf.lms.exam.ExamSession;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_group_id")
    private ExamGroup examGroup;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = Length.LONG32)
    private String description;


    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "storage_key", unique = true, length = 500)
    private String storageKey;

    public Assignment(ExamSession examSession, String title,
                      String description) {
        this.examSession = examSession;
        this.title = title;
        this.description = description;
    }
}
