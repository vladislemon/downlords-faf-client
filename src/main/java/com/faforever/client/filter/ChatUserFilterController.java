package com.faforever.client.filter;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ListItem;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatUserFilterController extends AbstractFilterController<ListItem> {

  protected ChatUserFilterController(UiService uiService, I18n i18n) {
    super(uiService, i18n);
  }

  @Override
  protected void build(FilterBuilder<ListItem> filterBuilder) {
    filterBuilder

        .textField(FilterName.CLAN, i18n.get("chat.filter.clan"),
            (text, item) -> item.isCategory() || text.isEmpty() || item.getUser().filter(user -> user.getClanTag().isPresent())
                .map(ChatChannelUser::getClanTag).map(Optional::get).stream().anyMatch(clan -> StringUtils.containsIgnoreCase(clan, text)))

        .build();
  }


}
