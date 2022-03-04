package com.faforever.client.fxml;

import com.faforever.client.fx.Controller;

public abstract class FxViewObject<T extends Controller<?>> {
  protected T controller;

  public void setController(T controller) {
    this.controller = controller;
  }

  public T getController() {
    return this.controller;
  }

  public abstract void initialize();
}
