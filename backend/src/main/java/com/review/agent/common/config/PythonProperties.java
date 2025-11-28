package com.review.agent.common.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Data
@ConfigurationProperties(prefix = "app.python")
public class PythonProperties {
    private Path interpreter;
    private Path scriptDir;

    @PostConstruct
    public void init() throws IOException {
        if (interpreter == null) {
            interpreter = findSystemPython();
        }
        if (!Files.exists(interpreter)) {
            throw new IllegalStateException("Python interpreter not found: " + interpreter);
        }
        Files.createDirectories(scriptDir);
    }

    private static Path findSystemPython() {
        return Arrays.stream(System.getenv("PATH").split(File.pathSeparator))
                     .map(Paths::get)
                     .map(p -> p.resolve("python"))
                     .filter(Files::exists)
                     .findFirst()
                     .orElseThrow(() -> new IllegalStateException("python not in PATH"));
    }
}