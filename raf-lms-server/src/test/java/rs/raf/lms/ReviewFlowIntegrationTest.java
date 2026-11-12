package rs.raf.lms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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


//Tok pregleda i ocenjivanja kroz pravi HTTP sloj, H2 bazu i seed podatke.
//Seed: teacher1 (kurs 1), student0..student5 (indeksi 2023/0000..2023/0005, lozinka lozinka123),
//termin 2 je aktivan, zadatak 1 je za sve grupe, maksimum poena je 100.00.

@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:raflms-review;DB_CLOSE_DELAY=-1"
})
class ReviewFlowIntegrationTest {

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

    //ocenjivanje
    @Test
    @DisplayName("Student kome nastavnik nije dao ocenu vidi graded=false")
    void studentBezOceneVidiGradedFalse() throws Exception {
        predajResenje(1);

        MockHttpServletResponse rez = pozovi(get("/api/student/exam-sessions/2/result")
                .header(AUTH, bearer("student1")));

        assertThat(rez.getStatus()).isEqualTo(200);
        assertThat(telo(rez)).contains("\"graded\":false");
    }

    @Test
    @DisplayName("Ocena i komentari postaju vidljivi studentu tek kad nastavnik sačuva ocenu")
    void ocenaIKomentariVidljiviTekPosleOcene() throws Exception {
        long[] ids = predajResenje(2); // {submissionId, attemptId}

        MockHttpServletResponse komentar = pozovi(post("/api/teacher/courses/1/submissions/" + ids[0] + "/comments")
                .header(AUTH, bearer(NASTAVNIK))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"filePath\":\"Main.java\",\"lineNumber\":2,\"content\":\"Dodaj proveru praznog unosa\"}"));
        assertThat(komentar.getStatus()).isEqualTo(201);

        // pre ocene student ne vidi ni poene ni komentar
        String pre = telo(pozovi(get("/api/student/exam-sessions/2/result").header(AUTH, bearer("student2"))));
        assertThat(pre).contains("\"graded\":false").doesNotContain("Dodaj proveru");

