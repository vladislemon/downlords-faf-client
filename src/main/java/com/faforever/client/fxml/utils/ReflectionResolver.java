package com.faforever.client.fxml.utils;

import com.faforever.client.fxml.infrastructure.JavaNode;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.text.MessageFormat.format;

@Getter
public class ReflectionResolver {

  private final List<String> imports = new ArrayList<>();

  private final Map<String, Class<?>> fixedTypeNames = new HashMap<>();
  private final Map<String, Class<?>> fixedTypes = new HashMap<>();

  public ReflectionResolver(Set<String> imports) {
    for (String imprt : imports) {
      if (imprt.endsWith(".*")) {
        String importSubtracted = imprt.substring(0, imprt.length() - 2);
        this.imports.add(importSubtracted);
      } else {
        Class<?> clazz = null;
        try {
          clazz = Class.forName(imprt);
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
        fixedTypes.put(imprt, clazz);
        String fixedTypeName = StringUtils.substringAfterLast(imprt, ".");
        fixedTypeNames.put(fixedTypeName, clazz);
      }
    }

  }

  public boolean hasDefaultConstructor(JavaNode javaNode) {
    Class<?> clazz = javaNode.getClazz();
    Constructor<?>[] allConstructors = clazz.getDeclaredConstructors();
    for (Constructor<?> ctor : allConstructors) {
      if (ctor.getParameterCount() == 0) {
        String modifierString = Modifier.toString(ctor.getModifiers());
        if (modifierString.contains("public")) {
          return true;
        }
      }
    }
    return false;
  }

  public Constructor<?> firstPublicConstructor(Class<?> clazz) {
    Constructor<?>[] allConstructors = clazz.getDeclaredConstructors();
    for (Constructor<?> constructor : allConstructors) {
      String modifierString = Modifier.toString(constructor.getModifiers());
      if (modifierString.contains("public")) {
        return constructor;
      }
    }
    return null;
  }

  public void resolve(JavaNode javaNode) {
    String typeName = javaNode.getName();
    if (fixedTypes.containsKey(typeName)) {
      javaNode.setClazz(fixedTypes.get(typeName));
      return;
    }
    if (fixedTypeNames.containsKey(typeName)) {
      javaNode.setClazz(fixedTypeNames.get(typeName));
      return;
    }


    for (String imprt : imports) {
      String baseName = imprt + "." + typeName.replace(".", "$");
      try {
        javaNode.setClazz(ClassLoader.getSystemClassLoader().loadClass(baseName));
        return;
      } catch (Exception ignored) {
      }
    }
    javaNode.setClazz(null);
  }

  public Class<?> resolve(String typeName) {
    if (fixedTypes.containsKey(typeName)) {
      return fixedTypes.get(typeName);
    }
    if (fixedTypeNames.containsKey(typeName)) {
      return fixedTypeNames.get(typeName);
    }

    try {
      return ClassLoader.getSystemClassLoader().loadClass(typeName);
    } catch (ClassNotFoundException ignored) {
    }

    for (String imprt : imports) {
      String baseName = imprt + "." + typeName.replace(".", "$");
      try {
        return ClassLoader.getSystemClassLoader().loadClass(baseName);
      } catch (ClassNotFoundException ignored) {
      }
    }

    return null;
  }

  public Method getMethod(Class<?> clz, String name, int paramCount) {
    Method[] methods = clz.getMethods();
    for (Method method : methods) {

      String methodName = method.getName();
      if (!methodName.equals(name)) {
        continue;
      }

      int methodparameterCount = method.getParameterCount();
      if ((paramCount != -1) && (methodparameterCount == paramCount)) {
        return method;

      }
    }
    return null;
  }

  public Method resolvePropertyMethod(Class<?> clz, String name, boolean isSetter) {
    Method mth;
    String capitalizedName = StringUtils.capitalize(name);
    if (isSetter) {
      mth = getMethod(clz, format("set{0}", capitalizedName), 1);
    } else {
      mth = getMethod(clz, format("get{0}", capitalizedName), 0);
    }

    return mth;
  }

  public boolean hasProperty(Class<?> clz, String name) {
    Field[] fields = clz.getFields();
    for (Field field : fields) {
      if (field.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  public Method resolveClassStaticSetter(Class<?> clz, String name) {

    String indentedMethodName = StringUtils.capitalize(name);
    return getMethod(clz, format("set{0}", indentedMethodName), 2);
  }
}
