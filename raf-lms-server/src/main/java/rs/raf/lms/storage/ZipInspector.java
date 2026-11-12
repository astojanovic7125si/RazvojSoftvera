package rs.raf.lms.storage;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import rs.raf.lms.exceptions.BadRequestException;
import rs.raf.lms.exceptions.InternalServerException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class ZipInspector {

    // Putanje svih fajlova u ZIP-u (bez foldera), sortirane
    public List<String> listFiles(Resource zipResource) {
        try (ZipFile zip = new ZipFile(zipResource.getFile())) {
            List<String> names = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    names.add(normalize(entry.getName()));
                }
            }
            Collections.sort(names);
            return names;
        } catch (IOException exception) {
            throw new InternalServerException("Ne mogu da pročitam ZIP predaje", exception);
        }
    }

    // Broj linija fajla unutar zipa, ako fajla nema baca 400
    public int countLines(Resource zipResource, String filePath) {
        String wanted = normalize(filePath);

        try (ZipFile zip = new ZipFile(zipResource.getFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (!entry.isDirectory() && normalize(entry.getName()).equals(wanted)) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
                        return (int) reader.lines().count();
                    }
                }
            }
        } catch (IOException | UncheckedIOException exception) {
            throw new InternalServerException("Ne mogu da pročitam ZIP predaje", exception);
        }

        throw new BadRequestException("Fajl ne postoji u predaji: " + wanted);
    }

    // Ujednačava zapis putanje: obrnute kose crte -> kose, bez početnog "./" i "/"
    public String normalize(String path) {
        String result = path.trim().replace('\\', '/');
        while (result.startsWith("./")) {
            result = result.substring(2);
        }
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }
}
