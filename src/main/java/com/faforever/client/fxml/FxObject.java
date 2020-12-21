package com.faforever.client.fxml;

import com.faforever.client.fx.Controller;

public abstract class FxObject<T extends Controller<?>> {
  public T controller;
}
