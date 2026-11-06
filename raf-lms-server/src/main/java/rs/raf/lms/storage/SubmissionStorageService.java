package rs.raf.lms.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipFile;

@Service
public class SubmissionStorageService {

    private final Path submissionDirectory;

    public SubmissionStorageService(
            @Value("${app.storage.root}") String storageRoot
    ) {

        this.submissionDirectory = Path.of(storageRoot)
                .toAbsolutePath()
                .normalize()
                .resolve("submissions");
    }

    // CUVANJE STUDENTSKOG ZIP RESENJA
    public StoredSubmission storeZip(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ZIP resenje nije poslato"
            );
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Fajl nema naziv"
            );
        }

        originalName = originalName.replace('\\', '/');
        originalName = originalName.substring(
                originalName.lastIndexOf('/') + 1
        );

        if (originalName.length() > 255 ||
                !originalName.toLowerCase(Locale.ROOT).endsWith(".zip")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dozvoljen je samo ZIP fajl"
            );
        }

        String storageKey = UUID.randomUUID() + ".zip";

        Path target = submissionDirectory.resolve(storageKey);

        try {

            Files.createDirectories(submissionDirectory);

            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }

            boolean validZip;

            try (ZipFile zip = new ZipFile(target.toFile())) {
                validZip = zip.size() > 0;

            } catch (IOException invalidZip) {
                validZip = false;
            }

            if (!validZip) {

                Files.deleteIfExists(target);

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Poslato resenje nije ispravna ZIP arhiva"
                );
            }

            return new StoredSubmission(
                    originalName,
                    storageKey
            );

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Neuspesno cuvanje resenja",
                    exception
            );
        }
    }

    // UCITAVANJE SACUVANOG RESENJA
    public Resource loadZip(String storageKey) {

        Path file = resolveStoredFile(storageKey);

        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Resenje nije pronadjeno"
            );
        }

        return new FileSystemResource(file);
    }

    // BRISANJE FAJLA AKO PREDaja NIJE PRIHVACENA
    public void deleteZip(String storageKey) {

        Path file = resolveStoredFile(storageKey);

        try {
            Files.deleteIfExists(file);

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Neuspesno brisanje privremene predaje",
                    exception
            );
        }
    }

    // BEZBEDNO ODREDJIVANJE PUTANJE DO FAJLA
    private Path resolveStoredFile(String storageKey) {

        if (storageKey == null ||
                !storageKey.matches("[0-9a-fA-F-]{36}\\.zip")) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Resenje nije pronadjeno"
            );
        }

        Path file = submissionDirectory
                .resolve(storageKey)
                .normalize();

        if (!file.startsWith(submissionDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Resenje nije pronadjeno"
            );
        }

        return file;
    }

    public record StoredSubmission(
            String originalFileName,
            String storageKey
    ) {
    }
}