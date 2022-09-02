package com.faforever.client.filter;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import org.apache.commons.lang3.Range;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapFilterController extends AbstractFilterController<MapVersionBean> {

  protected MapFilterController(UiService uiService, I18n i18n) {
    super(uiService, i18n);
  }

  @Override
  protected void build(FilterBuilder<MapVersionBean> filterBuilder) {
    filterBuilder

        .rangeSlider(FilterName.MAP_WIDTH, i18n.get("game.filter.mapWidth"), 5, 100,
            (pair, mapVersion) -> Range.between(pair.getLeft(), pair.getRight())
                .contains(mapVersion.getSize().getWidthInKm()))

        .rangeSlider(FilterName.MAP_HEIGHT, i18n.get("game.filter.mapHeight"), 5, 100,
            (pair, mapVersion) -> Range.between(pair.getLeft(), pair.getRight())
                .contains(mapVersion.getSize().getHeightInKm()))

        .rangeSlider(FilterName.NUMBER_OF_PLAYERS, i18n.get("game.filter.numberOfPlayers"), 1, 16,
            (pair, mapVersion) -> Range.between(pair.getLeft(), pair.getRight())
                .contains(mapVersion.getMaxPlayers()))

        .build();
  }
}
