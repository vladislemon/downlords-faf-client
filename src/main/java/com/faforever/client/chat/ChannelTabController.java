package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.faforever.client.fx.PlatformService.URL_REGEX_PATTERN;
import static com.faforever.client.player.SocialStatus.FOE;
import static java.util.Locale.US;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChannelTabController extends AbstractChatTabController {

  public static final String MODERATOR_STYLE_CLASS = "moderator";
  public static final String USER_STYLE_CLASS = "user-%s";

  private static final int TOPIC_CHARACTERS_LIMIT = 350;

  private final PlatformService platformService;

  public Tab root;
  public SplitPane splitPane;
  public HBox chatMessageSearchContainer;
  public Button closeChatMessageSearchButton;
  public TextField chatMessageSearchTextField;
  public WebView messagesWebView;
  public TextField messageTextField;
  public HBox topicPane;
  public Label topicCharactersLimitLabel;
  public TextField topicTextField;
  public TextFlow topicText;
  public Button changeTopicTextButton;
  public Button cancelChangesTopicTextButton;
  public ToggleButton userListVisibilityToggleButton;
  public Node chatUserList;
  public ChatUserListController chatUserListController;

  private final ObservableValue<ChannelTopic> channelTopic = chatChannel.flatMap(ChatChannel::topicProperty);
  private final ObservableValue<ObservableList<ChatChannelUser>> users = chatChannel.map(ChatChannel::getUsers)
      .orElse(FXCollections.emptyObservableList());
  private final ListChangeListener<ChatChannelUser> channelUserListChangeListener = this::updateChangedUsersStyles;


  public ChannelTabController(WebViewConfigurer webViewConfigurer, UserService userService, ChatService chatService,
                              PreferencesService preferencesService, PlayerService playerService,
                              AudioService audioService, TimeService timeService, I18n i18n,
                              NotificationService notificationService, UiService uiService, EventBus eventBus,
                              CountryFlagService countryFlagService, EmoticonService emoticonService,
                              PlatformService platformService, ChatPrefs chatPrefs,
                              NotificationPrefs notificationPrefs) {
    super(userService, chatService, preferencesService, playerService, audioService, timeService, i18n, notificationService, uiService, eventBus, webViewConfigurer, emoticonService, countryFlagService, chatPrefs, notificationPrefs);
    this.platformService = platformService;
  }

  @Override
  public void initialize() {
    super.initialize();
    JavaFxUtil.bindManagedToVisible(topicPane, chatUserList, changeTopicTextButton, topicTextField, cancelChangesTopicTextButton, topicText, topicCharactersLimitLabel, chatMessageSearchContainer);
    JavaFxUtil.bind(topicCharactersLimitLabel.visibleProperty(), topicTextField.visibleProperty());
    JavaFxUtil.bind(cancelChangesTopicTextButton.visibleProperty(), topicTextField.visibleProperty());
    JavaFxUtil.bind(chatUserList.visibleProperty(), userListVisibilityToggleButton.selectedProperty());

    ObservableValue<Boolean> showing = getRoot().selectedProperty()
        .and(BooleanExpression.booleanExpression(getRoot().tabPaneProperty().flatMap(JavaFxUtil::showingProperty)));

    userListVisibilityToggleButton.selectedProperty().bindBidirectional(chatPrefs.playerListShownProperty());
    topicTextField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText()
        .length() <= TOPIC_CHARACTERS_LIMIT ? change : null));

    topicCharactersLimitLabel.textProperty()
        .bind(topicTextField.textProperty()
            .length()
            .map(length -> String.format("%d / %d", length.intValue(), TOPIC_CHARACTERS_LIMIT)));

    topicPane.visibleProperty()
        .bind(topicText.visibleProperty()
            .or(changeTopicTextButton.visibleProperty())
            .or(topicTextField.visibleProperty()));

    root.idProperty().bind(channelName);
    root.textProperty().bind(channelName.map(name -> name.replaceFirst("^#", "")));

    chatUserListController.chatChannelProperty().bind(chatChannel);

    ObservableValue<Boolean> isModerator = chatChannel.map(channel -> channel.getUser(userService.getUsername())
        .orElse(null)).flatMap(ChatChannelUser::moderatorProperty).orElse(false);
    changeTopicTextButton.visibleProperty()
        .bind(BooleanExpression.booleanExpression(isModerator)
            .and(topicTextField.visibleProperty().not()));

    chatMessageSearchTextField.textProperty().addListener((SimpleChangeListener<String>) this::highlightText);

    chatPrefs.hideFoeMessagesProperty()
        .when(showing)
        .addListener((SimpleChangeListener<Boolean>) this::hideFoeMessages);
    chatPrefs.chatColorModeProperty()
        .when(showing)
        .addListener((SimpleInvalidationListener) () -> users.getValue().forEach(this::updateUserMessageColor));
    channelTopic
        .addListener(((observable, oldValue, newValue) -> updateChannelTopic(oldValue, newValue)));
    users.addListener((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.removeListener(channelUserListChangeListener);
      }
      if (newValue != null) {
        newValue.addListener(channelUserListChangeListener);
      }
    });

    userListVisibilityToggleButton.selectedProperty().addListener(observable -> updateDividerPosition());

    AutoCompletionHelper autoCompletionHelper = getAutoCompletionHelper();
    autoCompletionHelper.bindTo(messageTextField());
  }

  public AutoCompletionHelper getAutoCompletionHelper() {
    return new AutoCompletionHelper(currentWord -> users.getValue()
        .stream()
        .map(ChatChannelUser::getUsername)
        .filter(username -> username.toLowerCase(US).startsWith(currentWord.toLowerCase()))
        .sorted()
        .collect(Collectors.toList()));
  }

  private void highlightText(String newValue) {
    if (StringUtils.isBlank(newValue)) {
      callJsMethod("removeHighlight");
    } else {
      callJsMethod("highlightText", newValue);
    }
  }

  private void hideFoeMessages(boolean shouldHide) {
    users.getValue()
        .stream()
        .filter(user -> user.getCategories().stream().anyMatch(status -> status == ChatUserCategory.FOE))
        .forEach(user -> updateUserMessageVisibility(user, shouldHide));
  }

  private void updateChangedUsersStyles(Change<? extends ChatChannelUser> change) {
    while (change.next()) {
      if (change.wasUpdated()) {
        List<ChatChannelUser> changedUsers = List.copyOf(change.getList().subList(change.getFrom(), change.getTo()));
        for (ChatChannelUser user : changedUsers) {
          boolean shouldHide = user.getCategories().stream().anyMatch(status -> status == ChatUserCategory.FOE);
          updateUserMessageVisibility(user, shouldHide);
          updateStyleClass(user);
          updateUserMessageColor(user);
        }
      }
    }
  }

  private void updateDividerPosition() {
    boolean selected = userListVisibilityToggleButton.isSelected();
    splitPane.setDividerPositions(selected ? 0.8 : 1);
  }

  private void setChannelTopic(String content) {
    List<Node> children = topicText.getChildren();
    children.clear();
    boolean notBlank = StringUtils.isNotBlank(content);
    if (notBlank) {
      Arrays.stream(content.split("\\s")).forEach(word -> {
        if (URL_REGEX_PATTERN.matcher(word).matches()) {
          Hyperlink link = new Hyperlink(word);
          link.setOnAction(event -> platformService.showDocument(word));
          children.add(link);
        } else {
          children.add(new Label(word + " "));
        }
      });
    }
    topicText.setVisible(notBlank);
  }

  private void updateChannelTopic(ChannelTopic oldTopic, ChannelTopic newTopic) {
    String newTopicContent = newTopic.content();

    JavaFxUtil.runLater(() -> {
      setChannelTopic(newTopicContent);

      if (topicPane.isDisable()) {
        topicTextField.setVisible(false);
        topicPane.setDisable(false);
      }
    });


    if (oldTopic != null) {
      String oldTopicContent = oldTopic.content();
      onChatMessage(new ChatMessage(Instant.now(), newTopic.author(), i18n.get("chat.topicUpdated", oldTopicContent, newTopicContent)));
    }
  }

  public void onChatChannelKeyReleased(KeyEvent keyEvent) {
    KeyCode code = keyEvent.getCode();
    if (code == KeyCode.ESCAPE) {
      onChatMessageSearchButtonClicked();
    } else if (keyEvent.isControlDown() && code == KeyCode.F) {
      showOrHideChatMessageSearchContainer();
    }
  }

  public void onChatMessageSearchButtonClicked() {
    chatMessageSearchContainer.setVisible(false);
    chatMessageSearchTextField.clear();
  }

  public void showOrHideChatMessageSearchContainer() {
    chatMessageSearchContainer.setVisible(!chatMessageSearchContainer.isVisible());
    chatMessageSearchTextField.clear();
    if (chatMessageSearchContainer.isVisible()) {
      chatMessageSearchTextField.requestFocus();
    } else {
      messageTextField().requestFocus();
    }
  }

  private void updateUserMessageColor(ChatChannelUser user) {
    String color = user.getColor().map(JavaFxUtil::toRgbCode).orElse("");
    JavaFxUtil.runLater(() -> callJsMethod("updateUserMessageColor", user.getUsername(), color));
  }

  private void updateUserMessageVisibility(ChatChannelUser user, boolean shouldHide) {
    String displayPropertyValue = shouldHide ? "none" : "";
    JavaFxUtil.runLater(() -> callJsMethod("updateUserMessageDisplay", user.getUsername(), displayPropertyValue));
  }

  private void updateStyleClass(ChatChannelUser user) {
    user.getPlayer()
        .ifPresentOrElse(player -> removeUserMessageStyleClass(user, CSS_CLASS_CHAT_ONLY), () -> addUserMessageStyleClass(user, CSS_CLASS_CHAT_ONLY));
    if (user.isModerator()) {
      addUserMessageStyleClass(user, MODERATOR_STYLE_CLASS);
    } else {
      removeUserMessageStyleClass(user, MODERATOR_STYLE_CLASS);
    }
  }

  private void addUserMessageStyleClass(ChatChannelUser user, String styleClass) {
    JavaFxUtil.runLater(() -> callJsMethod("addUserMessageClass", String.format(USER_STYLE_CLASS, user.getUsername()), styleClass));
  }

  private void removeUserMessageStyleClass(ChatChannelUser user, String styleClass) {
    if (StringUtils.isNotBlank(styleClass)) {
      JavaFxUtil.runLater(() -> callJsMethod("removeUserMessageClass", String.format(USER_STYLE_CLASS, user.getUsername()), styleClass));
    }
  }

  @Override
  protected String getMessageCssClass(String login) {
    return chatService.getOrCreateChatUser(login, channelName.getValue())
        .isModerator() ? MODERATOR_STYLE_CLASS : super.getMessageCssClass(login);
  }

  @Override
  protected void onMention(ChatMessage chatMessage) {
    if (notificationPrefs.getNotifyOnAtMentionOnlyEnabled() && !chatMessage.message()
        .contains("@" + userService.getUsername())) {
      return;
    }

    if (playerService.getPlayerByNameIfOnline(chatMessage.username())
        .filter(player -> player.getSocialStatus() == FOE)
        .isPresent()) {
      log.debug("Ignored ping from {}", chatMessage.username());
    } else if (!hasFocus()) {
      audioService.playChatMentionSound();
      showNotificationIfNecessary(chatMessage);
      incrementUnreadMessagesCount();
      setUnread(true);
    }
  }

  @Override
  protected String getInlineStyle(String username) {
    ChatChannelUser user = chatChannel.map(channel -> channel.getUser(username).orElse(null)).getValue();
    if (user == null) {
      return "";
    }

    if (chatPrefs.isHideFoeMessages() && user.getCategories()
        .stream()
        .anyMatch(category -> category == ChatUserCategory.FOE)) {
      return "display: none;";
    } else {
      return user.getColor().map(color -> String.format("color: %s;", JavaFxUtil.toRgbCode(color))).orElse("");
    }
  }

  public void onChangeTopicTextButtonClicked() {
    topicText.setVisible(false);
    topicTextField.setText(channelTopic.getValue().content());
    topicTextField.setVisible(true);
    topicTextField.requestFocus();
    topicTextField.selectEnd();
  }

  public void onTopicTextFieldEntered() {
    String normalizedText = StringUtils.normalizeSpace(topicTextField.getText());
    if (!normalizedText.equals(channelTopic.getValue().content())) {
      topicPane.setDisable(true);
      chatService.setChannelTopic(chatChannel.getValue(), normalizedText);
    } else {
      onCancelChangesTopicTextButtonClicked();
    }
  }

  public void onCancelChangesTopicTextButtonClicked() {
    topicTextField.setText(channelTopic.getValue().content());
    topicTextField.setVisible(false);
    topicText.setVisible(true);
    changeTopicTextButton.setVisible(true);
  }

  @Override
  public Tab getRoot() {
    return root;
  }

  @Override
  protected TextInputControl messageTextField() {
    return messageTextField;
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }
}
