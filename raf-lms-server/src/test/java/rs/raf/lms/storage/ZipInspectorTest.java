package rs.raf.lms.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import rs.raf.lms.exceptions.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

class ZipInspectorTest {

    private final ZipInspector inspector = new ZipInspector();

    //JUnit za svaki test napravi privremeni folder i obriše ga kad test završi
    @TempDir
    Path privremeniFolder;


    //Pravi ZIP kao pravi fajl na disku (ZipInspector čita fajl, ne bajtove iz memorije).
    //Argumenti idu u parovima: ime fajla, sadržaj. Ime koje se završava sa "/" je folder

    private Resource zip(String... imenaISadrzaj) throws IOException {
        ByteArrayOutputStream bajtovi = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bajtovi)) {
            for (int i = 0; i < imenaISadrzaj.length; i += 2) {
                String ime = imenaISadrzaj[i];
                out.putNextEntry(new ZipEntry(ime));
                if (!ime.endsWith("/")) {
                    out.write(imenaISadrzaj[i + 1].getBytes(StandardCharsets.UTF_8));
                }
                out.closeEntry();
            }
        }
        Path fajl = Files.createTempFile(privremeniFolder, "predaja", ".zip");
        Files.write(fajl, bajtovi.toByteArray());
        return new FileSystemResource(fajl);
    }

    @Test
    @DisplayName("listFiles vraća samo fajlove (bez foldera), sortirano")
    void listFilesVracaSamoFajloveSortirano() throws Exception {
        Resource predaja = zip(
                "util/Helper.java", "class Helper {}",
                "util/", "",
                "Main.java", "linija1\nlinija2\nlinija3");

        assertThat(inspector.listFiles(predaja)).containsExactly("Main.java", "util/Helper.java");
    }

    @Test
    @DisplayName("countLines broji linije u fajlu")
    void countLinesBroji() throws Exception {
        Resource predaja = zip("Main.java", "linija1\nlinija2\nlinija3");

        assertThat(inspector.countLines(predaja, "Main.java")).isEqualTo(3);
    }

    @Test
    @DisplayName("Putanja se normalizuje: ./, vodeća kosa crta i obrnuta kosa crta")
    void putanjaSeNormalizuje() throws Exception {
        Resource predaja = zip("util/Helper.java", "class Helper {}");

        assertThat(inspector.countLines(predaja, "util/Helper.java")).isEqualTo(1);
        assertThat(inspector.countLines(predaja, "./util/Helper.java")).isEqualTo(1);
        assertThat(inspector.countLines(predaja, "/util/Helper.java")).isEqualTo(1);
        assertThat(inspector.countLines(predaja, "util\\Helper.java")).isEqualTo(1);
    }

    @Test
    @DisplayName("Fajl koji ne postoji u predaji daje BadRequestException")
    void nepostojeciFajlJeGreska() throws Exception {
        Resource predaja = zip("Main.java", "linija1");

        assertThatThrownBy(() -> inspector.countLines(predaja, "nema/ovog/fajla.java"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Fajl ne postoji");
    }
}