package com.heliostat.engine.repository;

import com.heliostat.engine.model.UserProfile;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserProfileRepository {
    private final Path storageDirectory;

    public UserProfileRepository(String customPath) {
        this.storageDirectory = Paths.get(customPath);
        try {
            // Ensure the storage directory exists on startup
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local file system storage directory", e);
        }
    }

    private Path getFilePath(String profileId) {
        return storageDirectory.resolve(profileId + ".dat");
    }

    // CREATE & UPDATE (Save/Upsert)
    public void save(UserProfile profile) {
        Path filePath = getFilePath(profile.getId());
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(filePath))) {
            oos.writeObject(profile);
        } catch (IOException e) {
            throw new RuntimeException("File System Error: Failed to write profile data for ID " + profile.getId(), e);
        }
    }

    // READ (Find by ID)
    public Optional<UserProfile> findById(String profileId) {
        Path filePath = getFilePath(profileId);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(filePath))) {
            UserProfile profile = (UserProfile) ois.readObject();
            return Optional.of(profile);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("File System Error: Failed to read profile data for ID " + profileId, e);
        }
    }

    // READ ALL
    public List<UserProfile> findAll() {
        List<UserProfile> profiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.dat")) {
            for (Path entry : stream) {
                String filename = entry.getFileName().toString();
                String id = filename.substring(0, filename.lastIndexOf('.'));
                findById(id).ifPresent(profiles::add);
            }
        } catch (IOException e) {
            throw new RuntimeException("File System Error: Failed to scan profiles directory", e);
        }
        return profiles;
    }

    // DELETE
    public boolean deleteById(String profileId) {
        Path filePath = getFilePath(profileId);
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("File System Error: Failed to remove profile file for ID " + profileId, e);
        }
    }
}
