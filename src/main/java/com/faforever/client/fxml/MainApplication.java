package com.faforever.client.fxml;

import com.faforever.client.fxml.model.processor.FxmlProcessor;
import com.faforever.client.fxml.utils.OsUtils;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

public class MainApplication extends Application {
    public Path outPath = Path.of("src", "main", "java", ClassUtils.classPackageAsResourcePath(MainApplication.class)).resolve("compiled");

    public static void main(String[] args) {
        Application.launch();

    }

    void run() throws URISyntaxException, IOException {
        Files.createDirectories(outPath);
        URL url = this.getClass().getClassLoader().getResource("theme");
        if (url != null) {
            Path inPath = Path.of(url.toURI());

            boolean generatePreloader = false;
            int failed = 0;
            String[] files = OsUtils.GetDirectoryFiles(inPath, true,
                file -> file.getName().endsWith(".fxml"));
            List<String> MappedTypesToCreate = new ArrayList<>();
            for (String file : files) {
                try {
                    if (file.endsWith(".fxml")) {
                        out.println("To compile: " + file);
                        compile(Path.of(file), MappedTypesToCreate);
                        out.println("SUCCESS");
                    }
                } catch (Exception e) {
                    failed++;
                    out.println("FAILED");
                    e.printStackTrace(out);
                }
            }
            if (generatePreloader) {
                computePreloader(inPath, MappedTypesToCreate);
            }
            out.println(String.format("%d out of %d files compiled", files.length - failed, files.length));
        }
        System.exit(0);
    }

    private void computePreloader(Path path, List<String> mappedTypesToCreate) {
        Path fullFilePath = outPath.resolve("FXPreloader.java");
        StringBuilder stringBuilder = new StringBuilder();

        String packageName = OsUtils.getPackageName(path);
        if (!OsUtils.isNullOrEmpty(packageName)) {
            stringBuilder.append("package ");
            stringBuilder.append("com.faforever.client.fxml");
            stringBuilder.append(";");
            stringBuilder.append("\n\r");
        }
        stringBuilder.append("public class FXPreloader {");
        stringBuilder.append("\n\r");
        stringBuilder.append(" public static void preload() {");
        stringBuilder.append("\n\r");
        for (String clazz : mappedTypesToCreate) {
            stringBuilder.append(" new ").append(clazz).append("();").append("\n\r");
        }

        stringBuilder.append("}");
        stringBuilder.append("}");
        stringBuilder.append("\n\r");

        String generatedCode = stringBuilder.toString();
        OsUtils.writeAllText(fullFilePath, generatedCode);
    }

    void compile(Path filePath, List<String> mappedTypesToCreate) {
        FxmlProcessor processor = new FxmlProcessor(filePath, "com.faforever.client.fxml.compiled");

        processor.generateJava(outPath);
        if (OsUtils.isNullOrEmpty(processor.getPackageName())) {
            mappedTypesToCreate.add(processor.getFxClassName().trim());
        } else {
            mappedTypesToCreate.add(processor.getPackageName() + "." + processor.getFxClassName().trim());
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        run();
    }
}
