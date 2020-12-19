package com.faforever.client.fxml.model.reader;

import com.faforever.client.fxml.infrastructure.JavaNode;
import com.faforever.client.fxml.model.ControlFactory;
import com.faforever.client.fxml.model.GeneratorConfiguration;
import com.faforever.client.fxml.model.XmlDocToJavaNode;
import com.faforever.client.fxml.model.generator.CodeGenerator;
import com.faforever.client.fxml.utils.OsUtils;
import com.faforever.client.fxml.utils.ReflectionResolver;
import javafx.fxml.Initializable;
import javafx.util.Pair;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FxmlGenerator {

    private final Document _doc;
    List<String> imports = new ArrayList<>();
    Path path;

    public FxmlGenerator(String fileName, Path outPath) {
        _doc = OsUtils.readXmlPlain(fileName);
        path = outPath;
        if (path == null) {
            path = Path.of(".");
        }
    }

    public void process(String className, String packageName) {
        XmlDocToJavaNode xmlDataTranslator = new XmlDocToJavaNode();

        Pair<JavaNode, GeneratorConfiguration> result = xmlDataTranslator.buildNodeInfo(_doc, imports);

        JavaNode javaNode = result.getKey();
        GeneratorConfiguration configuration = result.getValue();

        ReflectionResolver resolver = new ReflectionResolver(imports);

        CodeGenerator codeGenerator = new CodeGenerator();

        setupCodeGenerator(codeGenerator, javaNode, resolver, className, packageName, configuration);

        String generatedCode = codeGenerator.generateCode();

        Path fullFilePath = path.resolve(codeGenerator.className + ".java");
        OsUtils.writeAllText(fullFilePath, generatedCode);
    }

    private void setupCodeGenerator(CodeGenerator codeGenerator, JavaNode javaNode,
                                    ReflectionResolver resolver, String clzName, String packageName, GeneratorConfiguration configuration) {
        codeGenerator.packageName = packageName;
        codeGenerator.className = clzName;
        codeGenerator.controllerType = javaNode.extractAttribute("fx:controller");
        if (!codeGenerator.controllerType.isBlank()) {
            codeGenerator.controllerConstructor = resolver.controllerConstructor(codeGenerator.controllerType);
            if (codeGenerator.controllerConstructor != null) {
                codeGenerator.controllerParams.addAll(List.of(codeGenerator.controllerConstructor.getParameterTypes()));
            }
        } else {
            throw new UnsupportedOperationException("Must have controller");
        }
        codeGenerator.viewType = javaNode.getName();
        javaNode.extractAttribute("xmlns:fx");

        ControlFactory builder = new ControlFactory(codeGenerator.controllerType, codeGenerator.buildControlsLines,
            javaNode, resolver, configuration);
        builder.process();
        codeGenerator.starImports.addAll(resolver.Imports);
        codeGenerator.staticImports.addAll(resolver.FixedTypes.keySet());
        setupInitializableControllerCode(codeGenerator, resolver);
    }

    private void setupInitializableControllerCode(CodeGenerator codeGenerator, ReflectionResolver resolver) {
        if (OsUtils.isNullOrEmpty(codeGenerator.controllerType)) {
            return;
        }
        Class<?> controllerClass = resolver.resolve(codeGenerator.controllerType);
        if (controllerClass == null || !Initializable.class.isAssignableFrom(controllerClass)) {
            return;
        }
        codeGenerator.buildControlsLines.add("controller.initialize(null, null);");
    }
}
