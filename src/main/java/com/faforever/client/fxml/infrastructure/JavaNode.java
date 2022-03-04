package com.faforever.client.fxml.infrastructure;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class JavaNode {

    private final List<JavaNode> children = new ArrayList<>();
    private final Map<String, String> attributes = new HashMap<>();

    private final JavaNode parent;
    @ToString.Include
    private final String name;
    private String controlName;
    private Class<?> clazz;
    private String innerText;

    public JavaNode(String name, JavaNode parent) {
        this.name = name;
        this.parent = parent;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String extractAttribute(String attrName) {
        if (attributes.containsKey(attrName)) {
            String result = attributes.get(attrName);
            attributes.remove(attrName);
            return result;
        }
        return "";
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Boolean hasChildren() {
        return !children.isEmpty();
    }

    public List<JavaNode> getChildren() {
        return children;
    }
}
