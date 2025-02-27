package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.ClanBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.clan.ClanService;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendPrivateMessageClanLeaderMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private ClanService clanService;
  @Mock
  private EventBus eventBus;

  @InjectMocks
  private SendPrivateMessageClanLeaderMenuItem instance;

  @Test
  public void testSendMessageClanLeader() {
    when(clanService.getClanByTag(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(ClanBeanBuilder.create().defaultValues().get())));

    instance.setObject(PlayerBeanBuilder.create().get());
    instance.onClicked();
    verify(eventBus).post(any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testVisibleItem() {
    ClanBean clan = ClanBeanBuilder.create().defaultValues().get();
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    instance.setObject(player);
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenNoClan() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testGetItemText() {
    instance.getItemText();
    verify(i18n).get(any());
  }
}