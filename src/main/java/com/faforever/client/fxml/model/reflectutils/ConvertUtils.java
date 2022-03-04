package com.faforever.client.fxml.model.reflectutils;

import com.faforever.client.fxml.infrastructure.JavaNode;
import com.faforever.client.fxml.model.MathUtils;
import com.faforever.client.fxml.utils.ReflectionResolver;
import com.faforever.client.fxml.utils.StringUtils;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ciprian on 6/24/2016.
 */
public class ConvertUtils {
    public static String computeColorAttributeName(Class<?> parameterType, String attributeValue) {
        if (attributeValue.startsWith("0x")) {
            return "Color.web(\"" + attributeValue + "\")";
        }
        return ConvertUtils.computeEnumAttributeName(parameterType, attributeValue);
    }


    public static String handleSettingInnerText(ReflectionResolver resolver, Class<?> parentClass, JavaNode child, String innerText, String parentControl) {
        Method method = resolver.resolvePropertyMethod(parentClass, child.getName(), false);
        Class<?> returnType = method.getReturnType();
        Method methodAddAll = resolver.getMethod(returnType, "addAll", 1);
        Class<?> collectionType = methodAddAll.getParameterTypes()[0];
        Method methodGetInCollection = resolver.getMethod(collectionType, "get", 1);
        String returnNameType = methodGetInCollection.getReturnType().getSimpleName();

        String arrayData = "";
        switch (returnNameType) {
            case "float" -> arrayData = MathUtils.parseFloatToCombinedString(innerText);
            case "int" -> arrayData = MathUtils.parseIntToCombinedString(innerText);
            case "double" -> arrayData = MathUtils.parseDoubleToCombinedString(innerText);
            default -> System.out.println("Never handled");
        }

        return MessageFormat.format("{0}.get{1}().setAll({2})",
            parentControl,
            StringUtils.capitalize(child.getName()),
            arrayData);
    }

    public static String computeDoubleAttributeName(String attributeValue) {
        String parameterValue;
        if (attributeValue.equalsIgnoreCase("Infinity")) {
            parameterValue = "Double.POSITIVE_INFINITY";
        } else if (attributeValue.equalsIgnoreCase("-Infinity")) {
            parameterValue = "Double.NEGATIVE_INFINITY";
        } else {
            parameterValue = attributeValue;
        }
        return parameterValue;
    }

    public static String computeEnumAttributeName(Class<?> parameterType, String attributeValue) {
        Object[] constants = parameterType.getEnumConstants();
        Map<String, String> enumNames = new HashMap<>();
        for (Object constant : constants) {
            String constText = constant.toString();
            enumNames.put(constText.toLowerCase(), constText);
        }
        String attributeNameLowered = attributeValue.toLowerCase();
        String foundMappedName = enumNames.getOrDefault(attributeNameLowered, "");

        String parameterTypeName = parameterType.toString();
        String remainingName = StringUtils.removeAfterLastSeparator(parameterTypeName, "\\.");
        String remainingNameCorrected = remainingName.replace('$', '.');

        return remainingNameCorrected + "." + foundMappedName;
    }

    public static String addCodeValues(JavaNode javaNode) {
        String controlName = javaNode.getControlName();
        String[] controlNameParts = controlName.split("\\.");
        controlName = controlNameParts[controlNameParts.length - 1];
        String name = javaNode.getName();
        String[] nameParts = name.split("\\.");
        name = nameParts[nameParts.length - 1];
        List<String> parameters = new ArrayList<>();
        for (Map.Entry<String, String> entry : javaNode.getAttributes().entrySet()) {
            String value = entry.getValue();
            parameters.add(value);
        }
        String paramLines = StringUtils.join(parameters);
        return MessageFormat.format("{0} {1} = new {2}({3})",
            name,
            controlName,
            name,
            paramLines);
    }
}
