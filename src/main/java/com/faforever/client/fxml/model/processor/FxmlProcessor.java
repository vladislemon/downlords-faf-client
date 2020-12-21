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

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class FxmlProcessor {

  List<String> imports = new ArrayList<>();
  Path filePath;
  String fxClassName;
  String packageName;
  JavaNode javaNode;
  String viewType;
  ReflectionResolver resolver;
  Class<?> controllerClass;
  Constructor<?> controllerConstructor;
  List<Class<?>> controllerParams = new ArrayList<>();
  Set<Class<?>> fxObjectParams = new HashSet<>();
  List<String> staticImports = new ArrayList<>();
  List<String> starImports = new ArrayList<>();
  StringBuilder stringBuilder = new StringBuilder();
  List<String> buildControlsLines = new ArrayList<>();
  boolean initialize;

  public FxmlProcessor(Path filePath, String packageName) {
    Document doc = OsUtils.readXmlPlain(filePath);
    this.filePath = filePath;
    this.packageName = packageName;
    fxClassName = StringUtils.fxmlFileToJavaClass(filePath.toString());
    XmlDocToJavaNode xmlDataTranslator = new XmlDocToJavaNode();
    Pair<JavaNode, GeneratorConfiguration> result = xmlDataTranslator.buildNodeInfo(doc, imports);

    resolver = new ReflectionResolver(imports);
    javaNode = result.getKey();
    controllerClass = resolver.resolve(javaNode.extractAttribute("fx:controller"));
    viewType = javaNode.getName();
    if (controllerClass != null) {
      initialize = resolver.getMethod(controllerClass, "initialize", 0) != null;
      controllerConstructor = resolver.firstPublicConstructor(controllerClass);
      if (controllerConstructor != null) {
        controllerParams.addAll(List.of(controllerConstructor.getParameterTypes()));
      }
    } else {
      staticImports.add("javafx.scene.Node");
    }

    fxObjectParams.addAll(controllerParams);
    fxObjectParams.add(resolver.resolve("com.faforever.client.i18n.I18n"));
  }

  public void generateJava(Path outPath) {
    javaNode.extractAttribute("xmlns:fx");

    ControlFactory builder = new ControlFactory(this);
    buildControlsLines.addAll(builder.buildControl());
    starImports.addAll(resolver.Imports);
    staticImports.addAll(resolver.FixedTypes.keySet());

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
    if (controllerClass != null) {
      controllerType = controllerClass.getName();
    }
    append("public final class ");
    append(fxClassName);
    append(" extends FxObject<");
    if (!OsUtils.isNullOrEmpty(controllerType)) {
      append(controllerType);
    } else {
      append("Controller<Node>");
    }
    append(">");
    appendln(" {");

    appendln("");

    if (!OsUtils.isNullOrEmpty(controllerType)) {
      append("\tpublic ");
      append(controllerType);
      appendln(" controller;");
    }

    append("\tpublic ");
    append(viewType);
    appendln(" view;");

    appendln("");

    append("\tpublic ");
    append(fxClassName);
    append("(");
    append(StringUtils.join(fxObjectParams.stream().map(fxObjectParam ->
        fxObjectParam.getSimpleName() + " " + StringUtils.camelCase(fxObjectParam.getSimpleName()))));
    appendln(") {");

    if (!OsUtils.isNullOrEmpty(controllerType)) {
      append("\t\tcontroller = new ");
      append(controllerType);
      append("(");
      append(StringUtils.join(controllerParams.stream().map(controllerParam -> StringUtils.camelCase(controllerParam.getSimpleName()))));
      appendln(");");
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
      appendln("controller.initialize();");
    }

    appendln("\t}");
    appendln("");
    appendln("}");
  }
}
