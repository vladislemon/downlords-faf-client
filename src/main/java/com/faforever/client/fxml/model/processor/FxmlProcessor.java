package com.faforever.client.fxml.model.processor;

import com.faforever.client.fxml.infrastructure.JavaNode;
import com.faforever.client.fxml.model.ControlFactory;
import com.faforever.client.fxml.model.GeneratorConfiguration;
import com.faforever.client.fxml.model.XmlDocToJavaNode;
import com.faforever.client.fxml.utils.OsUtils;
import com.faforever.client.fxml.utils.ReflectionResolver;
import com.faforever.client.fxml.utils.StringUtils;
import javafx.util.Pair;
import lombok.Data;
import org.w3c.dom.Document;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
public class FxmlProcessor {

  Set<String> imports = new HashSet<>();
  Path filePath;
  Path resourcePath;
  String fxClassName;
  String packageName;
  JavaNode javaNode;
  String viewType;
  ReflectionResolver resolver;
  Class<?> controllerClass;
  Constructor<?> controllerConstructor;
  Set<Class<?>> controllerParams = new LinkedHashSet<>();
  Set<Class<?>> fxObjectParams = new LinkedHashSet<>();
  Set<String> staticImports = new HashSet<>();
  Set<String> starImports = new HashSet<>();
  StringBuilder stringBuilder = new StringBuilder();
  List<String> buildControlsLines = new ArrayList<>();
  boolean initialize;

  public FxmlProcessor(Path filePath, Path resourcePath, String packageName) {
    Document doc = OsUtils.readXmlPlain(filePath);
    this.filePath = filePath;
    this.resourcePath = resourcePath;
    this.packageName = packageName;
    fxClassName = StringUtils.fxmlFileToJavaClass(filePath.toString());
    XmlDocToJavaNode xmlDataTranslator = new XmlDocToJavaNode();
    Pair<JavaNode, GeneratorConfiguration> result = xmlDataTranslator.buildNodeInfo(doc, imports);

    resolver = new ReflectionResolver(imports);
    javaNode = result.getKey();
    viewType = javaNode.getName();
    String controllerString = javaNode.extractAttribute("fx:controller");
    if (!controllerString.isBlank()) {
      controllerClass = resolver.resolve(controllerString);
      initialize = resolver.getMethod(controllerClass, "initialize", 0) != null;
      controllerConstructor = resolver.firstPublicConstructor(controllerClass);
      if (controllerConstructor != null) {
        controllerParams.addAll(List.of(controllerConstructor.getParameterTypes()));
      }
    } else {
      Path relativePath = resourcePath.relativize(filePath);
      if (relativePath.getNameCount() > 1) {
        String potentialController = "com.faforever.client." +
            relativePath.subpath(0, relativePath.getNameCount()).getParent().toString().replace(File.separatorChar, '.') + '.' +
            StringUtils.fxmlFileToControllerClass(filePath.getFileName().toString());
        controllerClass = resolver.resolve(potentialController);
        initialize = resolver.getMethod(controllerClass, "initialize", 0) != null;
      }
    }
    if (controllerClass == null) {
      throw new IllegalArgumentException("Fxml has no controller");
    }
    fxObjectParams.addAll(controllerParams);
    fxObjectParams.add(resolver.resolve("com.faforever.client.i18n.I18n"));
  }

  public void generateJava(Path outPath) {
    javaNode.extractAttribute("xmlns:fx");

    ControlFactory builder = new ControlFactory(this);
    buildControlsLines.addAll(builder.buildControl());
    starImports.addAll(resolver.getImports());
    staticImports.addAll(resolver.getFixedTypes().keySet());

    Path fullFilePath = outPath.resolve(fxClassName + ".java");
    OsUtils.writeAllText(fullFilePath, generateCode());
  }

  void appendln(String text) {
    stringBuilder.append(text);
    stringBuilder.append('\r');
    stringBuilder.append('\n');
  }

  void append(String text) {
    stringBuilder.append(text);
  }

  public String generateCode() {
    appendPackage();
    appendImports();
    appendClass();

    return stringBuilder.toString();
  }

  public void appendPackage() {
    if (!OsUtils.isNullOrEmpty(packageName)) {
      append("package ");
      append(packageName);
      appendln(";");
      appendln("");
    }
  }

  public void appendImports() {
    appendln("import com.faforever.client.fxml.FxObject;");
    for (String imprt : staticImports) {
      append("import ");
      append(imprt.replace("$", "."));
      appendln(";");
    }
    appendln("");
    for (String imprt : starImports) {
      append("import ");
      append(imprt);
      appendln(".*;");
    }

    for (Class<?> fxObjectParam : fxObjectParams) {
      append("import ");
      append(fxObjectParam.getName());
      appendln(";");
    }
    appendln("");
  }

  public void appendClass() {
    String controllerType = null;
    String typeVariableString = null;
    if (controllerClass != null) {
      controllerType = controllerClass.getName();
      TypeVariable<? extends Class<?>>[] typeVariables = controllerClass.getTypeParameters();
      if (typeVariables != null && typeVariables.length > 0) {
        typeVariableString = "<" + StringUtils.join(Arrays.stream(typeVariables).map(TypeVariable::getName)) + ">";
        controllerType += typeVariableString;
      }
    }
    append("public final class ");
    append(fxClassName);
    if (typeVariableString != null) {
      append(typeVariableString);
    }
    append(" extends FxObject<");
    if (!OsUtils.isNullOrEmpty(controllerType)) {
      append(controllerType);
    } else {
      append("Controller<Node>");
    }
    append(">");
    appendln(" {");

    appendln("");

    append("\tpublic ");
    append(viewType);
    appendln(" view;");

    fxObjectParams.forEach(fxObjectParam -> appendln("\tpublic " + fxObjectParam.getSimpleName() + " " + StringUtils.camelCase(fxObjectParam.getSimpleName()) + ";"));

    appendln("");

    append("\tpublic ");
    append(fxClassName);
    append("(");
    append(StringUtils.join(fxObjectParams.stream().map(fxObjectParam ->
        fxObjectParam.getSimpleName() + " " + StringUtils.camelCase(fxObjectParam.getSimpleName()))));
    appendln(") {");
    fxObjectParams.forEach(fxObjectParam -> {
      String paramName = StringUtils.camelCase(fxObjectParam.getSimpleName());
      appendln("\t\tthis." + paramName + " = " + paramName + ";");
    });
    appendln("}");
    appendln("");

    appendln("\tpublic void initialize() {");

    if (!OsUtils.isNullOrEmpty(controllerType) && !Modifier.toString(controllerClass.getModifiers()).contains("abstract")) {
      appendln("\t\tif (controller == null) {");
      append("\t\t\tcontroller = new ");
      append(controllerType);
      append("(");
      append(StringUtils.join(controllerParams.stream().map(controllerParam -> StringUtils.camelCase(controllerParam.getSimpleName()))));
      appendln(");");
      appendln("\t\t}");
    }

    for (String line : this.buildControlsLines) {
      append("\t\t");
      append(line);
      if (!line.isBlank()) {
        appendln(";");
      } else {
        appendln("");
      }
    }

    if (initialize) {
      appendln("\t\tcontroller.initialize();");
    }

    appendln("\t}");
    appendln("");
    appendln("}");
  }
}
