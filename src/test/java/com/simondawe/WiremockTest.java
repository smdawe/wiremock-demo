package com.simondawe;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import static org.junit.Assert.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


public class WiremockTest {

  Logger logger = LoggerFactory.getLogger(WiremockTest.class);

  @Rule
  public WireMockRule wireMockRule = new WireMockRule();

  private HttpClient httpClient;

  private HttpUriRequest httpRequest;

  private String testPath;
  private String testUrl;


  @Before
  public void beforeTest() {
    wireMockRule.resetAll(); //ensure all stubs are removed before running the next test

    httpClient = HttpClients.createDefault();

    testPath = "/" + UUID.randomUUID().toString();

    testUrl = "http://localhost:" + wireMockRule.port() + testPath;
  }

  @Test
  public void stubEndpointThenPrintUrl() throws Exception{
    // when
    wireMockRule.stubFor(get(urlEqualTo(testPath))
      .willReturn(aResponse().withStatus(200).withBody("Hello" + System.lineSeparator()))
    );

    // then
    logger.info("curl " + testUrl);
    Thread.sleep(15_000);
  }

  @Test
  public void verifyUrlHasBeenCalled() throws IOException {
    // given
    HttpGet httpRequest = new HttpGet(testUrl);
    wireMockRule.stubFor(any(urlPathEqualTo(testPath)).willReturn(aResponse().withStatus(200)));

    // when
    httpClient.execute(httpRequest);

    // then
    verify(getRequestedFor(urlPathEqualTo(testPath)));
    verify(0, getRequestedFor(urlPathEqualTo("/another-path")));
  }

  @Test
  public void stubGetRequestWithQueryString() throws Exception {
    //given
    String queryName = "test";
    String queryValue = UUID.randomUUID().toString();
    String body = UUID.randomUUID().toString();

    wireMockRule.stubFor(get(urlPathEqualTo(testPath)).withQueryParam(queryName, equalTo(queryValue)).willReturn(ok(body)));

    httpRequest = new HttpGet(testUrl + "?" + queryName + "=" + queryValue);

    // when
    HttpResponse response = httpClient.execute(httpRequest);

    // then
    String result = getBodyFromResponse(response);
    assertEquals(body, result);
  }

  @Test
  public void stubGetRequestWithHeaders() throws Exception {
    //given
    String headerName = "test";
    String headerValue = UUID.randomUUID().toString();
    String body = UUID.randomUUID().toString();

    wireMockRule.stubFor(get(urlPathEqualTo(testPath)).withHeader(headerName, equalTo(headerValue)).willReturn(ok(body)));

    httpRequest = new HttpGet(testUrl);
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
    String body = UUID.randomUUID().toString();
    String pathRegex = testPath + ".*";

    wireMockRule.stubFor(get(urlMatching(pathRegex)).willReturn(ok(body)));

    httpRequest = new HttpGet(testUrl + "more-text");

    // when
    HttpResponse response = httpClient.execute(httpRequest);

    // then
    String result = getBodyFromResponse(response);
    assertEquals(body, result);
  }

  @Test
  public void stubGetRequestWithMultiplePaths() throws Exception {
    // given
    String body = UUID.randomUUID().toString();
    String pathRegex = testPath + ".*";
    String happyPath = testPath + "/resource";

    wireMockRule.stubFor(get(urlMatching(pathRegex)).atPriority(5).willReturn(unauthorized()));
    wireMockRule.stubFor(get(urlPathEqualTo(happyPath)).willReturn(ok(body)));

    httpRequest = new HttpGet(testUrl + "/resource");
    HttpGet unauthorizedHttpRequest = new HttpGet(testUrl + "/will-401");

    // when
    HttpResponse response = httpClient.execute(httpRequest);
    HttpResponse unauthorizedResponse = httpClient.execute(unauthorizedHttpRequest);

    // then
    String result = getBodyFromResponse(response);
    assertEquals(body, result);
    assertEquals(401, unauthorizedResponse.getStatusLine().getStatusCode());

  }

/*
  @Test
  public void stubGetRequestWithInvalidParam() {
  }

  @Test
  public void stubPostRequest() {

  }



  @Test
  public void simulateLongDelayOnResponse() {

  }*/

  private String getBodyFromResponse(HttpResponse response) throws IOException {
    try (InputStream in = response.getEntity().getContent()) {
      return IOUtils.toString(in, "utf-8");
    }
  }
}
