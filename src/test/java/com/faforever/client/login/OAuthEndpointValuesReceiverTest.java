package com.faforever.client.login;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.OAuthValuesReceiver.Values;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.verification.Timeout;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthEndpointValuesReceiverTest extends ServiceTest {

  public static final URI REDIRECT_URI = URI.create("http://localhost");
  public static final String STATE = "abc";
  public static final String VERIFIER = "def";

  @InjectMocks
  private OAuthValuesReceiver instance;
  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private UserService userService;

  @Test
  void receiveValues() throws Exception {
    String title = "JUnit Login Success";
    String message = "JUnit Login Message";
    when(i18n.get("login.browser.success.title")).thenReturn(title);
    when(i18n.get("login.browser.success.message")).thenReturn(message);

    CompletableFuture<Values> future = instance.receiveValues(List.of(REDIRECT_URI), STATE, VERIFIER);

    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);

    verify(userService, timeout(2000)).getHydraUrl(eq(STATE), eq(VERIFIER), captor.capture());

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(captor.getValue())
        .queryParam("code", "1234")
        .queryParam("state", "abcd");

    try (InputStream inputStream = uriBuilder.build().toUri().toURL().openStream()) {
      String response = new String(inputStream.readAllBytes());
      assertThat(response, containsString(title));
      assertThat(response, containsString(message));
    }

    Values values = future.get();
    assertThat(values.code(), is("1234"));
    assertThat(values.state(), is("abcd"));
  }

  @Test
  void receiveValuesTwice() throws Exception {
    String title = "JUnit Login Success";
    String message = "JUnit Login Message";
    when(i18n.get("login.browser.success.title")).thenReturn(title);
    when(i18n.get("login.browser.success.message")).thenReturn(message);

    CompletableFuture<Values> future1 = instance.receiveValues(List.of(REDIRECT_URI), STATE, VERIFIER);

    CompletableFuture<Values> future2 = instance.receiveValues(List.of(REDIRECT_URI), STATE, VERIFIER);

    assertEquals(future1, future2);

    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);

    verify(userService, new Timeout(2000, times(2))).getHydraUrl(eq(STATE), eq(VERIFIER), captor.capture());

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(captor.getValue())
        .queryParam("code", "1234")
        .queryParam("state", "abcd");

    try (InputStream inputStream = uriBuilder.build().toUri().toURL().openStream()) {
      String response = new String(inputStream.readAllBytes());
      assertThat(response, containsString(title));
      assertThat(response, containsString(message));
    }

    Values values = future1.get();
    assertThat(values.code(), is("1234"));
    assertThat(values.state(), is("abcd"));
  }

  @Test
  void receiveError() throws Exception {
    String title = "JUnit Login Failure";
    String message = "JUnit Login Message";
    when(i18n.get("login.browser.failed.title")).thenReturn(title);
    when(i18n.get("login.browser.failed.message")).thenReturn(message);

    CompletableFuture<Values> future = instance.receiveValues(List.of(REDIRECT_URI), STATE, VERIFIER);

    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);

    verify(userService, timeout(2000)).getHydraUrl(eq(STATE), eq(VERIFIER), captor.capture());

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(captor.getValue())
        .queryParam("error", "failed");

    try (InputStream inputStream = uriBuilder.build().toUri().toURL().openStream()) {
      String response = new String(inputStream.readAllBytes());
      assertThat(response, containsString(title));
      assertThat(response, containsString(message));
    }

    Exception throwable = assertThrows(ExecutionException.class, future::get);
    assertTrue(throwable.getCause() instanceof IllegalStateException);
  }

  @Test
  void receiveValuesNoPortsAvailable() throws Exception {
    ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    URI takenPort = URI.create("http://localhost:" + serverSocket.getLocalPort());
    CompletableFuture<Values> future = instance.receiveValues(List.of(takenPort), STATE, VERIFIER);
    Exception throwable = assertThrows(ExecutionException.class, future::get);
    assertTrue(throwable.getCause() instanceof IllegalStateException);
  }

  @Test
  void receiveValuesNoPortsGiven() throws Exception {
    assertThrows(ExecutionException.class, () -> instance.receiveValues(List.of(), STATE, VERIFIER).get());
  }

  @Test
  void receiveValuesInvalidUriGiven() throws Exception {
    URI badUri = URI.create("http://www.google.com");
    CompletableFuture<Values> future = instance.receiveValues(List.of(badUri), STATE, VERIFIER);
    Exception throwable = assertThrows(ExecutionException.class, future::get);
    assertTrue(throwable.getCause() instanceof IllegalStateException);
  }

  @Test
  void receiveValuesFirstPortNotAvailable() throws Exception {
    String title = "JUnit Login Success";
    String message = "JUnit Login Message";
    when(i18n.get("login.browser.success.title")).thenReturn(title);
    when(i18n.get("login.browser.success.message")).thenReturn(message);

    ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    URI takenPort = URI.create("http://localhost:" + serverSocket.getLocalPort());

    CompletableFuture<Values> future = instance.receiveValues(List.of(takenPort, REDIRECT_URI), STATE, VERIFIER);
    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);

    verify(userService, timeout(2000)).getHydraUrl(eq(STATE), eq(VERIFIER), captor.capture());

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(captor.getValue())
        .queryParam("code", "1234")
        .queryParam("state", "abcd");

    try (InputStream inputStream = uriBuilder.build().toUri().toURL().openStream()) {
      String response = new String(inputStream.readAllBytes());
      assertThat(response, containsString(title));
      assertThat(response, containsString(message));
    }

    Values values = future.get();
    assertThat(values.code(), is("1234"));
    assertThat(values.state(), is("abcd"));
  }

  @Test
  void assertFirstRedirectUsed() throws Exception {
    String title = "JUnit Login Success";
    String message = "JUnit Login Message";
    when(i18n.get("login.browser.success.title")).thenReturn(title);
    when(i18n.get("login.browser.success.message")).thenReturn(message);

    CompletableFuture<Values> future = instance.receiveValues(List.of(URI.create("http://127.0.0.1"), REDIRECT_URI), STATE, VERIFIER);
    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);

    verify(userService, timeout(2000)).getHydraUrl(eq(STATE), eq(VERIFIER), captor.capture());

    URI usedRedirect = captor.getValue();

    assertEquals("127.0.0.1", usedRedirect.getHost());
  }
}