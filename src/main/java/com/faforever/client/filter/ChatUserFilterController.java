package com.faforever.client.filter;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ListItem;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatUserFilterController extends AbstractFilterController<ListItem> {

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

  protected ChatUserFilterController(UiService uiService, I18n i18n) {
    super(uiService, i18n);
  }

  @Override
  protected void build(FilterBuilder<ListItem> filterBuilder) {
    filterBuilder

        .textField(FilterName.CLAN, i18n.get("chat.filter.clan"),
            (text, item) -> item.isCategory() || text.isEmpty() || item.getUser().filter(user -> user.getClanTag().isPresent())
                .map(ChatChannelUser::getClanTag).map(Optional::get).stream().anyMatch(clan -> StringUtils.containsIgnoreCase(clan, text)))

        .multiCheckbox(FilterName.GAME_STATUS, i18n.get("game.gameStatus"),
            CompletableFuture.completedFuture(Arrays.stream(PlayerStatus.values()).toList()), playerStatusConverter,
            (selectedStatus, item) -> item.isCategory() || selectedStatus.isEmpty() ||
                item.getUser().map(ChatChannelUser::getGameStatus).filter(Optional::isPresent).map(Optional::get).stream().anyMatch(selectedStatus::contains))

        .build();
  }


}
