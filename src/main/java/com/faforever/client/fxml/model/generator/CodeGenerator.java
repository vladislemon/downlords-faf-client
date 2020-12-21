package com.faforever.client.fxml.model.generator;

import com.faforever.client.fxml.utils.OsUtils;
import com.faforever.client.fxml.utils.StringUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class CodeGenerator {

    public List<String> staticImports = new ArrayList<>();
    public List<String> starImports = new ArrayList<>();
    public String packageName = "";
    public String className = "";
    public String controllerType = "";
    public Constructor<?> controllerConstructor = null;
    public List<Class<?>> controllerParams = new ArrayList<>();
    public List<Class<?>> fxObjectParams = new ArrayList<>();

    public String viewType = "";

    public List<String> buildControlsLines = new ArrayList<>();
    StringBuilder stringBuilder = new StringBuilder();

    void appendln(String text) {
        stringBuilder.append(text);
        stringBuilder.append('\r');
        stringBuilder.append('\n');
    }

    void append(String text) {
        stringBuilder.append(text);
    }

    public String generateCode() {

        if (!OsUtils.isNullOrEmpty(packageName)) {
            append("package ");
            append(packageName);
            appendln(";");
            appendln();
        }
        appendln("import com.faforever.client.fxml.FxObject;");
        for (String imprt : staticImports) {
            append("import ");
            append(imprt.replace("$", "."));
            appendln(";");
        }
        appendln();
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

        appendln();

        append("public final class ");
        append(className);
        append(" implements FxObject<");
        append(controllerType);
        append(">");
        appendln(" {");

        appendln();

        if (!OsUtils.isNullOrEmpty(controllerType)) {
            append("\tpublic ");
            append(controllerType);
            appendln(" controller;");
        }

        append("\tpublic ");
        append(viewType);
        appendln(" view;");

        appendln();

        append("\tpublic ");
        append(className);
        append("(");
        for (Class<?> fxObjectParam : fxObjectParams) {
            append(fxObjectParam.getSimpleName());
            append(" ");
            append(StringUtils.camelCase(fxObjectParam.getSimpleName()));
            if (fxObjectParams.indexOf(fxObjectParam) != fxObjectParams.size() - 1) {
                append(", ");
            }
        }
        appendln(") {");

        if (!OsUtils.isNullOrEmpty(controllerType)) {
            append("\t\tcontroller = new ");
            append(controllerType);
            append("(");
            for (Class<?> controllerParam : controllerParams) {
                append(StringUtils.camelCase(controllerParam.getSimpleName()));
                if (controllerParams.indexOf(controllerParam) != controllerParams.size() - 1) {
                    append(", ");
                }
            }
            appendln(");");
        }

        for (String line : this.buildControlsLines) {
            append("\t\t");
            append(line);
            if (!line.isBlank()) {
                appendln(";");
            } else {
                appendln();
            }
        }
        appendln("\t}");

        appendln("");

        append("public ");
        append(controllerType);
        appendln(" getController() {");
        appendln("\t\treturn controller;");
        appendln("\t}");

        appendln("}");

        return stringBuilder.toString();
    }

    private void appendln() {
        appendln("");
    }

}
