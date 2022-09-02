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
import java.util.List;


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

  private final StringConverter<GameType> gameTypeConverter = new StringConverter<>() {
    @Override
    public String toString(GameType object) {
      return i18n.get(switch (object) {
        case CUSTOM -> "customGame";
        case MATCHMAKER -> "matchmaker";
        case COOP -> "coopGame";
        default -> throw new IllegalArgumentException(object + " should not be used");
      });
    }

    @Override
    public GameType fromString(String string) {
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

        .multiCheckbox(FilterName.GAME_TYPE, i18n.get("gameType"), List.of(GameType.CUSTOM, GameType.MATCHMAKER, GameType.COOP), gameTypeConverter,
            (selectedGameTypes, game) -> selectedGameTypes.isEmpty() || selectedGameTypes.contains(game.getGameType()))

        .checkbox(FilterName.SIM_MODS, i18n.get("hideSimMods"), (selected, game) -> !selected || game.getSimMods()
            .isEmpty())

        .checkbox(FilterName.PRIVATE_GAME, i18n.get("privateGame"), (selected, game) -> selected || !game.isPasswordProtected())

        .textField(FilterName.PLAYER_NAME, i18n.get("game.player.username"), (text, game) -> text.isEmpty() || game.getTeams()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .anyMatch(name -> StringUtils.containsIgnoreCase(name, text)))

        .multiCheckbox(FilterName.FEATURE_MOD, i18n.get("featuredMod.displayName"), modService.getFeaturedMods(),
            featuredModConverter, (selectedMods, game) -> selectedMods.isEmpty() || selectedMods.stream()
                .anyMatch(mod -> mod.getTechnicalName().equals(game.getFeaturedMod())))

        .mutableList(FilterName.MAP_FOLDER_NAME_BLACKLIST, i18n.get("blacklist.mapFolderName"), i18n.get("blacklist.mapFolderName.promptText"),
            (folderNames, game) -> folderNames.isEmpty() || folderNames.stream()
                .noneMatch(name -> game.getMapFolderName().contains(name)))

        .checkbox(FilterName.ONE_PLAYER, i18n.get("hideSingleGames"), (selected, game) -> !selected || game.getNumPlayers() != 1)

        .build();
  }
}
