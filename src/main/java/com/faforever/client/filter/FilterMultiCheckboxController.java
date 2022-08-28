package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.CheckListView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class FilterMultiCheckboxController<U, T> extends AbstractFilterNodeController<List<U>, T> {

  private final I18n i18n;

  public MenuButton root;
  public CheckListView<String> listView;

  private final ObservableList<U> selectedItems = FXCollections.observableArrayList();

  private StringConverter<U> converter;
  private List<U> sourceList;
  private String defaultText;

  @Override
  public boolean hasDefaultValue() {
    return selectedItems.isEmpty();
  }

  @Override
  public void resetFilter() {
    listView.getCheckModel().clearChecks();
  }

  @Override
  public Observable getObservable() {
    return selectedItems;
  }

  @Override
  public List<U> getValue() {
    return selectedItems;
  }

  @Override
  public Node getRoot() {
    return root;
  }

  public void setText(String text) {
    this.defaultText = text;
    root.setText(text);
  }

  public void setItems(List<U> items) {
    this.sourceList = items;
    listView.setItems(FXCollections.observableList(items.stream().map(item -> converter.toString(item)).toList()));

    JavaFxUtil.addListener(listView.getCheckModel().getCheckedIndices(),
        (InvalidationListener) observable -> invalidated(listView.getCheckModel().getCheckedIndices()));
    JavaFxUtil.bind(root.textProperty(), Bindings.createStringBinding(() -> i18n.get("filter.category", defaultText,
        String.join(", ", listView.getCheckModel().getCheckedItems())), listView.getCheckModel().getCheckedItems()));
  }

  private void invalidated(List<Integer> selectedIndices) {
    selectedItems.setAll(selectedIndices.stream().map(i -> sourceList.get(i)).toList());
  }

  public void setConverter(StringConverter<U> converter) {
    this.converter = converter;
  }
}
