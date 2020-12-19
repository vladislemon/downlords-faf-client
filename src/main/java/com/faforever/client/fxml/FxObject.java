package com.faforever.client.fxml;

import com.faforever.client.fx.Controller;

public interface FxObject<T extends Controller<?>> {
  T getController();
}
