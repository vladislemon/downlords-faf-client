package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RangeSliderFilterController<T> extends AbstractRangeSliderFilterController<ImmutablePair<Integer, Integer>, T> {

  protected RangeSliderFilterController(I18n i18n) {
    super(i18n);
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
    if (rangeProperty == null) {
      rangeProperty = Bindings.createObjectBinding(() -> {
            int lowValue = (int) rangeSlider.getLowValue();
            int highValue = (int) rangeSlider.getHighValue();
            return lowValue == minValue && highValue == maxValue ? NO_CHANGE : ImmutablePair.of(lowValue, highValue);
          },
          rangeSlider.lowValueProperty(), rangeSlider.highValueProperty());
    }
    return rangeProperty;
  }

  @Override
  public ImmutablePair<Integer, Integer> getValue() {
    return rangeProperty.getValue();
  }
}
