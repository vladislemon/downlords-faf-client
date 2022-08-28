package com.faforever.client.filter;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import lombok.Getter;
import lombok.Setter;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public abstract class AbstractFilterNodeController<U, T> implements Controller<Node> {

  @Getter
  @Setter
  private FilterName filterName;
  @Getter
  @Setter
  private U defaultValue;

  private final ObjectProperty<Predicate<T>> predicate = new SimpleObjectProperty<>(item -> true);

  public abstract boolean hasDefaultValue();

  public abstract void resetFilter();

  public abstract Observable getObservable();

  public abstract U getValue();

  public void registerListener(BiFunction<U, T, Boolean> filter) {
    JavaFxUtil.addAndTriggerListener(getObservable(), observable -> predicate.set(object -> filter.apply(getValue(), object)));
  }

  public final ObjectProperty<Predicate<T>> getPredicateProperty() {
    return predicate;
  }

  public final Predicate<T> getPredicate() {
    return predicate.get();
  }
}
