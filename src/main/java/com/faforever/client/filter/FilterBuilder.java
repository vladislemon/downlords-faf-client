package com.faforever.client.filter;

import com.faforever.client.theme.UiService;
import javafx.util.Pair;
import javafx.util.StringConverter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.MODULE)
public class FilterBuilder<T> {

  private final UiService uiService;
  private final Consumer<List<AbstractFilterNodeController<?, T>>> onFilterBuilt;
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

  public <U> FilterBuilder<T> multiCheckbox(FilterName filterName, String text, List<U> items, StringConverter<U> converter, BiFunction<List<U>, T, Boolean> filter) {
    return multiCheckbox(filterName, text, CompletableFuture.completedFuture(items), converter, filter);
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

  public FilterBuilder<T> rangeSlider(FilterName filterName, String text, double minValue, double maxValue, BiFunction<Pair<Integer, Integer>, T, Boolean> filter) {
    RangeSliderFilterController<T> controller = uiService.loadFxml("theme/filter/range_slider_filter.fxml");
    controller.setFilterName(filterName);
    controller.setText(text);
    controller.setMinValue(minValue);
    controller.setMaxValue(maxValue);
    controller.registerListener(filter);
    controllers.add(controller);
    return this;
  }

  public void build() {
    onFilterBuilt.accept(controllers);
  }
}
