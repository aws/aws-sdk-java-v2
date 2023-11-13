/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.auth.credentials;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Pair;

public class ContainerCredentialsEndpointProviderTest {

    private static final EnvironmentVariableHelper helper = new EnvironmentVariableHelper();
    public static final String HTTPS_VALID_URI = "https://awscredentials.amazonaws.com/credentials";
    public static final String V4_LOOPBACK_URI = "http://127.0.0.2/credentials";
    public static final String V6_LOOPBACK_URI = "http://[::1]/credentials";
    public static final String COMPLEX_URI = "http://127.0.0.1:8080/credentials?foo=bar%20baz";
    public static final String AUTHORIZATION_HEADER_VALUE = "Basic static%20token2";

    private static final String EKS_CONTAINER_HOST_IPV6 = "http://[fd00:ec2::23]";
    public static final String ECS_CONTAINER_HOST = "http://169.254.170.2";
    public static final String EKS_CONTAINER_HOST_IPV4 = "http://169.254.170.23";
    public static final String RELATIVE_URI_ENV = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";
    public static final String TOKEN_FILE_ENV = "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE";
    public static final String FULL_URI_ENV = "AWS_CONTAINER_CREDENTIALS_FULL_URI";

    public static File testFile  ;

    @BeforeAll
    static void setUp() throws IOException {
        testFile = File.createTempFile("testFile", UUID.randomUUID().toString());
        Files.write(testFile.toPath(), AUTHORIZATION_HEADER_VALUE.getBytes(StandardCharsets.UTF_8));
        testFile.deleteOnExit();
    }

