package com.simondawe;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


public class WiremockTest {

  Logger logger = LoggerFactory.getLogger(WiremockTest.class);

  @ClassRule
  public static WireMockRule wireMockRule = new WireMockRule();
  private static String baseUrl;


  private HttpClient httpClient;

  private HttpUriRequest httpRequest;

  private String testPath;
  private String testUrl;

  @BeforeClass
  public static void beforeClass() {
    baseUrl = "http://localhost:" + wireMockRule.port();
  }


  @Before
  public void beforeTest() {
    wireMockRule.resetAll(); //ensure all stubs are removed before running the next test

    httpClient = HttpClients.createDefault();

    testPath = "/" + UUID.randomUUID().toString();

    testUrl = baseUrl + testPath;
  }

  @Test
  public void stubEndpointThenPrintUrl() throws Exception{
    // when
    wireMockRule.stubFor(get(urlEqualTo(testPath))
      .willReturn(aResponse().withStatus(200).withBody("Hello" + System.lineSeparator()))
    );

    // then
    logger.info("curl " + testUrl);
    Thread.sleep(30_000);
  }

  @Test
  public void verifyUrlHasBeenCalled() throws IOException {
    // given
    HttpGet httpRequest = new HttpGet(baseUrl + testPath);
    wireMockRule.stubFor(any(urlPathEqualTo(testPath)).willReturn(aResponse().withStatus(200)));

    // when
    httpClient.execute(httpRequest);

    // then
    verify(getRequestedFor(urlPathEqualTo(testPath)));
    verify(0, getRequestedFor(urlPathEqualTo("/another-path")));
  }
/*


  @Test
  public void stubGetRequestWithQueryString() {
    //given
    String queryName = "test";
    String queryParam = UUID.randomUUID().toString();
    String response = UUID.randomUUID().toString();

    wireMockRule.stubFor(get(urlPathEqualTo(testPath)).withQueryParam(queryName, equalTo(queryParam)).willReturn(
      aResponse().withStatus(200).withBody(response)
    ));

    httpRequest = new HttpGet(testUrl);

    // when

  }

  @Test
  public void stubGetRequestWithHeaders() {

  }



  @Test
  public void stubPostRequest() {

  }



  @Test
  public void simulateLongDelayOnResponse() {

  }*/
}
