
package rs.raf.lms.assignment;

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

import rs.raf.lms.exam.ExamGroup;
import rs.raf.lms.exam.ExamSession;

@Entity
@Table(name = "assignments")
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


    protected Assignment() {
    }

    public Assignment(ExamSession examSession, String title,
                      String description) {
        this.examSession = examSession;
        this.title = title;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public ExamSession getExamSession() {
        return examSession;
    }

    public void setExamSession(ExamSession examSession) {
        this.examSession = examSession;
    }

    public ExamGroup getExamGroup() {
        return examGroup;
    }

    public void setExamGroup(ExamGroup examGroup) {
        this.examGroup = examGroup;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }
}
