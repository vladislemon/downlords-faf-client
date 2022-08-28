package com.faforever.client.filter;

import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class AbstractFilterController<T> implements Controller<SplitPane> {


  /**
   * Takes from {@code theme/filter/filter.fxml} file
   */
  public SplitPane root;
  public VBox primaryFiltersContent;
  public VBox secondaryFiltersContent;
  public Button filterModeButton;
  public Button resetAllButton;
  public Region filterModeIcon;

  protected final UiService uiService;
  protected final I18n i18n;

  private final List<FilterName> primaryFilterNames = new ArrayList<>();
  private final List<FilterName> secondaryFilterNames = new ArrayList<>();
  private List<? extends AbstractFilterNodeController<?, T>> usedFilters;
  private ObservableMap<FilterName, Predicate<T>> customFilters = FXCollections.observableHashMap();
  private Predicate<T> defaultPredicate = t -> true;

  private final BooleanProperty filterStateProperty = new SimpleBooleanProperty(false);
  private final ObjectProperty<Predicate<T>> predicateProperty = new SimpleObjectProperty<>(defaultPredicate);
  private final FilterBuilder<T> filterBuilder;

  protected AbstractFilterController(UiService uiService, I18n i18n) {
    this.i18n = i18n;
    this.uiService = uiService;

    filterBuilder = new FilterBuilder<>(uiService, this::onFilterBuilt);
  }

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(filterModeButton, secondaryFiltersContent);
    JavaFxUtil.addAndTriggerListener(secondaryFiltersContent.getChildren(),
        (InvalidationListener) observable -> filterModeButton.setVisible(!secondaryFiltersContent.getChildren()
            .isEmpty()));
  }

  protected abstract void build(FilterBuilder<T> filterBuilder);

  public final BooleanProperty getFilterStateProperty() {
    return filterStateProperty;
  }

  public final ObjectProperty<Predicate<T>> getPredicateProperty() {
    return predicateProperty;
  }

  public final boolean getFilterState() {
    return filterStateProperty.get();
  }

  public final void setDefaultPredicate(Predicate<T> defaultPredicate) {
    this.defaultPredicate = defaultPredicate;
    predicateProperty.setValue(defaultPredicate);
  }

  public final void setPrimaryFilters(FilterName... filterNames) {
    primaryFilterNames.addAll(Arrays.stream(filterNames).toList());
  }

  public final void setSecondaryFilters(FilterName... filterNames) {
    secondaryFilterNames.addAll(Arrays.stream(filterNames).toList());
  }

  private void onFilterBuilt(List<AbstractFilterNodeController<?, T>> controllers) {
    List<? extends AbstractFilterNodeController<?, T>> primaryFilters = getUsedControllers(primaryFilterNames, controllers);
    List<? extends AbstractFilterNodeController<?, T>> secondaryFilters = getUsedControllers(secondaryFilterNames, controllers);

    setFilterContent(primaryFiltersContent, primaryFilters);
    setFilterContent(secondaryFiltersContent, secondaryFilters);

    usedFilters = Stream.concat(primaryFilters.stream(), secondaryFilters.stream()).toList();
    usedFilters.forEach(filter -> JavaFxUtil.addListener(filter.getPredicateProperty(), observable -> invalidated()));
    customFilters.addListener((InvalidationListener) observable -> invalidated());
    invalidated();
  }

  private List<? extends AbstractFilterNodeController<?, T>> getUsedControllers(List<FilterName> filterNames, List<AbstractFilterNodeController<?, T>> controllers) {
    return filterNames.stream()
        .map(filterName -> controllers.stream()
            .filter(controller -> controller.getFilterName() == filterName)
            .findFirst()
            .orElseThrow(() -> new ProgrammingError(String.format("No filter name '%s' in available controllers: %s", filterName, filterNames))))
        .toList();
  }

  private void setFilterContent(VBox content, List<? extends AbstractFilterNodeController<?, T>> controllers) {
    content.getChildren().setAll(controllers.stream().map(Controller::getRoot).toList());
  }

  public final void completeSetting() {
    build(filterBuilder);
  }

  private synchronized void invalidated() {
    MutableObject<Predicate<T>> object = new MutableObject<>(defaultPredicate);
    usedFilters.forEach(filter -> object.setValue(object.getValue().and(filter.getPredicate())));
    customFilters.values().forEach(filter -> object.setValue(object.getValue().and(filter)));
    predicateProperty.setValue(object.getValue());
    updateFilterState();
  }

  private void updateFilterState() {
    boolean hasDefaultValues = usedFilters.stream().allMatch(AbstractFilterNodeController::hasDefaultValue);
    filterStateProperty.setValue(!hasDefaultValues);
    resetAllButton.setDisable(hasDefaultValues);
  }

  public <U> void addCustomFilter(FilterName filterName, Property<U> property, BiFunction<U, T, Boolean> filter) {
    JavaFxUtil.addListener(property, observable -> customFilters.put(filterName, item -> filter.apply(property.getValue(), item)));
  }

  public void onResetAllButtonClicked() {
    usedFilters.forEach(AbstractFilterNodeController::resetFilter);
    resetAllButton.setDisable(true);
  }

  public void onFilterModeButtonClicked() {
    secondaryFiltersContent.setVisible(!secondaryFiltersContent.isVisible());
    boolean visible = secondaryFiltersContent.isVisible();
    ObservableList<String> styleClass = filterModeIcon.getStyleClass();
    styleClass.remove(visible ? "circle-down-icon" : "circle-up-icon");
    styleClass.add(visible ? "circle-up-icon" : "circle-down-icon");
  }

  @Override
  public SplitPane getRoot() {
    return root;
  }
}