        MockHttpServletResponse ocena = pozovi(put("/api/teacher/courses/1/attempts/" + ids[1] + "/grade")
                .header(AUTH, bearer(NASTAVNIK))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"points\":25.5,\"feedback\":\"Dobro uradjeno\"}"));
        assertThat(ocena.getStatus()).isEqualTo(200);
        assertThat(telo(ocena)).contains("\"points\":25.50").contains("\"maxPoints\":100.00");

        // posle ocene student vidi poene, povratnu informaciju i komentar
        String posle = telo(pozovi(get("/api/student/exam-sessions/2/result").header(AUTH, bearer("student2"))));
        assertThat(posle)
                .contains("\"graded\":true")
                .contains("\"points\":25.50")
                .contains("Dobro uradjeno")
                .contains("Dodaj proveru praznog unosa");
    }

    @Test
    @DisplayName("Ocena se menja (granice 0 i 100 važe) i uvek postoji samo jedna ocena po pokušaju")
    void izmenaOceneNeStvaraDruguOcenu() throws Exception {
        long[] ids = predajResenje(3);
        String url = "/api/teacher/courses/1/attempts/" + ids[1] + "/grade";

        assertThat(oceni(url, "{\"points\":0}").getStatus()).isEqualTo(200);
        assertThat(oceni(url, "{\"points\":100}").getStatus()).isEqualTo(200);
        assertThat(oceni(url, "{\"points\":30,\"feedback\":\"Posle razgovora\"}").getStatus()).isEqualTo(200);

        MockHttpServletResponse citanje = pozovi(get(url).header(AUTH, bearer(NASTAVNIK)));
        assertThat(citanje.getStatus()).isEqualTo(200);
        assertThat(telo(citanje)).contains("\"points\":30.00").contains("Posle razgovora");
    }

    @Test
    @DisplayName("Poeni veći od maksimuma, negativni i sa previše decimala se odbijaju; ocena se ne upisuje")
    void nevalidniPoeniSuOdbijeni() throws Exception {
        long[] ids = predajResenje(4);
        String url = "/api/teacher/courses/1/attempts/" + ids[1] + "/grade";

        assertThat(oceni(url, "{\"points\":150}").getStatus()).isEqualTo(400);
        assertThat(oceni(url, "{\"points\":-1}").getStatus()).isEqualTo(400);
        assertThat(oceni(url, "{\"points\":10.123}").getStatus()).isEqualTo(400);

        // ništa nije sačuvano, pa ocena ne postoji
        assertThat(pozovi(get(url).header(AUTH, bearer(NASTAVNIK))).getStatus()).isEqualTo(404);
    }

    //komentari na kod
    @Test
    @DisplayName("Komentari na kod: lista fajlova, dodavanje, validacije, izmena i brisanje")
    void komentariNaKod() throws Exception {
        long[] ids = predajResenje(5);
        String osnova = "/api/teacher/courses/1/submissions/" + ids[0];

        MockHttpServletResponse fajlovi = pozovi(get(osnova + "/files").header(AUTH, bearer(NASTAVNIK)));
        assertThat(fajlovi.getStatus()).isEqualTo(200);
        assertThat(telo(fajlovi)).contains("Main.java").contains("util/Helper.java");

        // Main.java ima tačno 3 linije: 3 je poslednja dozvoljena, 4 i 0 nisu
        MockHttpServletResponse dodat = komentar(osnova, "Main.java", 3, "Prvi komentar");
        assertThat(dodat.getStatus()).isEqualTo(201);
        long id = izvuciBroj(telo(dodat), "id");

        assertThat(komentar(osnova, "Main.java", 4, "van fajla").getStatus()).isEqualTo(400);
        assertThat(komentar(osnova, "Main.java", 0, "nulta linija").getStatus()).isEqualTo(400);
        assertThat(komentar(osnova, "nema/ovog/fajla.java", 1, "nepostojeci fajl").getStatus()).isEqualTo(400);
        assertThat(komentar(osnova, "Main.java", 1, " ").getStatus()).isEqualTo(400);

        assertThat(telo(pozovi(get(osnova + "/comments").header(AUTH, bearer(NASTAVNIK))))).contains("Prvi komentar");

        MockHttpServletResponse izmenjen = pozovi(put(osnova + "/comments/" + id)
                .header(AUTH, bearer(NASTAVNIK))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Izmenjen komentar\"}"));
        assertThat(izmenjen.getStatus()).isEqualTo(200);
        assertThat(telo(izmenjen)).contains("Izmenjen komentar");

        assertThat(pozovi(delete(osnova + "/comments/" + id).header(AUTH, bearer(NASTAVNIK))).getStatus())
                .isEqualTo(204);
        assertThat(telo(pozovi(get(osnova + "/comments").header(AUTH, bearer(NASTAVNIK))))).isEqualTo("[]");
    }

    //preuzimanje
    @Test
    @DisplayName("Nastavnik preuzima jednu predaju i sve predaje termina kao ZIP")
    void preuzimanjePredaja() throws Exception {
        long[] ids = predajResenje(0);

        MockHttpServletResponse jedna = pozovi(get("/api/teacher/courses/1/submissions/" + ids[0] + "/download")
                .header(AUTH, bearer(NASTAVNIK)));
        assertThat(jedna.getStatus()).isEqualTo(200);
        assertThat(jedna.getContentType()).contains("zip");
        assertThat(jedna.getContentAsByteArray()).isNotEmpty();

        MockHttpServletResponse sve = pozovi(get("/api/teacher/courses/1/submissions/download")
                .param("sessionId", "2")
                .header(AUTH, bearer(NASTAVNIK)));
        assertThat(sve.getStatus()).isEqualTo(200);
        assertThat(imenaUZipu(sve.getContentAsByteArray()))
                .anyMatch(ime -> ime.startsWith("termin-2/") && ime.contains("zadatak-1"));
    }

    //prava pristupa
    @Test
    @DisplayName("Bez tokena nema pristupa (401)")
    void bezTokenaJeNeovlascen() throws Exception {
        assertThat(pozovi(get("/api/teacher/courses/1/submissions")).getStatus()).isEqualTo(401);
        assertThat(pozovi(get("/api/student/exam-sessions/2/result")).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Student ne sme na nastavničke endpointe (403)")
    void studentNemaPristupNastavnickimEndpointima() throws Exception {
        assertThat(pozovi(get("/api/teacher/courses/1/submissions").header(AUTH, bearer("student1"))).getStatus())
                .isEqualTo(403);
        assertThat(oceniKao("student1", "/api/teacher/courses/1/attempts/1/grade", "{\"points\":10}").getStatus())
                .isEqualTo(403);
    }

    @Test
    @DisplayName("Nastavnik ne sme na studentske endpointe (403)")
    void nastavnikNemaPristupStudentskimEndpointima() throws Exception {
        assertThat(pozovi(get("/api/student/exam-sessions/2/result").header(AUTH, bearer(NASTAVNIK))).getStatus())
                .isEqualTo(403);
    }

    @Test
    @DisplayName("Pogrešna lozinka daje 401")
    void pogresnaLozinkaNijeDozvoljena() throws Exception {
        MockHttpServletResponse rez = pozovi(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"student1\",\"password\":\"pogresna\"}"));

        assertThat(rez.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Student bez pokušaja u terminu dobija 404")
    void studentBezPokusajaDobijaNotFound() throws Exception {
        // student0 u terminu 1 nije radio ništa
        MockHttpServletResponse rez = pozovi(get("/api/student/exam-sessions/1/result")
                .header(AUTH, bearer("student0")));

        assertThat(rez.getStatus()).isEqualTo(404);
    }

    //pomoćne metode
    //Ceo put jednog studenta: prijava na termin, preuzimanje zadatka i predaja konačnog rešenja
    //Vraća {submissionId, attemptId} koje nastavnik vidi u listi predaja
    private long[] predajResenje(int n) throws Exception {
        String username = "student" + n;
        String indeks = "2023/000" + n;

        postaviProjekatZaZadatak1();
        prijaviStudenta(indeks);

        MockHttpServletResponse preuzimanje = pozovi(get("/api/student/exam-sessions/2/assignments/1/project")
                .header(AUTH, bearer(username)));
        assertThat(preuzimanje.getStatus()).isEqualTo(200);

        MockHttpServletResponse predaja = pozovi(multipart("/api/student/exam-sessions/2/assignments/1/submissions/final")
                .file(new MockMultipartFile("file", "resenje.zip", "application/zip", resenje()))
                .header(AUTH, bearer(username)));
        assertThat(predaja.getStatus()).isEqualTo(201);

        MockHttpServletResponse lista = pozovi(get("/api/teacher/courses/1/submissions")
                .param("sessionId", "2")
                .param("indexNumber", indeks)
                .header(AUTH, bearer(NASTAVNIK)));
        assertThat(lista.getStatus()).isEqualTo(200);
        String json = telo(lista);
        assertThat(json).contains(indeks);
        return new long[] { izvuciBroj(json, "submissionId"), izvuciBroj(json, "attemptId") };
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

    private void prijaviStudenta(String indeks) throws Exception {
        String json = "{\"students\":[{\"indexNumber\":\"" + indeks + "\",\"groupName\":\"A\",\"classroom\":\"sala1\"}]}";
        MockHttpServletResponse rez = pozovi(post("/api/teacher/courses/1/exam-sessions/2/registrations")
                .header(AUTH, bearer(NASTAVNIK))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
        assertThat(rez.getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse oceni(String url, String json) throws Exception {
        return oceniKao(NASTAVNIK, url, json);
    }

    private MockHttpServletResponse oceniKao(String username, String url, String json) throws Exception {
        return pozovi(put(url)
                .header(AUTH, bearer(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private MockHttpServletResponse komentar(String osnova, String fajl, int linija, String sadrzaj) throws Exception {
        String json = "{\"filePath\":\"" + fajl + "\",\"lineNumber\":" + linija + ",\"content\":\"" + sadrzaj + "\"}";
        return pozovi(post(osnova + "/comments")
                .header(AUTH, bearer(NASTAVNIK))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
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

    //Telo odgovora čitamo kao UTF-8, da slova sa kvačicama ne budu pokvarena
    private static String telo(MockHttpServletResponse rez) throws Exception {
        return rez.getContentAsString(StandardCharsets.UTF_8);
    }

    private static long izvuciBroj(String json, String polje) {
        Matcher m = Pattern.compile("\"" + polje + "\":(\\d+)").matcher(json);
        assertThat(m.find()).as("polje " + polje + " mora postojati u odgovoru").isTrue();
        return Long.parseLong(m.group(1));
    }

    private static byte[] resenje() throws IOException {
        // Main.java ima tačno 3 linije (bez završnog novog reda)
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

    private static List<String> imenaUZipu(byte[] zip) throws IOException {
        List<String> imena = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                imena.add(entry.getName());
            }
        }
        return imena;
    }

    private static Path napraviPrivremeniFolder() {
        try {
            return Files.createTempDirectory("raf-lms-test-storage");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
