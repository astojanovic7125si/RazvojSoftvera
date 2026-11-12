
package rs.raf.lms.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "exam_groups",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exam_group_session_name",
                columnNames = {"exam_session_id", "name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @Column(nullable = false, length = 50)
    private String name;

    public ExamGroup(ExamSession examSession, String name) {
        this.examSession = examSession;
        this.name = name;
    }
}
