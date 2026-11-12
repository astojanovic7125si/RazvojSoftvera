
package rs.raf.lms.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToMany//jedan nastavnik drži više predmeta i da jedan predmet ima više nastavnika.
    @JoinTable(
            name = "course_teachers",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    //@JoinTable kaže da se veza čuva u posebnoj tabeli course_teachers sa dve kolone.
    private Set<AppUser> teachers = new HashSet<>();

    public Course(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Set<AppUser> getTeachers() {
        return Collections.unmodifiableSet(teachers);
    }
    public void addTeacher(AppUser teacher) {
        teachers.add(teacher);
    }
    public void removeTeacher(AppUser teacher) {
        teachers.remove(teacher);
    }
    //Getter vraća samo čitljiv skup, pa se izmena radi samo kroz addTeacher i removeTeacher.
}
