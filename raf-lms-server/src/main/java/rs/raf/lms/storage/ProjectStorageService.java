package rs.raf.lms.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import rs.raf.lms.exceptions.BadRequestException;
import rs.raf.lms.exceptions.InternalServerException;
import rs.raf.lms.exceptions.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipFile;

@Service
public class ProjectStorageService {

    private final Path assignmentDirectory;

    public ProjectStorageService(@Value("${app.storage.root}") String storageRoot) {
        this.assignmentDirectory = Path.of(storageRoot)
                .toAbsolutePath()
                .normalize()
                .resolve("assignments");
    }

    public StoredProject storeZip(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("ZIP fajl nije poslat");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new BadRequestException("Fajl nema naziv");
        }

        originalName = originalName.replace('\\', '/');//Čišćenje imena
        originalName = originalName.substring(originalName.lastIndexOf('/') + 1);

        if (originalName.length() > 255
                || !originalName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new BadRequestException("Dozvoljen je samo ZIP fajl");
        }

        String storageKey = UUID.randomUUID() + ".zip";
        Path target = assignmentDirectory.resolve(storageKey);

        try {
            Files.createDirectories(assignmentDirectory);//napravi folder ako ne postoji
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);//prepiši sadržaj u fajl
            }

            boolean validZip;
            try (ZipFile zip = new ZipFile(target.toFile())) {
                validZip = zip.size() > 0;
            } catch (IOException invalidZip) {
                validZip = false;
            }

            if (!validZip) {
                Files.deleteIfExists(target);
                throw new BadRequestException("Poslati fajl nije ispravna ZIP arhiva"
                );
            }

            return new StoredProject(originalName, storageKey);
        } catch (IOException exception) {
            throw new InternalServerException("Neuspesno cuvanje ZIP fajla", exception);
        }
    }

    public Resource loadZip(String storageKey) {
        if (storageKey == null || !storageKey.matches("[0-9a-fA-F-]{36}\\.zip")) {
            throw new ResourceNotFoundException("Projekat nije pronadjen");
        }

        Path file = assignmentDirectory.resolve(storageKey).normalize();
        if (!file.startsWith(assignmentDirectory) || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Projekat nije pronadjen");
        }

        return new FileSystemResource(file);
    }

    public record StoredProject(String originalFileName, String storageKey) {
    }
}
