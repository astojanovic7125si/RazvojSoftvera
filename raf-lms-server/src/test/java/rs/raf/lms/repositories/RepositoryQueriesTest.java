package rs.raf.lms.repositories;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import rs.raf.lms.model.AppUser;
import rs.raf.lms.model.Assignment;
import rs.raf.lms.model.Course;
import rs.raf.lms.model.ExamAttempt;
import rs.raf.lms.model.ExamGroup;
import rs.raf.lms.model.ExamPeriod;
import rs.raf.lms.model.ExamRegistration;
import rs.raf.lms.model.ExamSession;
import rs.raf.lms.model.Role;
import rs.raf.lms.model.Student;
import rs.raf.lms.model.Submission;
import rs.raf.lms.model.SubmissionKind;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DataJpaTest(
        showSql = false,
        properties = "spring.datasource.url=jdbc:h2:mem:repository-tests;DB_CLOSE_DELAY=-1"
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryQueriesTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private ExamAttemptRepository attemptRepository;
    @Autowired
    private ExamRegistrationRepository registrationRepository;
    @Autowired
    private SubmissionRepository submissionRepository;

    private Course predmet;
    private Course tudjPredmet;
    private ExamPeriod rok;
    private ExamSession termin;
    private ExamSession raniTermin;
    private ExamSession tudjTermin;
    private ExamGroup grupaA;
    private ExamGroup grupaB;
    private Assignment zadatakSvi;
    private Assignment zadatakB;
    private ExamAttempt pokusaj1;
    private ExamAttempt pokusaj2;
    private Submission novijaPredajaS1;

    //  - student s1: grupa A, sala1, u terminu i u ranijem terminu; ima pokušaj
    //  - student s2: grupa B, sala2; ima pokušaj
    //  - student s3: bez grupe, nema pokušaj
    //  - student s4: prijavljen na termin tuđeg predmeta
    //  - s1 ima FINAL k1 (starija) i k2 (novija) za isti zadatak, AUTOSAVE k3 i FINAL k4 za drugi zadatak
    //  - s2 ima FINAL k5, a student s4 ima FINAL k6 (tuđ predmet)
    @BeforeEach
    void priprema() {
        LocalDateTime sada = LocalDateTime.now().withNano(0);
        BigDecimal max = new BigDecimal("100.00");

        predmet = em.persist(new Course("C1", "Matematika"));
        tudjPredmet = em.persist(new Course("C2", "Geografija"));
        rok = em.persist(new ExamPeriod("januar", LocalDate.now().minusDays(30), LocalDate.now().plusDays(30)));

        termin = em.persist(new ExamSession(predmet, rok, sada.minusHours(1), sada.plusHours(5), max));
        raniTermin = em.persist(new ExamSession(predmet, rok, sada.minusDays(3), sada.minusDays(3).plusHours(3), max));
        tudjTermin = em.persist(new ExamSession(tudjPredmet, rok, sada.minusHours(1), sada.plusHours(5), max));

        grupaA = em.persist(new ExamGroup(termin, "A"));
        grupaB = em.persist(new ExamGroup(termin, "B"));

        zadatakSvi = em.persist(new Assignment(termin, "Svi", "opis"));
        Assignment zadatakA = new Assignment(termin, "Samo A", "opis");
        zadatakA.setExamGroup(grupaA);
        em.persist(zadatakA);
        zadatakB = new Assignment(termin, "Samo B", "opis");
        zadatakB.setExamGroup(grupaB);
        em.persist(zadatakB);
        Assignment tudjZadatak = em.persist(new Assignment(tudjTermin, "Tudj", "opis"));

        Student s1 = student("s1", "2023/0001");
        Student s2 = student("s2", "2023/0002");
        Student s3 = student("s3", "2023/0003");
        Student s4 = student("s4", "2023/0004");

        ExamRegistration p1 = prijava(s1, termin, grupaA, "sala1");
        prijava(s1, raniTermin, null, null);
        ExamRegistration p2 = prijava(s2, termin, grupaB, "sala2");
        prijava(s3, termin, null, null);
        ExamRegistration p4 = prijava(s4, tudjTermin, null, "sala9");

        pokusaj1 = em.persist(new ExamAttempt(p1, sada.minusMinutes(50)));
        pokusaj2 = em.persist(new ExamAttempt(p2, sada.minusMinutes(50)));
        ExamAttempt tudjPokusaj = em.persist(new ExamAttempt(p4, sada.minusMinutes(50)));

        predaja(pokusaj1, zadatakSvi, SubmissionKind.FINAL, "k1", sada.minusMinutes(30));
        novijaPredajaS1 = predaja(pokusaj1, zadatakSvi, SubmissionKind.FINAL, "k2", sada.minusMinutes(10));
        predaja(pokusaj1, zadatakSvi, SubmissionKind.AUTOSAVE, "k3", sada.minusMinutes(20));
        predaja(pokusaj1, zadatakA, SubmissionKind.FINAL, "k4", sada.minusMinutes(5));
        predaja(pokusaj2, zadatakSvi, SubmissionKind.FINAL, "k5", sada.minusMinutes(15));
        predaja(tudjPokusaj, tudjZadatak, SubmissionKind.FINAL, "k6", sada.minusMinutes(15));

        // sve u bazu, pa prazan kontekst: upiti moraju sami da učitaju sve što im treba
        em.flush();
        em.clear();
    }

    //pomoćne metode
    private Student student(String username, String indeks) {
        AppUser korisnik = em.persist(new AppUser(username, "hash", Role.STUDENT, "Ime", "Prezime"));
        return em.persist(new Student(korisnik, indeks));
    }

    private ExamRegistration prijava(Student student, ExamSession termin, ExamGroup grupa, String ucionica) {
        ExamRegistration prijava = new ExamRegistration(student, termin);
        prijava.setExamGroup(grupa);
        prijava.setClassroom(ucionica);
        return em.persist(prijava);
    }

    private Submission predaja(ExamAttempt pokusaj, Assignment zadatak, SubmissionKind vrsta,
                               String kljuc, LocalDateTime vreme) {
        return em.persist(new Submission(pokusaj, zadatak, vrsta, "resenje.zip", kljuc, vreme));
    }

    private List<String> kljucevi(List<Submission> predaje) {
        return predaje.stream().map(Submission::getStorageKey).toList();
    }

    private Map<Long, Long> brojZadataka(Long terminId, SubmissionKind vrsta) {
        Map<Long, Long> rezultat = new HashMap<>();
        for (SubmissionRepository.FinalSubmissionCount red
                : submissionRepository.countAssignmentsByAttempt(terminId, vrsta)) {
            rezultat.put(red.getAttemptId(), red.getAssignmentCount());
        }
        return rezultat;
    }

    //AssignmentRepository
    @Test
    void findForStudent_grupaA_vracaZadatkeBezGrupeIZadatkeGrupeA() {
        List<Assignment> zadaci = assignmentRepository.findForStudent(termin.getId(), grupaA.getId());

        assertThat(zadaci.stream().map(Assignment::getTitle).toList())
                .containsExactlyInAnyOrder("Svi", "Samo A");
    }

    @Test
    void findForStudent_grupaB_vracaZadatkeBezGrupeIZadatkeGrupeB() {
        List<Assignment> zadaci = assignmentRepository.findForStudent(termin.getId(), grupaB.getId());

        assertThat(zadaci.stream().map(Assignment::getTitle).toList())
                .containsExactlyInAnyOrder("Svi", "Samo B");
    }

    @Test
    void findForStudent_studentBezGrupe_vidiSamoZadatkeZaSve() {
        List<Assignment> zadaci = assignmentRepository.findForStudent(termin.getId(), null);

        assertThat(zadaci.stream().map(Assignment::getTitle).toList()).containsExactly("Svi");
    }

    //ExamRegistrationRepository
    @Test
    void findAllWithDetailsBySessionId_vracaPrijaveTerminaPoBrojuIndeksa() {
        List<ExamRegistration> prijave = registrationRepository.findAllWithDetailsBySessionId(termin.getId());

        assertThat(prijave.stream().map(p -> p.getStudent().getIndexNumber()).toList())
                .containsExactly("2023/0001", "2023/0002", "2023/0003");
        // join fetch: student i korisnik su već učitani, bez dodatnih upita
        assertThat(Hibernate.isInitialized(prijave.get(0).getStudent().getUser())).isTrue();
    }

    @Test
    void findAllForStudent_najnovijiTerminJePrvi() {
        List<ExamRegistration> prijave = registrationRepository.findAllForStudent("s1");

        assertThat(prijave).hasSize(2);
        assertThat(prijave.get(0).getExamSession().getId()).isEqualTo(termin.getId());
        assertThat(prijave.get(1).getExamSession().getId()).isEqualTo(raniTermin.getId());
        assertThat(Hibernate.isInitialized(prijave.get(0).getExamSession().getCourse())).isTrue();
        assertThat(Hibernate.isInitialized(prijave.get(0).getExamSession().getExamPeriod())).isTrue();
    }

    @Test
    void findAllForStudent_drugiStudentVidiSamoSvojePrijave() {
        List<ExamRegistration> prijave = registrationRepository.findAllForStudent("s2");

        assertThat(prijave).hasSize(1);
        assertThat(prijave.get(0).getExamGroup().getName()).isEqualTo("B");
        assertThat(prijave.get(0).getClassroom()).isEqualTo("sala2");
    }

    @Test
    void findAllForStudent_nepoznatKorisnik_vracaPraznuListu() {
        assertThat(registrationRepository.findAllForStudent("nepostoji")).isEmpty();
    }

    //SubmissionRepository

    @Test
    void countAssignmentsByAttempt_finalBrojiRazlicitaZadatkeNaPokusaj() {
        // s1: zadatakSvi (dva puta) + zadatakA = 2 zadatka; s2: 1 zadatak; tuđ termin se ne računa
        assertThat(brojZadataka(termin.getId(), SubmissionKind.FINAL))
                .containsOnly(entry(pokusaj1.getId(), 2L), entry(pokusaj2.getId(), 1L));
    }

    @Test
    void countAssignmentsByAttempt_autosaveSeNeMesaSaFinal() {
        assertThat(brojZadataka(termin.getId(), SubmissionKind.AUTOSAVE))
                .containsOnly(entry(pokusaj1.getId(), 1L));
    }

    @Test
    void findForReview_bezFiltera_redosledJeStudentZadatakNajnovijaPrva() {
        List<Submission> predaje = submissionRepository.findForReview(
                predmet.getId(), SubmissionKind.FINAL, null, null, null, null, null);

        assertThat(kljucevi(predaje)).containsExactly("k2", "k1", "k4", "k5");
    }

    @Test
    void findForReview_filterGrupa() {
        List<Submission> predaje = submissionRepository.findForReview(
                predmet.getId(), SubmissionKind.FINAL, null, null, grupaB.getId(), null, null);

        assertThat(kljucevi(predaje)).containsExactly("k5");
    }

    @Test
    void findForReview_filterUcionica() {
        List<Submission> predaje = submissionRepository.findForReview(
                predmet.getId(), SubmissionKind.FINAL, null, null, null, "sala1", null);

        assertThat(kljucevi(predaje)).containsExactly("k2", "k1", "k4");
    }

    @Test
    void findForReview_filterBrojIndeksa() {
        List<Submission> predaje = submissionRepository.findForReview(
                predmet.getId(), SubmissionKind.FINAL, null, null, null, null, "2023/0002");

        assertThat(kljucevi(predaje)).containsExactly("k5");
    }

    @Test
    void findForReview_filterRokITermin() {
        assertThat(kljucevi(submissionRepository.findForReview(
                predmet.getId(), SubmissionKind.FINAL, rok.getId(), termin.getId(), null, null, null)))
                .containsExactly("k2", "k1", "k4", "k5");

        assertThat(submissionRepository.findForReview(
                predmet.getId(), SubmissionKind.FINAL, null, raniTermin.getId(), null, null, null))
                .isEmpty();
    }

    @Test
    void findForReview_vrstaPredaje() {
        List<Submission> predaje = submissionRepository.findForReview(
                predmet.getId(), SubmissionKind.AUTOSAVE, null, null, null, null, null);

        assertThat(kljucevi(predaje)).containsExactly("k3");
    }

    @Test
    void findForReview_nastavnikNeVidiPredajeTudjegPredmeta() {
        assertThat(kljucevi(submissionRepository.findForReview(
                tudjPredmet.getId(), SubmissionKind.FINAL, null, null, null, null, null)))
                .containsExactly("k6");
    }

    @Test
    void findByIdForCourse_predaja_samoZaPredmetKomePripada() {
        assertThat(submissionRepository.findByIdForCourse(novijaPredajaS1.getId(), predmet.getId()))
                .isPresent();
        assertThat(submissionRepository.findByIdForCourse(novijaPredajaS1.getId(), tudjPredmet.getId()))
                .isEmpty();
    }

    @Test
    void findByAttemptAndKind_zaIsti_zadatakNajnovijaIde_prva() {
        List<Submission> predaje = submissionRepository.findByAttemptAndKind(
                pokusaj1.getId(), SubmissionKind.FINAL);

        assertThat(kljucevi(predaje)).containsExactly("k2", "k1", "k4");
        assertThat(Hibernate.isInitialized(predaje.get(0).getAssignment())).isTrue();
    }

    @Test
    void findFirstBy_vracaNajnovijuPredajuZaZadatak() {
        Optional<Submission> predaja = submissionRepository
                .findFirstByExamAttemptIdAndAssignmentIdAndKindOrderBySubmittedAtDesc(
                        pokusaj1.getId(), zadatakSvi.getId(), SubmissionKind.FINAL);

        assertThat(predaja).isPresent();
        assertThat(predaja.get().getStorageKey()).isEqualTo("k2");
    }

    @Test
    void existsByAssignment_Id_samoZaZadatkeSaPredajama() {
        assertThat(submissionRepository.existsByAssignment_Id(zadatakSvi.getId())).isTrue();
        assertThat(submissionRepository.existsByAssignment_Id(zadatakB.getId())).isFalse();
    }

    //ExamAttemptRepository
    @Test
    void findByIdForCourse_pokusaj_samoZaPredmetKomePripada() {
        Optional<ExamAttempt> pokusaj = attemptRepository.findByIdForCourse(pokusaj1.getId(), predmet.getId());

        assertThat(pokusaj).isPresent();
        assertThat(Hibernate.isInitialized(pokusaj.get().getExamRegistration().getStudent().getUser())).isTrue();
        assertThat(attemptRepository.findByIdForCourse(pokusaj1.getId(), tudjPredmet.getId())).isEmpty();
    }

    @Test
    void findByStudentUsernameAndSessionId_vracaPokusajTogStudenta() {
        Optional<ExamAttempt> pokusaj = attemptRepository
                .findByStudentUsernameAndSessionId("s2", termin.getId());

        assertThat(pokusaj).isPresent();
        assertThat(pokusaj.get().getId()).isEqualTo(pokusaj2.getId());
    }

    @Test
    void findByStudentUsernameAndSessionId_bezPokusajaVracaPrazno() {
        // s3 je prijavljen ali nije preuzeo zadatak; s1 u ranijem terminu nema pokušaj
        assertThat(attemptRepository.findByStudentUsernameAndSessionId("s3", termin.getId())).isEmpty();
        assertThat(attemptRepository.findByStudentUsernameAndSessionId("s1", raniTermin.getId())).isEmpty();
        assertThat(attemptRepository.findByStudentUsernameAndSessionId("nepostoji", termin.getId())).isEmpty();
    }
}