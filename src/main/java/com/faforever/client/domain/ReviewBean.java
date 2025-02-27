package com.faforever.client.domain;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.maven.artifact.versioning.ComparableVersion;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Data
public abstract class ReviewBean extends AbstractEntityBean<ReviewBean> {
  private final StringProperty text = new SimpleStringProperty();
  private final ObjectProperty<PlayerBean> player = new SimpleObjectProperty<>();
  private final IntegerProperty score = new SimpleIntegerProperty();
  protected final ObjectProperty<ComparableVersion> version = new SimpleObjectProperty<>();
  protected final ObjectProperty<ComparableVersion> latestVersion = new SimpleObjectProperty<>();

  public String getText() {
    return text.get();
  }

  public void setText(String text) {
    this.text.set(text);
  }

  public StringProperty textProperty() {
    return text;
  }

  public PlayerBean getPlayer() {
    return player.get();
  }

  public void setPlayer(PlayerBean player) {
    this.player.set(player);
  }

  public ObjectProperty<PlayerBean> playerProperty() {
    return player;
  }

  public int getScore() {
    return score.get();
  }

  public void setScore(int score) {
    this.score.set(score);
  }

  public IntegerProperty scoreProperty() {
    return score;
  }

  public ComparableVersion getVersion() {
    return versionProperty().getValue();
  }

  public ComparableVersion getLatestVersion() {
    return latestVersionProperty().getValue();
  }

  public abstract ObservableValue<ComparableVersion> versionProperty();

  public abstract ObservableValue<ComparableVersion> latestVersionProperty();
}
