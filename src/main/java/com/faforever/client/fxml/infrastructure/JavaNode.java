package com.faforever.client.fxml.infrastructure;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class JavaNode {

    private final List<JavaNode> children = new ArrayList<>();
    private final JavaNode parent;
    private final String name;
    public Map<String, String> Attributes = new HashMap<>();
    private String controlName;
    private Class<?> clazz;
    private String innerText;

    public JavaNode(String name, JavaNode parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getInnerText() {
        return innerText;
    }

    public void setInnerText(String innerText) {
        this.innerText = innerText;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String extractAttribute(String attrName) {
        if (Attributes.containsKey(attrName)) {
            String result = Attributes.get(attrName);
            Attributes.remove(attrName);
            return result;
        }
        return "";
    }

    public Map<String, String> getAttributes() {
        return Attributes;
    }

    public Boolean hasChildren() {
        return !children.isEmpty();
    }

    public List<JavaNode> getChildren() {
        return children;
    }
}
