package com.faforever.client.mod;

import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModManagerController implements Controller<Parent> {

  private static final Predicate<ModVersionBean> UI_FILTER = modVersion -> modVersion.getModType() == ModType.UI;
  private static final Predicate<ModVersionBean> SIM_FILTER = modVersion -> modVersion.getModType() == ModType.SIM;

  private final ModService modService;
  private final Set<ModVersionBean> selectedMods = new HashSet<>();
  public Button closeButton;
  private Runnable onCloseButtonClickedListener;
  public ToggleButton uiModsButton;
  public ToggleGroup viewToggleGroup;
  public ToggleButton simModsButton;
  public ListView<ModVersionBean> modListView;
  public VBox root;

  private FilteredList<ModVersionBean> modVersionFilteredList;

  public void onShowUIMods() {
    filterModList();
  }

  public void onShowSimMods() {
    filterModList();
  }

  public void onDeselectModsButtonClicked() {
    modListView.getSelectionModel().clearSelection();
    modVersionFilteredList.forEach(selectedMods::remove);
  }

  public void onReloadModsButtonClicked() {
    loadActivatedMods();
  }

  public void onCloseButtonClicked() {
    onCloseButtonClickedListener.run();
  }

  public void setOnCloseButtonClickedListener(Runnable onCloseButtonClickedListener) {
    this.onCloseButtonClickedListener = onCloseButtonClickedListener;
  }

  public void setCloseable(boolean closeable) {
    closeButton.setVisible(closeable);
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(closeButton);
    modListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    modListView.setCellFactory(modListCellFactory());

    viewToggleGroup.selectToggle(uiModsButton);

    loadActivatedMods();

    modListView.scrollTo(modListView.getSelectionModel().getSelectedItem());
    setCloseable(true);
  }

  private void loadActivatedMods() {
    ObservableList<ModVersionBean> installedModVersions = modService.getInstalledMods();
    try {
      selectedMods.addAll(modService.getActivatedSimAndUIMods());
    } catch (IOException e) {
      log.error("Activated mods could not be loaded", e);
    }
    modVersionFilteredList = new FilteredList<>(installedModVersions);
    modListView.setItems(modVersionFilteredList);
    JavaFxUtil.addAndTriggerListener(viewToggleGroup.selectedToggleProperty(), observable -> filterModList());
  }

  private void filterModList() {
    modVersionFilteredList.setPredicate(viewToggleGroup.getSelectedToggle() == uiModsButton ? UI_FILTER : SIM_FILTER);
    modVersionFilteredList.forEach(modVersion -> {
      if (selectedMods.contains(modVersion)) {
        modListView.getSelectionModel().select(modVersion);
      } else {
        modListView.getSelectionModel().clearSelection(modListView.getItems().indexOf(modVersion));
      }
    });
  }

  @NotNull
  private Callback<ListView<ModVersionBean>, ListCell<ModVersionBean>> modListCellFactory() {
    return param -> {
      ListCell<ModVersionBean> cell = new StringListCell<>(modVersion -> modVersion.getMod().getDisplayName());
      cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
        modListView.requestFocus();
        MultipleSelectionModel<ModVersionBean> selectionModel = modListView.getSelectionModel();
        if (!cell.isEmpty()) {
          int index = cell.getIndex();
          if (selectionModel.getSelectedIndices().contains(index)) {
            selectionModel.clearSelection(index);
            selectedMods.remove(cell.getItem());
          } else {
            selectionModel.select(index);
            selectedMods.add(cell.getItem());
          }
          event.consume();
        }
      });
      return cell;
    };
  }

  public Set<ModVersionBean> apply() {
    Set<ModVersionBean> mods = getSelectedModVersions();
    modService.overrideActivatedMods(mods);
    return mods;
  }

  @NotNull
  public Set<ModVersionBean> getSelectedModVersions() {
    return Collections.unmodifiableSet(selectedMods);
  }
}
