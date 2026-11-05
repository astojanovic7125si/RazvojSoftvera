package rs.raf.lms;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.lms.academic.Course;
import rs.raf.lms.assignment.Assignment;
import rs.raf.lms.exam.ExamGroup;
import rs.raf.lms.exam.ExamPeriod;
import rs.raf.lms.exam.ExamRegistration;
import rs.raf.lms.exam.ExamSession;
import rs.raf.lms.repositories.*;
import rs.raf.lms.user.AppUser;
import rs.raf.lms.user.Role;
import rs.raf.lms.user.Student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class Seeder implements CommandLineRunner {
    private final AppUserRepository appUserRepository;
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final ExamGroupRepository examGroupRepository;
    private final ExamPeriodRepository examPeriodRepository;
    private final ExamRegistrationRepository examRegistrationRepository;
    private final ExamSessionRepository examSessionRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder  passwordEncoder;
    //ostalo nastaje kroz API
    @Override
    @Transactional//sve što run radi je jedna transakcija,pa se upiše sve ili ništa.Ako pukne na pola, ne upiše.
    public void run(String... args) throws Exception {
        if(appUserRepository.count()>0)
            return;//već ima podataka,izađi.Kod createdrop je baza prazna pri svakom startu pa se ovo ne aktivira

        String hash = passwordEncoder.encode("lozinka123");
        //passwordEncoder.encode(...) pretvara tu lozinku u BCrypt heš

        AppUser teacher1=appUserRepository.save(
                new AppUser("teacher1",hash,Role.TEACHER,"Petar","Petrovic"));
        AppUser teacher2=appUserRepository.save(
                new AppUser("teacher2",hash,Role.TEACHER,"Petar","Petrovic"));
        //Nastavnike ne čuvaš nigde posebno, jer tabela za nastavnike ne postoji. Nastavnik je običan nalog

        List<Student> students = new ArrayList<>();
        for(int i=0;i<6;i++){
            AppUser user=appUserRepository.save(
                    new AppUser("student"+i,hash,Role.STUDENT,"Student" + i, "Prezime" + i));
            students.add(studentRepository.save(new Student(user,"2023/000"+i)));//dodavanje u listu
        }

        Course predmet1=courseRepository.save(
                new Course("123","matematika"));
        predmet1.addTeacher(teacher1);
        Course predmet2=courseRepository.save(
                new Course("312","geografija"));
        predmet2.addTeacher(teacher2);//svaki predmet sa po jednim nastavnikom

        ExamPeriod rok1=examPeriodRepository.save(
                new ExamPeriod("januar", LocalDate.now().minusDays(30), LocalDate.now().plusDays(30)));

        LocalDateTime now=LocalDateTime.now();
        BigDecimal max=new BigDecimal("100.00");

        ExamSession before=examSessionRepository.save(
                new ExamSession(predmet1,rok1, now.minusDays(3), now.minusDays(3).plusHours(3),max));//rok pre
        ExamSession session1=examSessionRepository.save(//moj rok
                new ExamSession(predmet1,rok1, now.minusHours(1), now.plusHours(5),max));
        ExamSession after=examSessionRepository.save(//rok posle
                new ExamSession(predmet1,rok1,now.plusDays(3), now.plusDays(3).plusHours(3),max));

        ExamGroup group1=examGroupRepository.save(
                new ExamGroup(session1,"A"));//Grupe A i B u aktivnom terminu
        ExamGroup group2=examGroupRepository.save(
                new ExamGroup(session1,"B"));

        for(int i=0;i<students.size()-1;i++){
            ExamRegistration reg1= new ExamRegistration(students.get(i),session1);
            reg1.setExamGroup(i%2==0?group1:group2);
            reg1.setClassroom("sala"+(i%2+1));
            examRegistrationRepository.save(reg1);
            //studenti 0 do 4 na aktivan termin, grupe naizmenično (A: 0, 2, 4; B: 1, 3) i učionice sala1/sala2.
            //Student 0 je prijavljen i na budući termin, a student 5 nigde.
        }
        examRegistrationRepository.save(new ExamRegistration(students.get(0),after));

        //Zadaci: “mnozenje” za ceo termin, “sabiranje” samo za grupu A, “oduzimanje” samo za grupu B.
        Assignment assignment1=assignmentRepository.save(
                new Assignment(session1,"mnozenje","10 puta 10"));
        Assignment assignment2=new Assignment(session1,"sabiranje", "5 plus 5");
        assignment2.setExamGroup(group1);
        assignmentRepository.save(assignment2);
        Assignment assignment3=new Assignment(session1,"oduzimanje", "9 minus 4");
        assignment3.setExamGroup(group2);
        assignmentRepository.save(assignment3);
    }
}
//Seeder pravi polazno stanje
// (exam_attempts, submissions, code_comments, grades) beleže šta se dešava tokom ispita.
// Zato ih ne puniš unapred