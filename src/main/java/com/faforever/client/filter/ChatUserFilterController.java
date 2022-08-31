package com.faforever.client.filter;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ListItem;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.CountryFlagService.Country;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatUserFilterController extends AbstractFilterController<ListItem> {

  private final CountryFlagService countryFlagService;

  private final int MIN_RATING = -1000;
  private final int MAX_RATING = 3000;

  private final StringConverter<PlayerStatus> playerStatusConverter = new StringConverter<>() {
    @Override
    public String toString(PlayerStatus object) {
      return i18n.get(object.getI18nKey());
    }

    @Override
    public PlayerStatus fromString(String string) {
      throw new UnsupportedOperationException("Not supported");
    }
  };

  private final StringConverter<Country> countryConverter = new StringConverter<>() {
    @Override
    public String toString(Country object) {
      return String.format("%s [%s]", object.displayName(), object.code());
    }

    @Override
    public Country fromString(String string) {
      throw new UnsupportedOperationException("Not supported");
    }
  };

  protected ChatUserFilterController(UiService uiService, I18n i18n, CountryFlagService countryFlagService) {
    super(uiService, i18n);
    this.countryFlagService = countryFlagService;
  }

  @Override
  protected void build(FilterBuilder<ListItem> filterBuilder) {
    filterBuilder

        .textField(FilterName.CLAN, i18n.get("chat.filter.clan"),
            (text, item) -> item.isCategory() || text.isEmpty() || item.getUser()
                .filter(user -> user.getClanTag().isPresent())
                .map(ChatChannelUser::getClanTag)
                .map(Optional::get)
                .stream()
                .anyMatch(clan -> StringUtils.containsIgnoreCase(clan, text)))

        .multiCheckbox(FilterName.GAME_STATUS, i18n.get("game.gameStatus"), Arrays.stream(PlayerStatus.values())
                .toList(),
            playerStatusConverter, (selectedStatus, item) -> item.isCategory() || selectedStatus.isEmpty() ||
                item.getUser()
                    .map(ChatChannelUser::getGameStatus)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .stream()
                    .anyMatch(selectedStatus::contains))

        .rangeSlider(FilterName.PLAYER_RATING, i18n.get("game.globalRating"), MIN_RATING, MAX_RATING,
            (pair, item) ->
                item.isCategory() || item.getUser().map(ChatChannelUser::getPlayer)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(player -> RatingUtil.getRating(player.getLeaderboardRatings().get("global")))
                    .stream().anyMatch(rating -> rating >= pair.getKey() && rating <= pair.getValue()))

        .multiCheckbox(FilterName.COUNTRY_CODE, i18n.get("country"), countryFlagService.getCountries(), countryConverter,
            (countries, item) -> item.isCategory() || countries.isEmpty() ||
                item.getUser()
                    .flatMap(ChatChannelUser::getPlayer)
                    .map(PlayerBean::getCountry)
                    .stream()
                    .anyMatch(countryCode -> countries.stream().map(Country::code).toList().contains(countryCode)))

        .build();
  }
}
