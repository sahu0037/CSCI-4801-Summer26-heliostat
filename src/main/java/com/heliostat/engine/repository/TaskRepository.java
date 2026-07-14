package com.heliostat.engine.repository;

import com.heliostat.engine.model.Task;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TaskRepository {
    private final Path storageDirectory;

    public TaskRepository(String customPath) {
        this.storageDirectory = Paths.get(customPath);
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize Task storage directory", e);
        }
    }

    private Path getFilePath(String taskId) {
        return storageDirectory.resolve(taskId + ".dat");
    }

    public void save(Task task) {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(getFilePath(task.getId())))) {
            oos.writeObject(task);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save task to file system: " + task.getId(), e);
        }
    }

    public Optional<Task> findById(String taskId) {
        Path filePath = getFilePath(taskId);
        if (!Files.exists(filePath)) return Optional.empty();

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(filePath))) {
            return Optional.of((Task) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to read task from disk: " + taskId, e);
        }
    }

    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.dat")) {
            for (Path entry : stream) {
                String filename = entry.getFileName().toString();
                String id = filename.substring(0, filename.lastIndexOf('.'));
                findById(id).ifPresent(tasks::add);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan tasks directory", e);
        }
        return tasks;
    }

    public boolean deleteById(String taskId) {
        try {
            return Files.deleteIfExists(getFilePath(taskId));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete task file: " + taskId, e);
        }
    }
}
