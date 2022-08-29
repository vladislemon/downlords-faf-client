package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.RangeSlider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RangeSliderFilterController<T> extends AbstractFilterNodeController<Pair<Integer, Integer>, T> {

  private final I18n i18n;

  public MenuButton root;
  public RangeSlider rangeSlider;
  public TextField lowValueTextField;
  public TextField highValueTextField;

  private ObjectBinding<Pair<Integer, Integer>> pairProperty;
  private String defaultText;

  private double minValue;
  private double maxValue;

  @Override
  public void initialize() {
    rangeSlider.setShowTickLabels(true);
    rangeSlider.setShowTickMarks(true);
  }

  public void setMinValue(double minValue) {
    this.minValue = minValue;
    rangeSlider.setMin(minValue);
    rangeSlider.setLowValue(minValue);
    JavaFxUtil.bindTextFieldAndRangeSlide(lowValueTextField, rangeSlider, false);
  }

  public void setMaxValue(double maxValue) {
    this.maxValue = maxValue;
    rangeSlider.setMax(maxValue);
    rangeSlider.setHighValue(maxValue);
    JavaFxUtil.bindTextFieldAndRangeSlide(highValueTextField, rangeSlider, true);
  }

  @Override
  public boolean hasDefaultValue() {
    return hasLowDefaultValue() && hasHighDefaultValue();
  }

  private boolean hasLowDefaultValue() {
    return rangeSlider.getLowValue() == minValue;
  }

  private boolean hasHighDefaultValue() {
    return rangeSlider.getHighValue() == maxValue;
  }

  @Override
  public void resetFilter() {
    rangeSlider.setLowValue(minValue);
    rangeSlider.setHighValue(maxValue);
  }

  public void setText(String text) {
    this.defaultText = text;
    JavaFxUtil.bind(root.textProperty(), Bindings.createStringBinding(this::getFormattedText, rangeSlider.lowValueProperty(), rangeSlider.highValueProperty()));
  }

  private String getFormattedText() {
    return i18n.get("filter.range",
        defaultText,
        hasLowDefaultValue() ? "" : ((int) rangeSlider.getLowValue()),
        hasHighDefaultValue() ? "" : ((int) rangeSlider.getHighValue())
    );
  }

  @Override
  public Observable getObservable() {
    if (pairProperty == null) {
      pairProperty = Bindings.createObjectBinding(() -> new Pair<>(((int) rangeSlider.getLowValue()), ((int) rangeSlider.getHighValue())),
          rangeSlider.lowValueProperty(), rangeSlider.highValueProperty());
    }
    return pairProperty;
  }

  @Override
  public Pair<Integer, Integer> getValue() {
    return pairProperty.getValue();
  }

  @Override
  public Node getRoot() {
    return root;
  }
}
