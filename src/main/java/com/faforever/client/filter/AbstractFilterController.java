package com.faforever.client.filter;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class AbstractFilterController<T> implements Controller<SplitPane> {


  /**
   * Variables are located at {@code theme/filter/filter.fxml} file
   */
  public SplitPane root;
  public VBox filtersContent;
  public Button resetAllButton;

  protected final UiService uiService;
  protected final I18n i18n;

  private final ObservableMap<ObservableValue<?>, Predicate<T>> externalFilters = FXCollections.observableHashMap();
  private final List<AbstractFilterNodeController<?, ? extends ObservableValue<?>, T>> filters = new ArrayList<>();
  private Predicate<T> defaultPredicate = t -> true;
  private boolean resetInProgress = false;

  private final BooleanProperty filterState = new SimpleBooleanProperty(false);
  private final ObjectProperty<Predicate<T>> predicate = new SimpleObjectProperty<>(defaultPredicate);

  public AbstractFilterController(UiService uiService, I18n i18n) {
    this.i18n = i18n;
    this.uiService = uiService;
  }

  @Override
  public void initialize() {
    build(new FilterBuilder<>(uiService, filters::add));
    afterBuilt();
  }

  protected abstract void build(FilterBuilder<T> filterBuilder);

  protected void afterBuilt() {
    // To be overridden by subclass
  };

  public ObjectProperty<Predicate<T>> predicateProperty() {
    return predicate;
  }

  public Predicate<T> getPredicate() {
    return predicate.get();
  }

  public BooleanProperty filterStateProperty() {
    return filterState;
  }

  public boolean getFilterState() {
    return filterState.get();
  }

  public void setDefaultPredicate(Predicate<T> defaultPredicate) {
    this.defaultPredicate = defaultPredicate;
    predicate.setValue(defaultPredicate);
  }

  private void setFilterContent() {
    filtersContent.getChildren().setAll(filters.stream().map(Controller::getRoot).toList());
  }

  public void completeSetting() {
    setFilterContent();
    filters.forEach(filter -> JavaFxUtil.addListener(filter.predicateProperty(), observable -> invalidate()));
    externalFilters.addListener((InvalidationListener) observable -> invalidate());
    invalidate();
  }

  private synchronized void invalidate() {
    if (!resetInProgress) {
      Predicate<T> finalPredicate = Stream.concat(filters.stream()
              .map(AbstractFilterNodeController::getPredicate), externalFilters.values().stream())
          .reduce(Predicate::and)
          .orElseThrow();
      predicate.setValue(defaultPredicate.and(finalPredicate));
      updateFilterState();
    }
  }

  private void updateFilterState() {
    boolean hasDefaultValues = filters.stream().allMatch(AbstractFilterNodeController::hasDefaultValue);
    filterState.setValue(!hasDefaultValues);
    resetAllButton.setDisable(hasDefaultValues);
  }

  public <U> void bindExternalFilter(ObservableValue<U> property, BiFunction<U, T, Boolean> filter) {
    JavaFxUtil.addListener(property, observable -> externalFilters.put(property, item -> filter.apply(property.getValue(), item)));
  }

  public void onResetAllButtonClicked() {
    resetInProgress = true;
    filters.stream().filter(filter -> !filter.hasDefaultValue()).forEach(AbstractFilterNodeController::resetFilter);
    resetAllButton.setDisable(true);
    resetInProgress = false;
    invalidate();
  }

  @Override
  public SplitPane getRoot() {
    return root;
  }
}
