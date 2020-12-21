/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.faforever.client.fxml.utils;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ciprian
 */
public class StringUtils {
    public static String substringBeforeLast(String _this, String delimiter) {
        int index = _this.lastIndexOf(delimiter);
        if (index == -1) {
            return _this;
        }
        return _this.substring(0, index);
    }

    public static String substringAfterLast(String _this, String delimiter) {
        int index = _this.lastIndexOf(delimiter);
        if (index == -1) {
            return _this;
        }
        return _this.substring(index + 1);
    }

    public static String removePrefix(String _this, String prefix) {
        if (_this.startsWith(prefix)) {
            return _this.substring(prefix.length());
        }
        return _this;
    }

    public static String removeAfterLastSeparator(String _this, String regexSeparator) {
        String[] tokens = _this.split(regexSeparator);
        return tokens[tokens.length - 1];
    }

    public static String removeSuffix(String _this, String suffix) {
        if (_this.endsWith(suffix)) {
            return _this.substring(0, _this.length() - suffix.length());
        }
        return _this;
    }

    public static String join(Stream<String> items) {

        return join(items.collect(Collectors.toList()));
    }

    public static String join(Collection<String> items) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String item : items) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            sb.append(item);
        }

        return sb.toString();
    }

    public static String capitalize(String text) {
        String startString = text.substring(0, 1).toUpperCase();
        String endString = text.substring(1);
        return startString + endString;
    }

    public static String camelCase(String text) {
        String startString = text.substring(0, 1).toLowerCase();
        String endString = text.substring(1);
        return startString + endString;
    }

    public static String snakeToCapitalize(String text) {
        String[] snakeTextParts = text.split("_");
        StringBuilder stringBuilder = new StringBuilder();
        for (String snakeTextPart : snakeTextParts) {
            stringBuilder.append(StringUtils.capitalize(snakeTextPart));
        }
        return stringBuilder.toString();
    }

    public static String quote(String s) {
        return "\"" + s + "\"";
    }

    public static String fxmlFileToJavaClass(String fileName) {
        File fileData = new File(fileName);
        String className = StringUtils.substringBeforeLast(fileData.getName(), ".");
        return "Fx" + StringUtils.snakeToCapitalize(className);
    }
}
