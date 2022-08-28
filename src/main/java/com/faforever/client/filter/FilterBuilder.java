package com.faforever.client.filter;

import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.theme.UiService;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.textfield.TextFields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

@RequiredArgsConstructor(access = AccessLevel.MODULE)
public class FilterBuilder<T> {

  private final UiService uiService;
  private final Consumer<List<AbstractFilterNodeController<?, T>>> onFilterBuilt;

  private final ObservableMap<FilterName, Predicate<T>> filterNameToPredicate = FXCollections.observableHashMap();

  private final List<AbstractFilterNodeController<?, T>> controllers = new ArrayList<>();

  public FilterBuilder<T> checkbox(FilterName filterName, String text, BiFunction<Boolean, T, Boolean> filter) {
    FilterCheckboxController<T> controller = uiService.loadFxml("theme/filter/checkbox_filter.fxml");
    controller.setFilterName(filterName);
    controller.setText(text);
    controller.setDefaultValue(controller.getValue());
    controller.registerListener(filter);
    controllers.add(controller);
    return this;
  }

  public FilterBuilder<T> textField(FilterName filterName, String promptText, BiFunction<String, T, Boolean> filter) {
    FilterTextFieldController<T> controller = uiService.loadFxml("theme/filter/textfield_filter.fxml");
    controller.setFilterName(filterName);
    controller.setPromptText(promptText);
    controller.setDefaultValue(controller.getValue());
    controller.registerListener(filter);
    controllers.add(controller);
    return this;
  }

  public <U> FilterBuilder<T> multiCheckbox(FilterName filterName, String text, CompletableFuture<List<U>> items, StringConverter<U> converter, BiFunction<List<U>, T, Boolean> filter) {
    FilterMultiCheckboxController<U, T> controller = uiService.loadFxml("theme/filter/multicheckbox_filter.fxml");
    controller.setFilterName(filterName);
    controller.setText(text);
    controller.setConverter(converter);
    items.thenAccept(items1 -> {
      controller.setItems(items1);
      controller.registerListener(filter);
    });
    controllers.add(controller);
    return this;
  }

  public void build() {
    onFilterBuilt.accept(controllers);
  }
}
