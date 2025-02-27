package com.faforever.client.game;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.helper.ContextMenuBuilderHelper;
import com.faforever.client.helper.TooltipHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.api.dto.Faction;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerCardControllerTest extends UITest {
  @Mock
  private I18n i18n;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

  @InjectMocks
  private PlayerCardController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/player_card.fxml", clazz -> instance);
    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));
  }

  @Test
  public void testSetFoe() {
    when(i18n.get("userInfo.tooltipFormat.withRating", "foe", 1000)).thenReturn("foe(1000)");

    PlayerBeanBuilder playerBeanBuilder = PlayerBeanBuilder.create()
        .defaultValues()
        .username("foe")
        .socialStatus(SocialStatus.FOE);
    PlayerBean player = playerBeanBuilder.get();
    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.CYBRAN);

    assertTrue(instance.factionIcon.getStyleClass().contains(UiService.CYBRAN_STYLE_CLASS));
    assertTrue(instance.factionIcon.isVisible());
    assertFalse(instance.factionImage.isVisible());
    assertTrue(instance.foeIconText.isVisible());
    assertFalse(instance.friendIconText.isVisible());
    assertEquals("foe(1000)", instance.playerInfo.getText());
  }

  @Test
  public void testSetFriend() {
    when(i18n.get("userInfo.tooltipFormat.withRating", "user", 1000)).thenReturn("user(1000)");

    PlayerBeanBuilder playerBeanBuilder = PlayerBeanBuilder.create()
        .defaultValues()
        .username("user")
        .socialStatus(SocialStatus.FRIEND);
    PlayerBean player = playerBeanBuilder.get();
    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.SERAPHIM);

    assertTrue(instance.factionIcon.getStyleClass().contains(UiService.SERAPHIM_STYLE_CLASS));
    assertTrue(instance.factionIcon.isVisible());
    assertFalse(instance.factionImage.isVisible());
    assertFalse(instance.foeIconText.isVisible());
    assertTrue(instance.friendIconText.isVisible());
    assertEquals("user(1000)", instance.playerInfo.getText());
  }

  @Test
  public void testSetOther() {
    when(i18n.get("userInfo.tooltipFormat.withRating", "user", 1000)).thenReturn("user(1000)");

    PlayerBeanBuilder playerBeanBuilder = PlayerBeanBuilder.create()
        .defaultValues()
        .username("user")
        .socialStatus(SocialStatus.OTHER);
    PlayerBean player = playerBeanBuilder.get();
    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertTrue(instance.factionImage.getImage().getUrl().contains(UiService.RANDOM_FACTION_IMAGE));
    assertFalse(instance.factionIcon.isVisible());
    assertTrue(instance.factionImage.isVisible());
    assertFalse(instance.foeIconText.isVisible());
    assertFalse(instance.friendIconText.isVisible());
    assertEquals("user(1000)", instance.playerInfo.getText());
  }

  @Test
  public void testSetPlayerAvatar() {
    Image avatarImage = mock(Image.class);
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(avatarService.loadAvatar(player.getAvatar())).thenReturn(avatarImage);

    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertTrue(instance.avatarImageView.isVisible());
    assertEquals(avatarImage, instance.avatarImageView.getImage());
  }

  @Test
  public void testInvisibleAvatarImageView() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().avatar(null).get();

    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertFalse(instance.avatarImageView.isVisible());
  }

  @Test
  public void testSetCountryImage() {
    Image countryImage = mock(Image.class);
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(countryFlagService.loadCountryFlag(player.getCountry())).thenReturn(Optional.of(countryImage));

    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertTrue(instance.countryImageView.isVisible());
    assertEquals(countryImage, instance.countryImageView.getImage());
  }

  @Test
  public void testInvisibleCountryImageView() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(countryFlagService.loadCountryFlag(player.getCountry())).thenReturn(Optional.empty());

    instance.setPlayer(player);
    instance.setRating(1000);
    instance.setFaction(Faction.RANDOM);

    assertFalse(instance.countryImageView.isVisible());
  }

  @Test
  public void testOpenContextMenu() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    ContextMenu contextMenuMock = ContextMenuBuilderHelper.mockContextMenuBuilderAndGetContextMenuMock(contextMenuBuilder);
    runOnFxThreadAndWait(() -> {
      instance.setPlayer(player);
      instance.setRating(1000);
      instance.setFaction(Faction.RANDOM);
      instance.openContextMenu(mock(MouseEvent.class));
    });

    verify(contextMenuMock).show(eq(instance.getRoot().getScene().getWindow()), anyDouble(), anyDouble());
  }

  @Test
  public void testNotePlayerTooltip() {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .note("Player 1")
        .game(GameBeanBuilder.create()
            .defaultValues()
            .get())
        .get();
    instance.setPlayer(player);
    instance.setRating(0);
    instance.setFaction(Faction.AEON);
    assertEquals("Player 1", TooltipHelper.getTooltipText(instance.root));

    runOnFxThreadAndWait(() -> player.setNote(""));
    assertNull(TooltipHelper.getTooltip(instance.root));
  }
}