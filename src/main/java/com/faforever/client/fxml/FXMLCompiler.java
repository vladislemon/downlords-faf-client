package com.faforever.client.fxml;

import com.faforever.client.fxml.model.processor.FxmlProcessor;
import com.faforever.client.fxml.utils.OsUtils;
import com.sun.javafx.application.PlatformImpl;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

public class FXMLCompiler {
    public Path outPath = Path.of("src", "main", "java", ClassUtils.classPackageAsResourcePath(FXMLCompiler.class)).resolve("compiled");

    public static void main(String[] args) throws IOException, URISyntaxException {
        FXMLCompiler fxmlCompiler = new FXMLCompiler();

        PlatformImpl.startup(fxmlCompiler::run);
    }

    void run() {
        try {
            Files.createDirectories(outPath);
            URL url = this.getClass().getClassLoader().getResource("theme");
            if (url != null) {
                Path resourcePath = Path.of(url.toURI());

                boolean generatePreloader = false;
                int failed = 0;
                String[] files = OsUtils.GetDirectoryFiles(resourcePath, true,
                    file -> file.getName().endsWith(".fxml"));
                List<String> MappedTypesToCreate = new ArrayList<>();
                for (String file : files) {
                    try {
                        if (file.endsWith(".fxml")) {
                            out.println("To compile: " + file);
                            compile(Path.of(file), resourcePath, MappedTypesToCreate);
                            out.println("SUCCESS");
                        }
                    } catch (Exception e) {
                        failed++;
                        out.println("FAILED");
                        e.printStackTrace(out);
                    }
                }
                if (generatePreloader) {
                    computePreloader(resourcePath, MappedTypesToCreate);
                }
                out.println(String.format("%d out of %d files compiled", files.length - failed, files.length));
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    void compile(Path filePath, Path resourcePath, List<String> mappedTypesToCreate) {
        FxmlProcessor processor = new FxmlProcessor(filePath, resourcePath, "com.faforever.client.fxml.compiled");

        processor.generateJava(outPath);
        if (OsUtils.isNullOrEmpty(processor.getPackageName())) {
            mappedTypesToCreate.add(processor.getFxClassName().trim());
        } else {
            mappedTypesToCreate.add(processor.getPackageName() + "." + processor.getFxClassName().trim());
        }
    }
}
