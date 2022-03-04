package com.faforever.client.fxml.model;

import com.faforever.client.fxml.infrastructure.JavaNode;
import com.faforever.client.fxml.infrastructure.TypeCode;
import com.faforever.client.fxml.model.processor.FxmlProcessor;
import com.faforever.client.fxml.model.reflectutils.ConvertUtils;
import com.faforever.client.fxml.utils.OsUtils;
import com.faforever.client.fxml.utils.StringUtils;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.layout.Priority;
import lombok.Data;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.faforever.client.fxml.model.reflectutils.ConvertUtils.addCodeValues;
import static com.faforever.client.fxml.model.reflectutils.ConvertUtils.handleSettingInnerText;
import static com.faforever.client.fxml.utils.StringUtils.quote;
import static java.lang.System.out;
import static java.text.MessageFormat.format;

@Data
public class ControlFactory {

  public static final String FX_NODE_ID = "fx:id";
  public static final String FX_CONSTANT = "fx:constant";
  public static final String FX_INCLUDE = "fx:include";
  public static final String FX_VALUE = "fx:value";
  public static final String FX_SOURCE = "source";

  public static final Set<String> SPECIAL_CONSTRUCTOR = Set.of("BarChart", "LineChart", "Image", "FXCollections", FX_INCLUDE);
  public static final Set<String> LIST_CHILDEREN = Set.of("children", "items", "rowConstraints", "columnConstraints",
      "buttons", "columns");
  public static final Set<String> SETTER_CHILDEREN = Set.of("content", "graphic", "font", "toggleGroup", "header",
      "tooltip", "columnResizePolicy", "valueFactory", "right", "left", "bottom", "top", "center");
  public static final Set<String> ITEMS_CHILD = Set.of("SplitPane");
  public static final Set<String> CONTENT_CHILD = Set.of("Tab", "ScrollPane", "TitledPane");
  public static final Set<String> TABS_CHILD = Set.of("TabPane");
  public static final Set<String> IMAGE_CHILD = Set.of("ImageView");


  public static final String PROP_POSTFIX_INT = "index"; // complex node properties ending, like  -rowindex/-columnindex

  public static Map<String, Class<?>> specPropClass = new LinkedHashMap<>();


  static {
    specPropClass.put("alignment", Pos.class);
    specPropClass.put("halignment", HPos.class);
    specPropClass.put("valignment", VPos.class);
    specPropClass.put("hgrow", Priority.class);
    specPropClass.put("vgrow", Priority.class);
    specPropClass.put("row", Integer.class);
    specPropClass.put("column", Integer.class);
  }

  private final FxmlProcessor fxmlProcessor;

  private final List<String> controlLines = new ArrayList<>();
  private final Map<String, Integer> controlIndexMap = new HashMap<>();

  public List<String> buildControl() {
    setupControl(fxmlProcessor.getJavaNode());
    addCodeLine(format("view = {0};", fxmlProcessor.getJavaNode().getControlName()));
    return controlLines;
  }

  private void setupControl(JavaNode javaNode) {
    fxmlProcessor.getResolver().resolve(javaNode);
    setControlName(javaNode);
    String controlName = javaNode.getControlName();
    String name = javaNode.getName();

    if (SPECIAL_CONSTRUCTOR.contains(name)) {
      handleSpecialNode(javaNode);
    } else if (name.contains("SpinnerValueFactory")) {
      handleSpinnerValueFactory(javaNode);
    } else if (javaNode.getAttributes().containsKey(FX_CONSTANT)) {
      String value = javaNode.getAttributes().get(FX_CONSTANT);
      javaNode.setControlName(format("{0}.{1}", name, value));
    } else if (javaNode.getAttributes().containsKey(FX_VALUE)) {
      String value = javaNode.getAttributes().get(FX_VALUE);
      javaNode.setControlName(format("{0}.valueOf(\"{1}\")", name, value));
    } else if (fxmlProcessor.getResolver().hasDefaultConstructor(javaNode)) {
      addCodeLine(format("{0} {1} = new {2}()",
          name,
          controlName,
          name));

      setupId(javaNode);
      setupAttributes(javaNode);
    } else {
      if (javaNode.getName().equals("Font")) {
        throw new UnsupportedOperationException("Font not supported");
      }
      String codeValues = addCodeValues(javaNode);

      addCodeLine(codeValues);
    }

    addLineBreak();

    if (javaNode.hasChildren()) {
      buildChildrenControls(javaNode);
      addLineBreak();
    }
  }

