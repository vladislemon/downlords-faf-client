package com.faforever.client.fxml;

import com.faforever.client.fxml.model.reader.FxmlGenerator;
import com.faforever.client.fxml.utils.OsUtils;
import com.faforever.client.fxml.utils.StringUtils;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.util.ClassUtils;

import java.io.File;
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
                        compile(file, MappedTypesToCreate);
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

        String packageName = getPackageName(path);
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

    void compile(String file, List<String> mappedTypesToCreate) {
        FxmlGenerator processor = new FxmlGenerator(file, outPath);
        File fileData = new File(file);
        String packageName = getPackageName(Path.of(fileData.getParent()));
        String className = StringUtils.substringBeforeLast(fileData.getName(), ".");

        String fxClassName = "Fx" + StringUtils.snakeToCapitalize(className);
        processor.process(fxClassName, "com.faforever.client.fxml.compiled");
        if (OsUtils.isNullOrEmpty(packageName)) {
            mappedTypesToCreate.add(fxClassName.trim());
        } else {
            mappedTypesToCreate.add(packageName + "." + fxClassName.trim());
        }
    }

    String getPackageName(Path path) {
        String[] files = OsUtils.GetDirectoryFiles(path, false, file ->
            (file.getName().endsWith(".java")
                || file.getName().endsWith(".kt"))
                && (!file.getName().startsWith("Fx"))
        );

        for (String file : files) {
            List<String> lines = OsUtils.readAllLines(file);
            for (String line : lines) {
                String lineTrimmed = line.trim();
                if (!lineTrimmed.startsWith("package")) {
                    continue;
                }
                return StringUtils.removeSuffix(
                    StringUtils.removePrefix(lineTrimmed, "package"),
                    ";");
            }
        }
        return "";
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        run();
    }
}
