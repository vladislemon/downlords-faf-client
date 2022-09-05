package com.faforever.client.filter;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RangeSliderWithChoiceFilterController<I, T> extends AbstractRangeSliderFilterController<ImmutablePair<I, ImmutablePair<Integer, Integer>>, T> {

  public ComboBox<I> choiceView = new ComboBox<>();
  private StringConverter<I> converter;

  protected RangeSliderWithChoiceFilterController(I18n i18n) {
    super(i18n);
  }

  @Override
  public void initialize() {
    super.initialize();
    JavaFxUtil.bind(choiceView.prefWidthProperty(), rangeSlider.widthProperty());
  }

  public void setText(String text) {
    this.defaultText = text;
    JavaFxUtil.bind(root.textProperty(), Bindings.createStringBinding(this::getFormattedText, rangeSlider.lowValueProperty(), rangeSlider.highValueProperty(), choiceView.getSelectionModel()
        .selectedItemProperty()));
  }

  private String getFormattedText() {
    if (converter != null && choiceView.getSelectionModel().getSelectedItem() != null) {
      defaultText = converter.toString(choiceView.getSelectionModel().getSelectedItem());
    }
    return i18n.get("filter.range",
        defaultText,
        hasLowDefaultValue() ? "" : ((int) rangeSlider.getLowValue()),
        hasHighDefaultValue() ? "" : ((int) rangeSlider.getHighValue())
    );
  }

  public void setItems(List<I> items) {
    choiceView.setItems(FXCollections.observableList(items));
    choiceView.getSelectionModel().selectFirst();
    vBoxContent.getChildren().add(0, choiceView);
  }

  @Override
  public Observable getObservable() {
    if (rangeProperty == null) {
      rangeProperty = Bindings.createObjectBinding(() -> {
            int lowValue = (int) rangeSlider.getLowValue();
            int highValue = (int) rangeSlider.getHighValue();
            I item = choiceView.getSelectionModel().getSelectedItem();
            return lowValue == minValue && highValue == maxValue
                ? ImmutablePair.of(item, NO_CHANGE)
                : ImmutablePair.of(item, ImmutablePair.of(lowValue, highValue));
          },
          rangeSlider.lowValueProperty(), rangeSlider.highValueProperty(), choiceView.getSelectionModel()
              .selectedItemProperty());
    }
    return rangeProperty;
  }

  @Override
  public ImmutablePair<I, ImmutablePair<Integer, Integer>> getValue() {
    return rangeProperty.getValue();
  }

  public void setConverter(StringConverter<I> converter) {
    this.converter = converter;
    choiceView.setConverter(converter);
  }
}
