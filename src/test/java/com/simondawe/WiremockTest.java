package com.simondawe;


import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


public class WiremockTest {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule();

  private HttpClient httpClient;

  private String baseUrl;

  @Before
  public void beforeTest() {
    wireMockRule.resetAll(); //ensure all stubs are removed before running the next test

    httpClient = HttpClients.createDefault();

    baseUrl = "http://localhost:" + wireMockRule.port();
  }

  @Test
  public void stubEndpointThenPrintUrl() throws Exception{
    // when
    String path = "/my-endpoint";
    wireMockRule.stubFor(get(urlEqualTo(path))
      .willReturn(aResponse().withStatus(200).withBody(htmlResponseBody()))
    );

    // then
    System.out.println("curl " + baseUrl + path);
    Thread.sleep(15_000);
  }

  @Test
  public void verifyUrlHasBeenCalled() throws IOException {
    // given
    String path = "/path-to-test";
    HttpGet httpRequest = new HttpGet(baseUrl + path);
    wireMockRule.stubFor(any(urlPathEqualTo(path))
      .willReturn(aResponse().withStatus(200)));

    // when
    httpClient.execute(httpRequest);

    // then
    verify(getRequestedFor(urlPathEqualTo(path)));
    verify(0, getRequestedFor(urlPathEqualTo("/another-path")));
  }

  @Test
  public void stubGetRequestWithQueryString() throws Exception {
    //given
    String path = "/path-to-test";
    String queryName = "query-param-1";
    String queryValue = "query-value";
    String body = htmlResponseBody();

    wireMockRule.stubFor(get(urlPathEqualTo(path))
      .withQueryParam(queryName, equalTo(queryValue))
      .willReturn(ok(body)));

    HttpGet httpRequest = new HttpGet(baseUrl + path + "?" + queryName + "=" + queryValue);

    // when
    HttpResponse response = httpClient.execute(httpRequest);

    // then
    String result = getBodyFromResponse(response);
    assertEquals(body, result);
  }

  @Test
  public void stubGetRequestWithJsonResponse() throws Exception {
    //given
    String path = "/path-to-test";
    String body = jsonResponseBody();

    wireMockRule.stubFor(get(urlPathEqualTo(path))
      .willReturn(okJson(body)));

    HttpGet httpRequest = new HttpGet(baseUrl + path);

    // when
    HttpResponse response = httpClient.execute(httpRequest);

    // then
    assertEquals("application/json", response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());
  }

  @Test
  public void stubGetRequestWithHeaders() throws Exception {
    //given
    String path = "/path-to-test";
    String headerName = "my-header";
    String headerValue = "my-header-value";
    String body = htmlResponseBody();

    wireMockRule.stubFor(get(urlPathEqualTo(path))
      .withHeader(headerName, equalTo(headerValue))
      .willReturn(ok(body)));

    HttpGet httpRequest = new HttpGet(baseUrl + path);
    httpRequest.setHeader(headerName, headerValue);

    // when
    HttpResponse response = httpClient.execute(httpRequest);

    // then
    String result = getBodyFromResponse(response);
    assertEquals(body, result);
  }



  @Test
  public void stubGetRequestWithRegex() throws Exception {
    // given
    String path = "/path-to-test";
    String body = htmlResponseBody();
    String pathRegex = path + ".*";

    wireMockRule.stubFor(get(urlMatching(pathRegex))
      .willReturn(ok(body)));

    HttpGet httpRequest = new HttpGet(baseUrl + path + "/deeper-path");

    // when
    HttpResponse response = httpClient.execute(httpRequest);

    // then
    String result = getBodyFromResponse(response);
    assertEquals(body, result);
  }

  @Test
  public void stubGetRequestWithMultiplePaths() throws Exception {
    // given
    String path = "/path-to-test";
    String body = htmlResponseBody();
    String pathRegex = path + ".*";
    String happyPath = path + "/resource";

    wireMockRule.stubFor(get(urlMatching(pathRegex))
      .atPriority(5)
      .willReturn(unauthorized()));

    wireMockRule.stubFor(get(urlPathEqualTo(happyPath))
      .willReturn(ok(body)));

    HttpGet httpRequest = new HttpGet(baseUrl + path + "/resource");
    HttpGet unauthorizedHttpRequest = new HttpGet(baseUrl + path + "/will-401");

    // when
    HttpResponse response = httpClient.execute(httpRequest);
    HttpResponse unauthorizedResponse = httpClient.execute(unauthorizedHttpRequest);

    // then
    String result = getBodyFromResponse(response);
    assertEquals(body, result);
    assertEquals(401, unauthorizedResponse.getStatusLine().getStatusCode());

  }

  @Test
  public void stubPostRequest() throws Exception {
    // given
    String path = "/path-to-test";
    String requestBodyKey = "key";
    String requestBodyValue = "value";
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(requestBodyKey, requestBodyValue);
    String requestBody = new Gson().toJson(requestMap);

    wireMockRule.stubFor(post(urlPathEqualTo(path))
      .withRequestBody(equalToJson(requestBody))
      .willReturn(created())
    );

    HttpPost httpRequest = new HttpPost(baseUrl + path);
    httpRequest.setEntity(new StringEntity(requestBody));

    // when
    HttpResponse response = httpClient.execute(httpRequest);

    // then
    assertEquals(201, response.getStatusLine().getStatusCode());
  }

  @Test
  public void stubFault() {
    // given
    String path = "/fault";

    wireMockRule.stubFor(get(urlPathEqualTo(path))
      .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    HttpGet httpRequest = new HttpGet(baseUrl + path);

    // when
    try {
      httpClient.execute(httpRequest);
    } catch (Exception e) {
      // then
      assertEquals(SocketException.class, e.getClass());
      assertEquals("Connection reset", e.getMessage());
    }
  }

  private String htmlResponseBody() throws  IOException{
    return IOUtils.toString(WiremockTest.class.getResourceAsStream("/hello-world.html"), "utf-8");
  }

  private String jsonResponseBody() throws  IOException{
    return IOUtils.toString(WiremockTest.class.getResourceAsStream("/hello-world.json"), "utf-8");
  }

  private String getBodyFromResponse(HttpResponse response) throws IOException {
    try (InputStream in = response.getEntity().getContent()) {
      return IOUtils.toString(in, "utf-8");
    }
  }
}
