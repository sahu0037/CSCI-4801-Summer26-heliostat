package com.heliostat.engine.repository;

import com.heliostat.engine.model.Reward;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RewardRepository {
    private final Path storageDirectory;

    public RewardRepository(String customPath) {
        this.storageDirectory = Paths.get(customPath);
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize Reward storage", e);
        }
    }

    private Path getFilePath(String id) {
        return storageDirectory.resolve(id + ".dat");
    }

    public void save(Reward reward) {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(getFilePath(reward.getId())))) {
            oos.writeObject(reward);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save reward: " + reward.getId(), e);
        }
    }

    public Optional<Reward> findById(String id) {
        Path filePath = getFilePath(id);
        if (!Files.exists(filePath)) return Optional.empty();

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(filePath))) {
            return Optional.of((Reward) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to read reward: " + id, e);
        }
    }

    public List<Reward> findAll() {
        List<Reward> rewards = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.dat")) {
            for (Path entry : stream) {
                String filename = entry.getFileName().toString();
                String id = filename.substring(0, filename.lastIndexOf('.'));
                findById(id).ifPresent(rewards::add);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan rewards catalog", e);
        }
        return rewards;
    }
}
