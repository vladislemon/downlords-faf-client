package com.faforever.client.mod;

import com.faforever.client.builders.ModVersionBeanBuilder;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.VaultEntityController.SearchType;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.google.common.eventbus.EventBus;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModVaultControllerTest extends UITest {

  @InjectMocks
  private ModVaultController instance;

  @Mock
  private ModService modService;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;

  @Mock
  private NotificationService notificationService;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private SearchController searchController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private ReportingService reportingService;
  @Mock
  private PlatformService platformService;
  @Spy
  private VaultPrefs vaultPrefs;

  private ModDetailController modDetailController;

  @BeforeEach
  public void setUp() throws Exception {
    when(modService.getNewestModsWithPageCount(anyInt(), anyInt())).thenReturn(Mono.zip(Mono.just(List.<ModVersionBean>of()), Mono.just(0)).toFuture());
    when(modService.getHighestRatedModsWithPageCount(anyInt(), anyInt())).thenReturn(Mono.zip(Mono.just(List.<ModVersionBean>of()), Mono.just(0)).toFuture());
    when(modService.getHighestRatedUiModsWithPageCount(anyInt(), anyInt())).thenReturn(Mono.zip(Mono.just(List.<ModVersionBean>of()), Mono.just(0)).toFuture());
    when(i18n.get(anyString())).thenReturn("test");

    doAnswer(invocation -> {
      modDetailController = mock(ModDetailController.class);
      when(modDetailController.getRoot()).then(invocation1 -> new Pane());
      return modDetailController;
    }).when(uiService).loadFxml("theme/vault/mod/mod_detail.fxml");

    when(modService.getRecommendedModPageCount(VaultEntityController.TOP_ELEMENT_COUNT)).thenReturn(CompletableFuture.completedFuture(0));

    loadFxml("theme/vault/vault_entity.fxml", clazz -> {
      if (SearchController.class.isAssignableFrom(clazz)) {
        return searchController;
      }
      if (SpecificationController.class.isAssignableFrom(clazz)) {
        return specificationController;
      }
      if (LogicalNodeController.class.isAssignableFrom(clazz)) {
        return logicalNodeController;
      }
      return instance;
    }, instance);
  }

  @Test
  public void testSetSupplier() {
    SortConfig sortOrder = vaultPrefs.getMapSortConfig();
    SearchConfig standardSearchConfig = new SearchConfig(sortOrder, "query");
    instance.searchType = SearchType.SEARCH;
    instance.setSupplier(standardSearchConfig);
    instance.searchType = SearchType.NEWEST;
    instance.setSupplier(null);
    instance.searchType = SearchType.HIGHEST_RATED;
    instance.setSupplier(null);
    instance.searchType = SearchType.HIGHEST_RATED_UI;
    instance.setSupplier(null);

    verify(modService).findByQueryWithPageCount(standardSearchConfig, instance.pageSize, 1);
    verify(modService).getHighestRatedModsWithPageCount(instance.pageSize, 1);
    verify(modService).getHighestRatedUiModsWithPageCount(instance.pageSize, 1);
    verify(modService).getNewestModsWithPageCount(instance.pageSize, 1);
  }

  @Test
  public void testShowModDetail() throws MalformedURLException {
    ModVersionBean modVersion = ModVersionBeanBuilder.create().defaultValues().get();
    JavaFxUtil.runLater(() -> instance.onDisplayDetails(modVersion));
    WaitForAsyncUtils.waitForFxEvents();

    verify(modDetailController).setModVersion(modVersion);
    assertThat(modDetailController.getRoot().isVisible(), is(true));
  }
}