  private void buildChildrenControls(JavaNode javaNode) {
    Class<?> parentClass = javaNode.getClazz();
    String parentControl = javaNode.getControlName();
    for (JavaNode child : javaNode.getChildren()) {
      if (handleSpecialChild(child)) {
        continue;
      }
      if (!OsUtils.isNullOrEmpty(child.getInnerText())) {
        String codeLine = handleSettingInnerText(fxmlProcessor.getResolver(), parentClass, child, child.getInnerText(), parentControl);
        addCodeLine(codeLine);
        continue;
      }
      setupControl(child);
      addChild(child);
    }
  }

  private void addChild(JavaNode javaNode) {
    String controlName = javaNode.getControlName();
    String parentName = javaNode.getParent().getControlName();
    String codeLine;
    if (ITEMS_CHILD.contains(javaNode.getParent().getName())) {
      codeLine = format("{0}.getItems().add({1})", parentName, controlName);
    } else if (CONTENT_CHILD.contains(javaNode.getParent().getName())) {
      codeLine = format("{0}.setContent({1})", parentName, controlName);
    } else if (TABS_CHILD.contains(javaNode.getParent().getName())) {
      codeLine = format("{0}.getTabs().add({1})", parentName, controlName);
    } else if (IMAGE_CHILD.contains(javaNode.getParent().getName())) {
      codeLine = format("{0}.setImage({1})", parentName, controlName);
    } else {
      codeLine = format("{0}.getChildren().add({1})", parentName, controlName);
    }
    addCodeLine(codeLine);
    addLineBreak();
  }

  private void setControlName(JavaNode javaNode) {
    String controlName = javaNode.getAttributes().get(FX_NODE_ID);
    if (controlName == null) {
      String name = javaNode.getName();
      if (name.equals(FX_INCLUDE)) {
        name = StringUtils.fxmlFileToJavaClass(javaNode.getAttributes().get(FX_SOURCE)).replace("Fx", "");
      }
      if (!controlIndexMap.containsKey(name)) {
        controlIndexMap.put(name, 1);
      }
      controlName = MessageFormat.format("{0}{1}", StringUtils.camelCase(name), String.valueOf(controlIndexMap.get(name)));
      controlIndexMap.put(name, controlIndexMap.get(name) + 1);
    }
    javaNode.setControlName(controlName);
  }

  private String getControlName(String name) {
    if (!controlIndexMap.containsKey(name)) {
      controlIndexMap.put(name, 1);
    }
    String controlName = MessageFormat.format("{0}{1}", StringUtils.camelCase(name), controlIndexMap.get(name));
    controlIndexMap.put(name, controlIndexMap.get(name) + 1);
    return controlName;
  }

  private void setupId(JavaNode javaNode) {
    String id = javaNode.extractAttribute(FX_NODE_ID);
    String controlName = javaNode.getControlName();

    if (OsUtils.isNullOrEmpty(id)) {
      return;
    }
    if (fxmlProcessor.getControllerClass() != null && fxmlProcessor.getResolver().hasProperty(fxmlProcessor.getControllerClass(), controlName)) {
      addCodeLine(format("controller.{0} = {1}", id, controlName));
    } else if (fxmlProcessor.getControllerClass() == null) {
      addCodeLine("if (controller != null) {");
      addCodeLine(format("controller.{0} = {1}", id, controlName));
      addCodeLine("}");
    }
  }

  private void setupAttributes(JavaNode javaNode) {
    String controlName = javaNode.getControlName();
    Class<?> controlClass = javaNode.getClazz();

    Map<String, String> attrs = javaNode.getAttributes();
    for (Entry<String, String> attr : attrs.entrySet()) {
      String attrName = attr.getKey();
      String attrValue = attr.getValue();
      String codeLine;
      if (handleSpecialAttribute(attr, controlName)) {
        continue;
      } else {
        if (staticMethodHandleAttribute(attr, controlName)) {
          continue;
        }
        Method resolvedMethod = fxmlProcessor.getResolver().resolvePropertyMethod(controlClass, attrName, true);

        if (resolvedMethod == null) {
          if (isNodeProperty(attrName)) {
            setupNodeProperty(attrName, attrValue, controlName);
          } else {
            out.println("cannot find method '" + attrName + "'");
          }
          continue;
        }

        Class<?> parameterType = resolvedMethod.getParameterTypes()[0];
        codeLine = buildFunctionCode(attr.getValue(), controlName, resolvedMethod.getName(), parameterType);
      }
      addCodeLine(codeLine);
    }
  }

