package com.faforever.client.filter;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FilterCheckboxController<T> extends AbstractFilterNodeController<Boolean, T> {

  public CheckBox root;


  @Override
  public void initialize() {

  }

  public void setText(String text) {
    root.setText(text);
  }

  @Override
  public boolean hasDefaultValue() {
    return root.isSelected() == getDefaultValue();
  }

  @Override
  public void resetFilter() {
    if (getDefaultValue() != null) {
      root.setSelected(getDefaultValue());
    }
  }

  @Override
  public BooleanProperty getObservable() {
    return root.selectedProperty();
  }

  @Override
  public Boolean getValue() {
    return root.isSelected();
  }

  @Override
  public CheckBox getRoot() {
    return root;
  }
}
