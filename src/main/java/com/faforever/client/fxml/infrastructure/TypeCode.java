package com.faforever.client.fxml.infrastructure;

public final class TypeCode {

    public final static int Byte = 0;
    public final static int Char = 1;
    public final static int Int = 2;
    public final static int String = 3;
    public final static int Double = 4;
    public final static int Float = 5;
    public final static int Enum = 6;
    public static final int Color = 7;
    public final static int Object = 8;

    public static int TypeNameToTypeCode(Class<?> type) {
        if (type.isEnum()) {
            return Enum;
        }

        String typeName = type.getTypeName();
        return switch (typeName) {
            case "byte" -> Byte;
            case "char" -> Char;
            case "int" -> Int;
            case "double" -> Double;
            case "float" -> Float;
            case "javafx.scene.paint.Color" -> Color;
            case "java.lang.String" -> String;
            default -> Object;
        };
    }
}
