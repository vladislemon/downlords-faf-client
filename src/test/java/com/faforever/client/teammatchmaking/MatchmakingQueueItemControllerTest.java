package com.faforever.client.teammatchmaking;

import com.faforever.client.builders.MatchmakerQueueBeanBuilder;
import com.faforever.client.builders.PartyBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.MatchingStatus;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PartyBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import com.faforever.client.user.UserService;
import com.faforever.commons.lobby.Player;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MatchmakingQueueItemControllerTest extends UITest {

  @Mock
  private UserService userService;
  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private EventBus eventBus;

  private PlayerBean player;
  @InjectMocks
  private MatchmakingQueueItemController instance;
  private MatchmakerQueueBean queue;
  private PartyBean party;
  private BooleanProperty partyMembersNotReadyProperty;

  @BeforeEach
  public void setUp() throws Exception {
    partyMembersNotReadyProperty = new ReadOnlyBooleanWrapper();

    queue = MatchmakerQueueBeanBuilder.create().defaultValues().get();
    party = PartyBuilder.create().defaultValues().get();
    player = party.getOwner();
    when(teamMatchmakingService.getParty()).thenReturn(party);
    when(i18n.getOrDefault(eq(queue.getTechnicalName()), anyString())).thenReturn(queue.getTechnicalName());
    when(i18n.get(anyString())).thenReturn("");
    when(i18n.get("teammatchmaking.playersInQueue", queue.getPlayersInQueue())).thenReturn(String.valueOf(queue.getPlayersInQueue()));
    when(i18n.get("teammatchmaking.activeGames", queue.getActiveGames())).thenReturn(String.valueOf(queue.getActiveGames()));
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(playerService.currentPlayerProperty()).thenReturn(new ReadOnlyObjectWrapper<>(player));
    Player ownPlayer = new Player(0, "junit", null, null, "us", null, Map.of());
    when(userService.getOwnPlayer()).thenReturn(ownPlayer);
    when(userService.ownPlayerProperty()).thenReturn(new SimpleObjectProperty<>(ownPlayer));
    when(userService.getConnectionState()).thenReturn(ConnectionState.CONNECTED);
    when(userService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTED));

    when(teamMatchmakingService.partyMembersNotReadyProperty()).thenReturn(partyMembersNotReadyProperty);
    when(teamMatchmakingService.partyMembersNotReady()).thenReturn(partyMembersNotReadyProperty.get());
    loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml", clazz -> instance);
    runOnFxThreadAndWait(() -> {
      getRoot().getChildren().add(instance.getRoot());
      instance.setQueue(queue);
    });
  }

  @Test
  public void testQueueNameSet() {
    assertThat(instance.selectButton.getText(), is(queue.getTechnicalName()));
  }

  @Test
  public void testShowMapPool() {
    instance.showMapPool();

    verify(eventBus).post(any(ShowMapPoolEvent.class));
  }

  @Test
  public void testOnJoinLeaveQueueButtonClicked() {
    when(teamMatchmakingService.joinQueues()).thenReturn(CompletableFuture.completedFuture(true));

    runOnFxThreadAndWait(() -> instance.selectButton.fire());

    assertThat(instance.getQueue().isSelected(), is(true));

    runOnFxThreadAndWait(() -> instance.selectButton.fire());

    assertThat(instance.getQueue().isSelected(), is(false));
  }

  @Test
  public void testMatchStatusListeners() {
    assertThat(instance.matchFoundLabel.isVisible(), is(false));
    assertThat(instance.matchStartingLabel.isVisible(), is(false));
    assertThat(instance.matchCancelledLabel.isVisible(), is(false));

    queue.setMatchingStatus(MatchingStatus.MATCH_FOUND);
    assertThat(instance.matchFoundLabel.isVisible(), is(true));
    assertThat(instance.matchStartingLabel.isVisible(), is(false));
    assertThat(instance.matchCancelledLabel.isVisible(), is(false));

    queue.setMatchingStatus(MatchingStatus.MATCH_CANCELLED);
    assertThat(instance.matchFoundLabel.isVisible(), is(false));
    assertThat(instance.matchStartingLabel.isVisible(), is(false));
    assertThat(instance.matchCancelledLabel.isVisible(), is(true));

    queue.setMatchingStatus(MatchingStatus.GAME_LAUNCHING);
    assertThat(instance.matchFoundLabel.isVisible(), is(false));
    assertThat(instance.matchStartingLabel.isVisible(), is(true));
    assertThat(instance.matchCancelledLabel.isVisible(), is(false));

    queue.setMatchingStatus(null);
    assertThat(instance.matchFoundLabel.isVisible(), is(false));
    assertThat(instance.matchStartingLabel.isVisible(), is(false));
    assertThat(instance.matchCancelledLabel.isVisible(), is(false));
  }

  @Test
  public void testPopulationListener() {
    assertThat(instance.playersInQueueLabel.getText(), is(String.valueOf(queue.getPlayersInQueue())));
    when(i18n.get(eq("teammatchmaking.playersInQueue"), anyInt())).thenReturn("10");
    queue.setPlayersInQueue(10);
    assertThat(instance.playersInQueueLabel.getText(), is(String.valueOf(queue.getPlayersInQueue())));
    verify(i18n).get("teammatchmaking.playersInQueue", queue.getPlayersInQueue());
  }

  @Test
  public void testActiveGamesListener() {
    assertThat(instance.activeGamesLabel.getText(), is(String.valueOf(queue.getActiveGames())));
    when(i18n.get(eq("teammatchmaking.activeGames"), anyInt())).thenReturn("10");
    runOnFxThreadAndWait(() -> queue.setActiveGames(10));
    assertThat(instance.activeGamesLabel.getText(), is(String.valueOf(queue.getActiveGames())));
    verify(i18n).get("teammatchmaking.activeGames", queue.getActiveGames());
  }

  @Test
  public void testPartySizeListener() {
    assertThat(instance.selectButton.isDisabled(), is(false));

    runOnFxThreadAndWait(() -> party.getMembers().add(new PartyBuilder.PartyMemberBuilder(
        PlayerBeanBuilder.create().defaultValues().username("notMe").get()).defaultValues().get()));
    assertThat(instance.selectButton.isDisabled(), is(true));

    runOnFxThreadAndWait(() -> party.getMembers().setAll(party.getMembers().get(0)));
    assertThat(instance.selectButton.isDisabled(), is(false));
  }

  @Test
  public void testTeamSizeListener() {
    assertThat(instance.selectButton.isDisabled(), is(false));

    runOnFxThreadAndWait(() -> queue.setTeamSize(0));
    assertThat(instance.selectButton.isDisabled(), is(true));

    runOnFxThreadAndWait(() -> queue.setTeamSize(2));
    assertThat(instance.selectButton.isDisabled(), is(false));
  }

  @Test
  public void testPartyOwnerListener() {
    assertThat(instance.selectButton.isDisabled(), is(false));

    runOnFxThreadAndWait(() -> party.setOwner(PlayerBeanBuilder.create()
        .defaultValues()
        .username("notMe")
        .id(100)
        .get()));
    assertThat(instance.selectButton.isDisabled(), is(true));

    runOnFxThreadAndWait(() -> party.setOwner(player));
    assertThat(instance.selectButton.isDisabled(), is(false));
  }

  @Test
  public void testMembersNotReadyListener() {
    assertThat(instance.selectButton.isDisabled(), is(false));

    when(teamMatchmakingService.partyMembersNotReady()).thenReturn(true);
    runOnFxThreadAndWait(() -> partyMembersNotReadyProperty.set(true));
    assertThat(instance.selectButton.isDisabled(), is(true));
  }
}