  private boolean staticMethodHandleAttribute(Entry<String, String> attr, String controlName) {
    String attrName = attr.getKey();
    String attrValue = attr.getValue();
    if (!attrName.contains(".")) {
      return false;
    }
    String[] itemsExpression = attrName.split("\\.");

    String className = itemsExpression[0];
    String staticMethodName = itemsExpression[1];

    Class<?> clsResolved = fxmlProcessor.getResolver().resolve(className);
    if (clsResolved == null) {
      out.println("staticMethodHandleAttribute cannot resolve" + className);
      return false;
    }

    Method setterMethod = fxmlProcessor.getResolver().resolveClassStaticSetter(clsResolved, staticMethodName);
    fxmlProcessor.getResolver().getFixedTypes().put(setterMethod.getParameterTypes()[1].getName(), setterMethod.getParameterTypes()[1]);
    String parameterValue = prepareFunctionParam(attrValue, setterMethod.getParameterTypes()[1]);
    String codeLine = format("{0}.{1}({2}, {3})", className, setterMethod.getName(), controlName, parameterValue);
    addCodeLine(codeLine);
    return true;
  }

  private boolean handleSpecialAttribute(Entry<String, String> attr, String controlName) {
    String attrName = attr.getKey();
    String attrValue = attr.getValue();
    switch (attrName) {
      case "xmlns:fx", "xmlns" -> {
        return true;
      }
      case "stylesheets" -> {
        String codeLine = format("{0}.getStylesheets().add(\"{1}\")", controlName, attrValue.substring(1));
        addCodeLine(codeLine);
        return true;
      }
      case "styleClass" -> {
        String codeLine = format("{0}.getStyleClass().add(\"{1}\")", controlName, attrValue);
        addCodeLine(codeLine);
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  private void handleSpecialNode(JavaNode javaNode) {
    String name = javaNode.getName();
    switch (name) {
      case "BarChart" -> handleChart(javaNode);
      case "FXCollections" -> handleCollections(javaNode);
      case "Image" -> handleImage(javaNode);
      case FX_INCLUDE -> handleInclude(javaNode);
      default -> {
      }
    }
  }

  private boolean handleSpecialChild(JavaNode child) {
    String childName = child.getName();
    if (LIST_CHILDEREN.contains(childName)) {
      handleListChild(child);
      return true;
    } else if (SETTER_CHILDEREN.contains(childName)) {
      handleSetterChild(child);
      return true;
    }
    switch (childName) {
      case "styleClass" -> {
        handleStyleClass(child);
        return true;
      }
      case "GridPane.margin", "VBox.margin", "HBox.margin", "BorderPane.margin" -> {
        handleMargin(child);
        return true;
      }
      case "padding" -> {
        handlePadding(child);
        return true;
      }
      case "Insets" -> {
        handleInsets(child);
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  private void handleStyleClass(JavaNode child) {
    for (JavaNode childStyleRow : child.getChildren()) {
      setupControl(childStyleRow);
      String codeLine = format("{0}.getStyleClass().add({1})", child.getParent().getControlName(), childStyleRow.getControlName());
      addCodeLine(codeLine);
    }
  }

  private void handleListChild(JavaNode listChild) {
    for (JavaNode listItem : listChild.getChildren()) {
      setupControl(listItem);
      String codeLine = format("{0}.get{1}().add({2})", listChild.getParent().getControlName(), StringUtils.capitalize(listChild.getName()), listItem.getControlName());
      addCodeLine(codeLine);
      addLineBreak();
    }
  }

  private void handleSetterChild(JavaNode setterWrapper) {
    JavaNode value = setterWrapper.getChildren().get(0);
    setupControl(value);
    String codeLine = format("{0}.set{1}({2})", setterWrapper.getParent().getControlName(), StringUtils.capitalize(setterWrapper.getName()), value.getControlName());
    addCodeLine(codeLine);
  }

  private void handleMargin(JavaNode marginWrapper) {
    Map<String, Double> marginMap = insetMapFromChildren(marginWrapper);
    String containerName = marginWrapper.getName().split("\\.")[0];
    String controlName = getControlName("Insets");
    String codeLine = format("Insets {0} = new Insets({1}, {2}, {3}, {4})", controlName,
        marginMap.get("top"), marginMap.get("right"), marginMap.get("bottom"), marginMap.get("left"));
    addCodeLine(codeLine);
    codeLine = format("{0}.setMargin({1}, {2})", containerName, marginWrapper.getParent().getControlName(), controlName);
    addCodeLine(codeLine);
    addLineBreak();
  }

  private void handlePadding(JavaNode paddingWrapper) {
    Map<String, Double> paddingMap = insetMapFromChildren(paddingWrapper);
    String controlName = getControlName("Insets");
    String codeLine = format("Insets {0} = new Insets({1}, {2}, {3}, {4})", controlName,
        paddingMap.get("top"), paddingMap.get("right"), paddingMap.get("bottom"), paddingMap.get("left"));
    addCodeLine(codeLine);
    codeLine = format("{0}.setPadding({1})", paddingWrapper.getParent().getControlName(), controlName);
    addCodeLine(codeLine);
    addLineBreak();
  }

  private void handleInsets(JavaNode inset) {
    Map<String, Double> paddingMap = insetMapFromChildren(inset.getParent());
    String controlName = getControlName("Insets");
    String codeLine = format("Insets {0} = new Insets({1}, {2}, {3}, {4})", controlName,
        paddingMap.get("top"), paddingMap.get("right"), paddingMap.get("bottom"), paddingMap.get("left"));
    addCodeLine(codeLine);
    codeLine = format("{0}.setPadding({1})", inset.getParent().getControlName(), controlName);
    addCodeLine(codeLine);
    addLineBreak();
  }

  private Map<String, Double> insetMapFromChildren(JavaNode insetWrapper) {
    Map<String, Double> insetMap = new HashMap<>();
    insetMap.put("top", 0.0);
    insetMap.put("bottom", 0.0);
    insetMap.put("left", 0.0);
    insetMap.put("right", 0.0);
    for (JavaNode inset : insetWrapper.getChildren()) {
      Set<Entry<String, String>> attrs = inset.getAttributes().entrySet();
      attrs.forEach(entry -> {
        if (insetMap.containsKey(entry.getKey())) {
          insetMap.put(entry.getKey(), Double.parseDouble(entry.getValue()));
        }
      });
    }
    return insetMap;
  }

  private void handleSpinnerValueFactory(JavaNode spinnerValueFactory) {
    throw new UnsupportedOperationException("SpinnerValueFactory not supported");
  }

  private void handleChart(JavaNode barChart) {
    throw new UnsupportedOperationException("Charts not supported");
  }

  private void handleCollections(JavaNode fxCollections) {
    throw new UnsupportedOperationException("FXCollections not supported");
  }

  private void handleImage(JavaNode image) {
    String controlName = image.getControlName();
    String url = image.extractAttribute("url");
    String codeLine = MessageFormat.format("Image {0} = new Image(\"{1}\")", controlName, url);
    addCodeLine(codeLine);
    setupAttributes(image);
  }

  private void handleInclude(JavaNode include) {
    setControlName(include);
    String sourceFile = include.extractAttribute(FX_SOURCE);
    Path sourcePath;
    if (sourceFile.startsWith("/")) {
      throw new UnsupportedOperationException("Need Relative Path");
    } else {
      sourcePath = fxmlProcessor.getFilePath().getParent().resolve(sourceFile);
    }
    FxmlProcessor includeFxmlProcessor = new FxmlProcessor(sourcePath, fxmlProcessor.getResourcePath(), "");
    new ControlFactory(includeFxmlProcessor).buildControl();
    String controllerClassName = includeFxmlProcessor.getFxClassName();
    if (!controlIndexMap.containsKey(controllerClassName)) {
      controlIndexMap.put(controllerClassName, 1);
    }
    String controllerName = format("{0}{1}", StringUtils.camelCase(controllerClassName), String.valueOf(controlIndexMap.get(controllerClassName)));
    controlIndexMap.put(controllerClassName, controlIndexMap.get(controllerClassName) + 1);
    String fxViewObjectArgument = StringUtils.join(includeFxmlProcessor.getFxViewObjectParams().stream().map(fxViewObjectParam -> StringUtils.camelCase(fxViewObjectParam.getSimpleName())));
    addCodeLine(format("{0} {1} = new {2}({3})", includeFxmlProcessor.getFxClassName(), controllerName, includeFxmlProcessor.getFxClassName(), fxViewObjectArgument));
    addCodeLine(format("{0}.initialize()", controllerName));
    addCodeLine(format("{0} {1} = {2}.view", includeFxmlProcessor.getViewType(), include.getControlName(), controllerName));
    fxmlProcessor.getFxViewObjectParams().addAll(includeFxmlProcessor.getFxViewObjectParams());
    fxmlProcessor.getResolver().getFixedTypeNames().putAll(includeFxmlProcessor.getResolver().getFixedTypeNames());
    fxmlProcessor.getResolver().getFixedTypes().putAll(includeFxmlProcessor.getResolver().getFixedTypes());
  }

  private void addCodeLine(String codeLine) {
    if (OsUtils.isNullOrEmpty(codeLine)) {
      return;
    }
    controlLines.add(codeLine);
  }

  private void addLineBreak() {
    controlLines.add("");
  }


  private boolean isNodeProperty(String attrName) {
    return attrName.contains(".");
  }

  private void setupNodeProperty(String attrName, String value, String controlName) {
    Class<?> valueClass = String.class;

    attrName = prepareAttrName(attrName);
    Entry<String, Class<?>> specProp = findSpecProp(attrName);
    if (specProp != null) {
      valueClass = specProp.getValue();
    }

    value = prepareFunctionParam(value, valueClass);

    String codeLine = format("{0}.getProperties().put({1},{2})", controlName, quote(attrName), value);
    addCodeLine(codeLine);
  }

  private String prepareFunctionParam(String attributeValue, Class<?> parameterType) {
    int typeCodeParameter = TypeCode.TypeNameToTypeCode(parameterType);
    switch (typeCodeParameter) {
      case TypeCode.String -> {
        if (attributeValue.startsWith("%")) {

          return format("i18n.get(\"{0}\")", attributeValue.substring(1));
        }
        return "\"" + attributeValue.replace("\"", "") + "\"";
      }
      case TypeCode.Enum -> {
        fxmlProcessor.getResolver().getFixedTypes().put(parameterType.getName(), parameterType);
        return ConvertUtils.computeEnumAttributeName(parameterType, attributeValue);
      }
      case TypeCode.Color -> {
        fxmlProcessor.getResolver().getFixedTypes().put(parameterType.getName(), parameterType);
        return ConvertUtils.computeColorAttributeName(parameterType, attributeValue);
      }
      case TypeCode.Object -> {
        return handleObjectParam(attributeValue, parameterType);
      }
      case TypeCode.Double -> {
        return ConvertUtils.computeDoubleAttributeName(attributeValue);
      }
      default -> {
        return attributeValue;
      }
    }
  }

  private String handleObjectParam(String attributeValue, Class<?> parameterType) {
    switch (parameterType.getSimpleName()) {
      case "EventHandler" -> {
        if (fxmlProcessor.getControllerClass() != null) {
          Method method = fxmlProcessor.getResolver().getMethod(fxmlProcessor.getControllerClass(), attributeValue.substring(1), 1);
          if (method != null) {
            Class<?> methodParameterType = method.getParameterTypes()[0];
            fxmlProcessor.getResolver().getFixedTypes().put(methodParameterType.getName(), methodParameterType);
            return format("event -> controller.{0}(({1}) event)", attributeValue.substring(1), methodParameterType.getSimpleName());
          }
        }
        return "event -> controller." + attributeValue.substring(1) + "()";
      }
      case "Duration" -> {
        fxmlProcessor.getResolver().getFixedTypes().put(parameterType.getName(), parameterType);
        return format("Duration.valueOf(\"{0}\")", attributeValue);
      }
      default -> {
        return attributeValue;
      }
    }
  }

  private String prepareAttrName(String attrName) {
    String localAttrName = attrName;
    localAttrName = localAttrName.replace('.', '-').toLowerCase();
    if (localAttrName.endsWith(PROP_POSTFIX_INT)) {
      localAttrName = localAttrName.substring(0, localAttrName.length() - 5);
    }
    return localAttrName;
  }

  private Entry<String, Class<?>> findSpecProp(String attrName) {
    for (Entry<String, Class<?>> prop : specPropClass.entrySet()) {
      if (attrName.endsWith(prop.getKey())) {
        fxmlProcessor.getResolver().getFixedTypes().put(prop.getValue().getName(), prop.getValue());
        return prop;
      }
    }
    return null;
  }

  public String buildFunctionCode(String attributeValue, String controlName, String methodName, Class<?> parameterType) {
    String parameterValue = prepareFunctionParam(attributeValue, parameterType);

    return controlName + "." + methodName + "(" + parameterValue.replace("$", "") + ")";
  }
}
