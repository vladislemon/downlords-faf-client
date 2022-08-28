package com.faforever.client.filter;

import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FilterTextFieldController<T> extends AbstractFilterNodeController<String, T> {

  public TextField root;

  @Override
  public boolean hasDefaultValue() {
    return root.getText().equals(getDefaultValue());
  }

  @Override
  public void resetFilter() {
    if (getDefaultValue() != null) {
      root.setText(getDefaultValue());
    }
  }

  @Override
  public Property<String> getObservable() {
    return root.textProperty();
  }

  @Override
  public String getValue() {
    return root.getText();
  }

  @Override
  public Node getRoot() {
    return root;
  }

  public void setPromptText(String promptText) {
    root.setPromptText(promptText);
  }
}