    private static Stream<Arguments> requestConstruction() {

        return Stream.of(
            Arguments.of("should reject forbidden host in full URI",
                         Collections.singletonList(Pair.of(FULL_URI_ENV, "http://192.168.1.1/endpoint")),
                         "http://192.168.1.1/endpoint",
                         new Result().type("error").reason("Host should resolve to a loopback address or have the full URI be HTTPS.")),

            Arguments.of("should reject forbidden link-local host in full URI",
                         Collections.singletonList(Pair.of(FULL_URI_ENV, "http://169.254.170.3/endpoint")),
                         "http://192.168.1.1/endpoint",
                         new Result().type("error").reason("Host should resolve to a loopback address or have the full URI be HTTPS.")),

            Arguments.of("should reject invalid token file path",
                         Arrays.asList(
                             Pair.of(RELATIVE_URI_ENV, "/endpoint"),
                             Pair.of(TOKEN_FILE_ENV, "/full/path/to/token/file")
                         ),
                         AWS_CONTAINER_SERVICE_ENDPOINT.defaultValue(),
                         new Result().type("headerError").reason("Failed to read authorization token from '/full/path/to/token/file':"
                                                           + " no such file or directory")),

            Arguments.of("https URI",
                         Collections.singletonList(Pair.of(FULL_URI_ENV, HTTPS_VALID_URI)),
                         HTTPS_VALID_URI,
                         new Result().type("success").sdkRequest(
                             SdkHttpFullRequest.builder()
                                               .uri(URI.create(HTTPS_VALID_URI))
                                               .method(SdkHttpMethod.GET)
                                               .headers(new HashMap<>())
                                               .build())),

            Arguments.of("http loopback(v4) URI",
                         Collections.singletonList(Pair.of(FULL_URI_ENV, V4_LOOPBACK_URI)),
                         V4_LOOPBACK_URI,
                         new Result().type("success").sdkRequest(
                             SdkHttpFullRequest.builder()
                                               .uri(URI.create(V4_LOOPBACK_URI))
                                               .method(SdkHttpMethod.GET)
                                               .headers(new HashMap<>())
                                               .build())),

            Arguments.of("http loopback(v6) URI",
                         Collections.singletonList(Pair.of(FULL_URI_ENV, V6_LOOPBACK_URI)),
                         V6_LOOPBACK_URI,
                         new Result().type("success").sdkRequest(
                             SdkHttpFullRequest.builder()
                                               .uri(URI.create(V6_LOOPBACK_URI))
                                               .method(SdkHttpMethod.GET)
                                               .headers(new HashMap<>())
                                               .build())),

            Arguments.of("http link-local ECS URI",
                         Collections.singletonList(Pair.of(FULL_URI_ENV, ECS_CONTAINER_HOST + "/credentials")),
                         ECS_CONTAINER_HOST + "/credentials",
                         new Result().type("success").sdkRequest(
                             SdkHttpFullRequest.builder()
                                               .uri(URI.create(ECS_CONTAINER_HOST + "/credentials"))
                                               .method(SdkHttpMethod.GET)
                                               .headers(new HashMap<>())
                                               .build())),

            Arguments.of("http link-local EKS URI",
                         Collections.singletonList(Pair.of(FULL_URI_ENV, EKS_CONTAINER_HOST_IPV4 + "/credentials")),
                         EKS_CONTAINER_HOST_IPV4 + "/credentials",
                         new Result().type("success").sdkRequest(
                             SdkHttpFullRequest.builder()
                                               .uri(URI.create(EKS_CONTAINER_HOST_IPV4 + "/credentials"))
                                               .method(SdkHttpMethod.GET)
                                               .headers(new HashMap<>())
                                               .build())),

            Arguments.of("http link-local EKS URI with IPv6",
                         Arrays.asList(
                             Pair.of(FULL_URI_ENV, EKS_CONTAINER_HOST_IPV6 + "/credentials"),
                             Pair.of("AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE", "IPv6")

                         ),
                         EKS_CONTAINER_HOST_IPV6 + "/credentials",
                         new Result().type("success").sdkRequest(
                             SdkHttpFullRequest.builder()
                                               .uri(URI.create(EKS_CONTAINER_HOST_IPV6 + "/credentials"))
                                               .method(SdkHttpMethod.GET)
                                               .headers(new HashMap<>())
                                               .build())),

            Arguments.of("complex full URI",
                         Collections.singletonList(Pair.of(FULL_URI_ENV, COMPLEX_URI)),
                         COMPLEX_URI,
                         new Result().type("success").sdkRequest(
                             SdkHttpFullRequest.builder()
                                               .uri(URI.create(COMPLEX_URI))
                                               .method(SdkHttpMethod.GET)
                                               .headers(new HashMap<>())
                                               .build())),

            Arguments.of("auth token from file",
                         Arrays.asList(
                             Pair.of(RELATIVE_URI_ENV, "/credentials-relative"),
                             Pair.of(TOKEN_FILE_ENV, testFile.toPath().toString())
                         ),
                         AWS_CONTAINER_SERVICE_ENDPOINT.defaultValue(),
                         new Result().type("success").sdkRequest(
                             SdkHttpFullRequest.builder()
                                               .uri(URI.create(AWS_CONTAINER_SERVICE_ENDPOINT.defaultValue()+"/credentials-relative"))
                                               .method(SdkHttpMethod.GET)
                                               .headers(addHeaders(Pair.of("Authorization", AUTHORIZATION_HEADER_VALUE)))
                                               .build())),

            Arguments.of("auth token from env",
                         Arrays.asList(
                             Pair.of(RELATIVE_URI_ENV, "/credentials-relative"),
                             Pair.of("AWS_CONTAINER_AUTHORIZATION_TOKEN", "Basic static%20token2")
                             ),
                         AWS_CONTAINER_SERVICE_ENDPOINT.defaultValue(),
                         new Result().type("success").sdkRequest(
                             SdkHttpFullRequest.builder()
                                               .uri(URI.create(AWS_CONTAINER_SERVICE_ENDPOINT.defaultValue()+"/credentials-relative"))
                                               .method(SdkHttpMethod.GET)
                                               .headers(addHeaders(Pair.of("Authorization", "Basic static%20token2")))
                                               .build()))
        );
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("requestConstruction")
    void standardTestCases(String testCase, List<Pair<String, String>> envKeyValues, String hostname, Result expected) throws IOException {
        assertThat(expected).isNotNull();
        envKeyValues.forEach(pair -> {
            helper.set(pair.left(), pair.right());
        });

        boolean relativeURIPresent = envKeyValues.stream().anyMatch(p -> RELATIVE_URI_ENV.equals(p.left()));
        ContainerCredentialsProvider.ContainerCredentialsEndpointProvider provider = relativeURIPresent ?
            new ContainerCredentialsProvider.ContainerCredentialsEndpointProvider(hostname) : sut;
        if("success".equals(expected.type) ){
            assertThat(provider.endpoint()).hasToString(expected.sdkRequest.getUri().toString());
            expected.sdkRequest.firstMatchingHeader("Authorization")
                               .ifPresent(header -> assertThat(header).isEqualTo(provider.headers().get("Authorization")));
        }else if ("error".equals(expected.type)){
            Assertions.assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> provider.endpoint())
                      .withMessageContaining(expected.reason);
        }
        else if ("headerError".equals(expected.type)){
            Assertions.assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> provider.headers())
                      .withMessageContaining(expected.reason);
        }else {
            throw new IllegalStateException("Unknown expected.type " +expected.type);
        }
    }


    private static final ContainerCredentialsProvider.ContainerCredentialsEndpointProvider sut =
        new ContainerCredentialsProvider.ContainerCredentialsEndpointProvider(null);

    @BeforeEach
    void clearContainerVariablesIncaseWereRunningTestsOnEC2() {
        helper.remove(AWS_CONTAINER_CREDENTIALS_RELATIVE_URI);
        helper.remove(AWS_CONTAINER_AUTHORIZATION_TOKEN);
        helper.remove(AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE);
    }

    @AfterEach
    public void restoreOriginal() {
        helper.reset();
    }

    @Test
    void takesUriFromOverride() throws IOException {
        String hostname = "http://localhost:8080";
        String path = "/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_RELATIVE_URI, path);
        assertThat(new ContainerCredentialsProvider.ContainerCredentialsEndpointProvider(hostname).endpoint())
            .hasToString(hostname + path);
    }

    @Test
    void takesUriFromTheEnvironmentVariable() throws IOException {
        String fullUri = "http://localhost:8080/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);
        assertThat(sut.endpoint()).hasToString(fullUri);
    }

    @Test
    void theLoopbackAddressIsAlsoAcceptable() throws IOException {
        String fullUri = "http://127.0.0.1:9851/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint()).hasToString(fullUri);
    }

    @Test
    void theLoopbackIpv6AddressIsAlsoAcceptable() throws IOException {
        String fullUri = "http://[::1]:9851/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint()).hasToString(fullUri);
    }

    @Test
    void anyHttpsAddressIsAlsoAcceptable() throws IOException {
        String fullUri = "https://192.168.10.120:9851/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint()).hasToString(fullUri);
    }

    @Test
    void anyHttpsIpv6AddressIsAlsoAcceptable() throws IOException {
        String fullUri = "https://[::FFFF:152.16.24.123]/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        assertThat(sut.endpoint()).hasToString(fullUri);
    }

    @Test
    void nonLoopbackAddressIsNotAcceptable() throws IOException {
        String fullUri = "http://192.168.10.120:9851/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        Assertions.assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> sut.endpoint());
    }

    @Test
    void nonLoopbackIpv6AddressIsNotAcceptable() throws IOException {
        String fullUri = "http://[::FFFF:152.16.24.123]/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);

        Assertions.assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> sut.endpoint());
    }

    @Test
    void onlyLocalHostAddressesAreValid()  {
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), "http://google.com/endpoint");
        Assertions.assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> sut.endpoint());
    }

    @Test
    void authorizationHeaderIsPresentIfEnvironmentVariableSet() {
        helper.set(AWS_CONTAINER_AUTHORIZATION_TOKEN.environmentVariable(), "hello authorized world!");
        Map<String, String> headers = sut.headers();
        assertThat(headers).hasSize(2);
        assertThat(headers).containsEntry("Authorization", "hello authorized world!");
        assertThat(headers).containsEntry("User-Agent", SdkUserAgent.create().userAgent());
    }

    @Test
    void failureWhileReadingTokenFile(){

        File tokenDir = new File(UUID.randomUUID().toString());
        tokenDir.mkdir();
        String fullUri = "https://192.168.10.120:9851/endpoint";
        helper.set(AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(), fullUri);
        helper.set(AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE.environmentVariable(), tokenDir.getAbsolutePath());
        Assertions.assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> sut.headers())
                  .withMessage(String.format("Failed to read %s.",tokenDir.getAbsolutePath()));
        tokenDir.delete();
    }

    private static Map<String, List<String>> addHeaders(Pair<String, String>... authorization) {
        return Arrays.stream(authorization)
                     .collect(Collectors.groupingBy(
                         Pair::left,
                         LinkedHashMap::new,
                         Collectors.mapping(Pair::right, Collectors.toList())
                     ));
    }

    static class Result{
        private String type;
        SdkHttpFullRequest sdkRequest;
        String reason;


        Result type(String type){
            this.type = type;
            return this;
        }
        Result sdkRequest(SdkHttpFullRequest sdkRequest){
            this.sdkRequest = sdkRequest;
            return this;
        }
        Result reason(String reason){
            this.reason = reason;
            return this;
        }
    }
}
