package rs.raf.lms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Pravila pristupa i predaje (403, 409, 400) kroz pravi HTTP sloj, H2 bazu i seed podatke.
// Seed: termin 1 je prošao, termin 2 je aktivan, termin 3 tek dolazi.
// Zadaci termina 2: 1 "mnozenje" (svi), 2 "sabiranje" (grupa A), 3 "oduzimanje" (grupa B).
// Studenti 0, 2, 4 su u grupi A, studenti 1 i 3 u grupi B; student 5 nije prijavljen nigde,
// a student 0 je prijavljen i na termin 3.
// Testovi dele bazu, pa svaki test koji menja stanje (preuzimanje zadatka) koristi svog studenta:
// student2 = predaja pre preuzimanja, student3 = neispravni fajlovi.
// NIKO ne postavlja ZIP za zadatke 2 i 3, jer se na tome zasnivaju testovi "zadatak bez ZIP-a".
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:raflms-rules;DB_CLOSE_DELAY=-1"
})
class AccessRulesIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final String LOZINKA = "lozinka123";
    private static final String NASTAVNIK = "teacher1";
    private static final Pattern JWT = Pattern.compile("eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+");
    private static final Path STORAGE = napraviPrivremeniFolder();
    private static final Map<String, String> TOKENI = new HashMap<>();
    private static boolean projekatPostavljen = false;

    @DynamicPropertySource
    static void podesavanja(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root", STORAGE::toString);
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mvc;

    @BeforeEach
    void podesiMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    //provera prijave

    @Test
    @DisplayName("Student koji nije prijavljen ne može da vidi, preuzme ni preda zadatak (403)")
    void neprijavljenStudentJeOdbijen() throws Exception {
        MockHttpServletResponse spisak = pozovi(get("/api/student/exam-sessions/2/assignments")
                .header(AUTH, bearer("student5")));
        assertThat(spisak.getStatus()).isEqualTo(403);
        assertThat(telo(spisak)).contains("nije prijavljen");

        assertThat(pozovi(get("/api/student/exam-sessions/2/assignments/1/project")
                .header(AUTH, bearer("student5"))).getStatus()).isEqualTo(403);

        assertThat(predaj("student5", 2, 1, "resenje.zip", resenje()).getStatus()).isEqualTo(403);
    }

    //provera vremena

    @Test
    @DisplayName("Termin koji još nije počeo: preuzimanje i predaja su odbijeni (403)")
    void terminKojiJosNijePoceoJeZakljucan() throws Exception {
        // student0 je prijavljen na termin 3, koji počinje tek za tri dana
        MockHttpServletResponse spisak = pozovi(get("/api/student/exam-sessions/3/assignments")
                .header(AUTH, bearer("student0")));
        assertThat(spisak.getStatus()).isEqualTo(403);
        assertThat(telo(spisak)).contains("tokom ispita");

        MockHttpServletResponse predaja = predaj("student0", 3, 1, "resenje.zip", resenje());
        assertThat(predaja.getStatus()).isEqualTo(403);
        assertThat(telo(predaja)).contains("tokom ispita");
    }

    @Test
    @DisplayName("Termin koji je istekao: preuzimanje i predaja posle roka su odbijeni (403)")
    void istekaoTerminJeZakljucan() throws Exception {
        // termin 1 je završen pre tri dana; nastavnik prijavljuje studenta1 na njega
        MockHttpServletResponse prijava = pozovi(post("/api/teacher/courses/1/exam-sessions/1/registrations")
                .header(AUTH, bearer(NASTAVNIK))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"students\":[{\"indexNumber\":\"2023/0001\"}]}"));
        assertThat(prijava.getStatus()).isEqualTo(200);

        MockHttpServletResponse spisak = pozovi(get("/api/student/exam-sessions/1/assignments")
                .header(AUTH, bearer("student1")));
        assertThat(spisak.getStatus()).isEqualTo(403);
        assertThat(telo(spisak)).contains("tokom ispita");

        MockHttpServletResponse predaja = predaj("student1", 1, 1, "resenje.zip", resenje());
        assertThat(predaja.getStatus()).isEqualTo(403);
        assertThat(telo(predaja)).contains("tokom ispita");
    }

    //provera grupe

    @Test
    @DisplayName("Zadatak tuđe grupe: preuzimanje i predaja su odbijeni (403)")
    void zadatakTudjeGrupeJeOdbijen() throws Exception {
        // student0 je u grupi A, a zadatak 3 je za grupu B
        MockHttpServletResponse preuzimanje = pozovi(get("/api/student/exam-sessions/2/assignments/3/project")
                .header(AUTH, bearer("student0")));
        assertThat(preuzimanje.getStatus()).isEqualTo(403);
        assertThat(telo(preuzimanje)).contains("tvojoj grupi");

        assertThat(predaj("student0", 2, 3, "resenje.zip", resenje()).getStatus()).isEqualTo(403);

        // student1 je u grupi B, a zadatak 2 je za grupu A
        assertThat(pozovi(get("/api/student/exam-sessions/2/assignments/2/project")
                .header(AUTH, bearer("student1"))).getStatus()).isEqualTo(403);
    }

    //stanje zadatka i pokušaja

    @Test
    @DisplayName("Zadatak bez ZIP-a: preuzimanje daje 404, predaja daje 409")
    void zadatakBezZipaSeNeMozeUzetiNiPredati() throws Exception {
        // zadatak 2 je za grupu A (student0) i nastavnik za njega nije postavio ZIP
        MockHttpServletResponse preuzimanje = pozovi(get("/api/student/exam-sessions/2/assignments/2/project")
                .header(AUTH, bearer("student0")));
        assertThat(preuzimanje.getStatus()).isEqualTo(404);
        assertThat(telo(preuzimanje)).contains("ZIP");

        MockHttpServletResponse predaja = predaj("student0", 2, 2, "resenje.zip", resenje());
        assertThat(predaja.getStatus()).isEqualTo(409);
        assertThat(telo(predaja)).contains("ZIP");
    }

    @Test
    @DisplayName("Predaja pre preuzimanja zadatka daje 409")
    void predajaPrePreuzimanjaJeKonflikt() throws Exception {
        postaviProjekatZaZadatak1();

        // student2 nikad nije preuzeo zadatak, pa nema pokušaj
        MockHttpServletResponse predaja = predaj("student2", 2, 1, "resenje.zip", resenje());

        assertThat(predaja.getStatus()).isEqualTo(409);
        assertThat(telo(predaja)).contains("Prvo preuzmi");
    }

    @Test
    @DisplayName("Fajl koji nije ZIP daje 400, a posle toga ispravna predaja prolazi (201)")
    void neispravanFajlJeOdbijen() throws Exception {
        postaviProjekatZaZadatak1();
        MockHttpServletResponse preuzimanje = pozovi(get("/api/student/exam-sessions/2/assignments/1/project")
                .header(AUTH, bearer("student3")));
        assertThat(preuzimanje.getStatus()).isEqualTo(200);

        // pogrešna ekstenzija
        MockHttpServletResponse tekst = predaj("student3", 2, 1, "resenje.txt",
                "tekst".getBytes(StandardCharsets.UTF_8));
        assertThat(tekst.getStatus()).isEqualTo(400);
        assertThat(telo(tekst)).contains("ZIP");

        // ekstenzija je .zip, ali sadržaj nije ZIP arhiva
        MockHttpServletResponse lazniZip = predaj("student3", 2, 1, "lazni.zip",
                "ovo nije zip".getBytes(StandardCharsets.UTF_8));
        assertThat(lazniZip.getStatus()).isEqualTo(400);
        assertThat(telo(lazniZip)).contains("ZIP");

        // neispravni pokušaji ne kvare stanje
        assertThat(predaj("student3", 2, 1, "resenje.zip", resenje()).getStatus()).isEqualTo(201);
    }

    //novi endpoint: termini studenta

    @Test
    @DisplayName("Student vidi svoje termine, najnoviji prvi; student bez prijava dobija praznu listu")
    void studentVidiSvojeTermine() throws Exception {
        MockHttpServletResponse rez = pozovi(get("/api/student/exam-sessions")
                .header(AUTH, bearer("student0")));
        assertThat(rez.getStatus()).isEqualTo(200);
        String json = telo(rez);
        assertThat(json).contains("\"sessionId\":2", "\"sessionId\":3");
        assertThat(json).contains("\"status\":\"ACTIVE\"", "\"status\":\"UPCOMING\"");
        assertThat(json).contains("\"groupName\":\"A\"");
        // termin 3 počinje kasnije od termina 2, pa mora biti prvi u listi
        assertThat(json.indexOf("\"sessionId\":3")).isLessThan(json.indexOf("\"sessionId\":2"));

        MockHttpServletResponse prazno = pozovi(get("/api/student/exam-sessions")
                .header(AUTH, bearer("student5")));
        assertThat(prazno.getStatus()).isEqualTo(200);
        assertThat(telo(prazno)).isEqualTo("[]");

        assertThat(pozovi(get("/api/student/exam-sessions")).getStatus()).isEqualTo(401);
        assertThat(pozovi(get("/api/student/exam-sessions").header(AUTH, bearer(NASTAVNIK))).getStatus())
                .isEqualTo(403);
    }

    //monitoring i termini nastavnika

    @Test
    @DisplayName("Monitoring ne računa zadatke kojima nije postavljen ZIP")
    void monitoringNeBrojiZadatkeBezZip() throws Exception {
        postaviProjekatZaZadatak1();

        MockHttpServletResponse rez = pozovi(get("/api/teacher/courses/1/exam-sessions/2/monitoring")
                .header(AUTH, bearer(NASTAVNIK)));

        assertThat(rez.getStatus()).isEqualTo(200);
        String json = telo(rez);
        // samo zadatak 1 ima ZIP: grupa A (zadaci 1 i 2) i grupa B (zadaci 1 i 3) imaju po 1 zadatak
        assertThat(json).contains("\"assignmentsTotal\":1");
        assertThat(json).doesNotContain("\"assignmentsTotal\":2");
        assertThat(json).doesNotContain("\"assignmentsTotal\":3");
    }

    @Test
    @DisplayName("Termin mora biti unutar datuma ispitnog roka")
    void terminMoraBitiUnutarRoka() throws Exception {
        // rok 1 ("januar") traje 30 dana u prošlost i 30 dana u budućnost
        LocalDateTime vanRoka = LocalDateTime.now().plusDays(90).withNano(0);
        MockHttpServletResponse odbijen = napraviTermin(vanRoka, vanRoka.plusHours(3));
        assertThat(odbijen.getStatus()).isEqualTo(400);
        assertThat(telo(odbijen)).contains("ispitnog roka");

        LocalDateTime uRoku = LocalDateTime.now().plusDays(5).withNano(0);
        assertThat(napraviTermin(uRoku, uRoku.plusHours(3)).getStatus()).isEqualTo(201);
    }

    //pomoćne metode

    private MockHttpServletResponse napraviTermin(LocalDateTime pocetak, LocalDateTime kraj) throws Exception {
        String json = "{\"examPeriodId\":1,\"startTime\":\"" + pocetak + "\",\"endTime\":\"" + kraj
                + "\",\"maxPoints\":100}";
        return pozovi(post("/api/teacher/courses/1/exam-sessions")
                .header(AUTH, bearer(NASTAVNIK))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private MockHttpServletResponse predaj(String username, int terminId, int zadatakId,
                                           String imeFajla, byte[] sadrzaj) throws Exception {
        return pozovi(multipart("/api/student/exam-sessions/" + terminId
                + "/assignments/" + zadatakId + "/submissions/final")
                .file(new MockMultipartFile("file", imeFajla, "application/zip", sadrzaj))
                .header(AUTH, bearer(username)));
    }

    private void postaviProjekatZaZadatak1() throws Exception {
        if (projekatPostavljen) {
            return;
        }
        MockHttpServletResponse rez = pozovi(multipart("/api/teacher/courses/1/exam-sessions/2/assignments/1/project")
                .file(new MockMultipartFile("file", "projekat.zip", "application/zip",
                        napraviZip("README.txt", "Resite zadatak")))
                .header(AUTH, bearer(NASTAVNIK)));
        // 200 = postavljen, 409 = već postoji (oba su u redu)
        assertThat(rez.getStatus()).isIn(200, 409);
        projekatPostavljen = true;
    }

    private MockHttpServletResponse pozovi(RequestBuilder zahtev) throws Exception {
        return mvc.perform(zahtev).andReturn().getResponse();
    }

    private String bearer(String username) throws Exception {
        String token = TOKENI.get(username);
        if (token == null) {
            MockHttpServletResponse rez = pozovi(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + username + "\",\"password\":\"" + LOZINKA + "\"}"));
            assertThat(rez.getStatus()).as("login za " + username).isEqualTo(200);
            Matcher m = JWT.matcher(telo(rez));
            assertThat(m.find()).as("odgovor na login mora sadržati JWT").isTrue();
            token = m.group();
            TOKENI.put(username, token);
        }
        return "Bearer " + token;
    }

    private static String telo(MockHttpServletResponse rez) throws Exception {
        return rez.getContentAsString(StandardCharsets.UTF_8);
    }

    private static byte[] resenje() throws IOException {
        return napraviZip(
                "Main.java", "linija1\nlinija2\nlinija3",
                "util/Helper.java", "class Helper {}");
    }

    private static byte[] napraviZip(String... imenaISadrzaj) throws IOException {
        ByteArrayOutputStream bajtovi = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bajtovi)) {
            for (int i = 0; i < imenaISadrzaj.length; i += 2) {
                out.putNextEntry(new ZipEntry(imenaISadrzaj[i]));
                out.write(imenaISadrzaj[i + 1].getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return bajtovi.toByteArray();
    }

    private static Path napraviPrivremeniFolder() {
        try {
            return Files.createTempDirectory("raf-lms-rules-storage");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}