package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.helper.ContextMenuBuilderHelper;
import com.faforever.client.helper.TooltipHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameStatus;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatUserItemControllerTest extends UITest {

  private static final String CHANNEL_NAME = "#testChannel";
  private static final String USER_NAME = "junit";


  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private MapService mapService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private CountryFlagService countryFlagService;
  @Spy
  private ChatPrefs chatPrefs;

  private ChatChannelUser defaultUser;

  @InjectMocks
  private ChatUserItemController instance;

  @BeforeEach
  public void setUp() throws Exception {
    defaultUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().get();

    when(mapService.isInstalledBinding(anyString())).thenReturn(new SimpleBooleanProperty());
    when(i18n.get(eq("clan.messageLeader"))).thenReturn("Message clan leader");
    when(i18n.get(eq("clan.visitPage"))).thenReturn("Visit clan website");
    doAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0))).when(imageViewHelper)
        .createPlaceholderImageOnErrorObservable(any());

    loadFxml("theme/chat/chat_user_item.fxml", param -> instance);
    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertNotNull(instance.getRoot());
  }

  @Test
  public void testGetPlayer() {
    instance.setChatUser(defaultUser);
    assertEquals(defaultUser, instance.getChatUser());
  }

  @Test
  public void testSingleClickDoesNotInitiatePrivateChat() {
    runOnFxThreadAndWait(() -> instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 1)));
    verify(eventBus, never()).post(CoreMatchers.any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testDoubleClickInitiatesPrivateChat() {
    instance.setChatUser(defaultUser);
    runOnFxThreadAndWait(() -> instance.onItemClicked(MouseEvents.generateClick(MouseButton.PRIMARY, 2)));

    ArgumentCaptor<InitiatePrivateChatEvent> captor = ArgumentCaptor.forClass(InitiatePrivateChatEvent.class);
    verify(eventBus, times(1)).post(captor.capture());
    assertEquals(USER_NAME, captor.getValue().username());
  }

  @Test
  public void testOnContextMenuRequested() {
    runOnFxThreadAndWait(() -> getRoot().getChildren().add(instance.getRoot()));
    ContextMenu contextMenuMock = ContextMenuBuilderHelper.mockContextMenuBuilderAndGetContextMenuMock(contextMenuBuilder);

    instance.setChatUser(defaultUser);
    instance.onContextMenuRequested(mock(ContextMenuEvent.class));
    verify(contextMenuMock).show(eq(instance.getRoot().getScene().getWindow()), anyDouble(), anyDouble());
  }

  @Test
  public void testCheckShowMapNameListener() {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .game(GameBeanBuilder.create().defaultValues().get())
        .get();
    defaultUser.setPlayer(player);

    when(i18n.get(anyString(), anyString())).thenReturn("name");
    when(mapGeneratorService.isGeneratedMap(anyString())).thenReturn(true);

    instance.setChatUser(defaultUser);
    assertEquals(instance.mapNameLabel.isVisible(), chatPrefs.isShowMapName());

    chatPrefs.setShowMapName(!chatPrefs.isShowMapName());
    assertEquals(instance.mapNameLabel.isVisible(), chatPrefs.isShowMapName());
  }

  @Test
  public void testInvisibleMapNameLabelWhenNoMapName() {
    runOnFxThreadAndWait(() -> {
      chatPrefs.setShowMapName(true);
      instance.mapNameLabel.setText("");
    });
    assertFalse(instance.mapNameLabel.isVisible());
  }

  @Test
  public void testCheckShowMapPreviewListener() {
    boolean visible = chatPrefs.isShowMapPreview();
    instance.setChatUser(defaultUser);
    defaultUser.setPlayer(PlayerBeanBuilder.create()
        .defaultValues()
        .game(GameBeanBuilder.create().status(GameStatus.OPEN).get())
        .get());
    assertEquals(instance.mapImageView.isVisible(), visible);

    runOnFxThreadAndWait(() -> chatPrefs.setShowMapPreview(!visible));
    assertEquals(instance.mapImageView.isVisible(), !visible);
  }

  @Test
  public void testCheckChatUserGameListener() {
    GameBean game = GameBeanBuilder.create().defaultValues().host("junit").get();
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .username("junit")
        .game(game)
        .get();
    String mapFolderName = player.getGame().getMapFolderName();
    MapVersionBean mapVersion = MapVersionBeanBuilder.create().defaultValues().get();
    defaultUser.setPlayer(player);

    when(uiService.getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING)).thenReturn(mock(Image.class));
    when(mapService.loadPreview(game.getMapFolderName(), PreviewSize.SMALL)).thenReturn(mock(Image.class));
    when(mapService.getMapLocallyFromName(mapFolderName)).thenReturn(Optional.of(mapVersion));
    when(mapService.convertMapFolderNameToHumanNameIfPossible(mapFolderName)).thenReturn("map name");
    when(i18n.get(eq("game.onMapFormat"), anyString())).thenReturn(mapVersion.getMap()
        .getDisplayName(), "map name", "Neroxis Generated Map");

    runOnFxThreadAndWait(() -> instance.setChatUser(defaultUser));
    assertNotNull(instance.gameStatusImageView.getImage());
    assertNotNull(instance.mapImageView.getImage());
    assertEquals(mapVersion.getMap().getDisplayName(), instance.mapNameLabel.getText());
  }

  @Test
  public void testAvatarImageViewHasTooltip() {
    assertNotNull(TooltipHelper.getTooltip(instance.avatarImageView));
  }

  @Test
  @Disabled("Flaky test due to race condition with tooltip installation")
  public void testPlayerNoteTooltip() throws Exception {
    defaultUser.setPlayer(PlayerBeanBuilder.create()
        .defaultValues()
        .game(GameBeanBuilder.create().defaultValues().get())
        .note("Player 1")
        .get());
    runOnFxThreadAndWait(() -> instance.setChatUser(defaultUser));
    assertEquals("Player 1", TooltipHelper.getTooltipText(instance.userContainer));

    runOnFxThreadAndWait(() -> defaultUser.getPlayer().orElseThrow().setNote(""));
    assertNull(TooltipHelper.getTooltip(instance.userContainer));
  }
}
