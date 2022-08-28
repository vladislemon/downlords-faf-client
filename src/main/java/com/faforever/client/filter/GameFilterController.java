package com.faforever.client.filter;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameType;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class GameFilterController extends AbstractFilterController<GameBean> {

  private final ModService modService;

  private final StringConverter<FeaturedModBean> featuredModConverter = new StringConverter<>() {
    @Override
    public String toString(FeaturedModBean object) {
      return object.getDisplayName();
    }

    @Override
    public FeaturedModBean fromString(String string) {
      throw new UnsupportedOperationException("Not supported");
    }
  };

  public GameFilterController(UiService uiService, I18n i18n, ModService modService) {
    super(uiService, i18n);
    this.modService = modService;
  }

  @Override
  protected void build(FilterBuilder<GameBean> filterBuilder) {
    filterBuilder

        .checkbox(FilterName.CUSTOM_GAME, i18n.get("customGames"), (selected, game) -> selected || game.getGameType() != GameType.CUSTOM)

        .checkbox(FilterName.COOP_GAME, i18n.get("coopGames"), (selected, game) -> selected || game.getGameType() != GameType.COOP)

        .checkbox(FilterName.MATCHMAKER, i18n.get("matchmaker"), (selected, game) -> selected || game.getGameType() != GameType.MATCHMAKER)

        .checkbox(FilterName.WITH_MODS, i18n.get("withMods"), (selected, game) -> selected || game.getSimMods().isEmpty())

        .checkbox(FilterName.PRIVATE_GAME, i18n.get("privateGames"), (selected, game) -> selected || !game.isPasswordProtected())

        .textField(FilterName.PLAYER_NAME, i18n.get("game.player.username"), (text, game) -> text.isEmpty() || game.getTeams()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .anyMatch(name -> StringUtils.containsIgnoreCase(name, text)))

        .multiCheckbox(FilterName.FEATURE_MOD, i18n.get("featuredMod.displayName"), modService.getFeaturedMods(),
            featuredModConverter, (selectedMods, game) -> selectedMods.isEmpty() || selectedMods.stream()
                .anyMatch(mod -> mod.getTechnicalName().equals(game.getFeaturedMod())))

        .build();
  }
}
