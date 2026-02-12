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

package software.amazon.awssdk.services.cloudfront;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.internal.utils.SigningUtils;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontOriginAccessIdentitySummary;
import software.amazon.awssdk.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityResponse;
import software.amazon.awssdk.services.cloudfront.model.CreatePublicKeyResponse;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.Distribution;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.DistributionSummary;
import software.amazon.awssdk.services.cloudfront.model.GetKeyGroupResponse;
import software.amazon.awssdk.services.cloudfront.model.KeyGroup;
import software.amazon.awssdk.services.cloudfront.model.KeyGroupConfig;
import software.amazon.awssdk.services.cloudfront.model.KeyGroupSummary;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.PriceClass;
import software.amazon.awssdk.services.cloudfront.model.PublicKeySummary;
import software.amazon.awssdk.services.cloudfront.model.ViewerProtocolPolicy;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.Waiter;

public class CloudFrontUtilitiesIntegrationTest extends IntegrationTestBase {
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final String RESOURCE_PREFIX = "do-not-delete-cf-test-v2";
    private static final String CALLER_REFERENCE = UUID.randomUUID().toString();
    private static final String S3_OBJECT_KEY = "s3ObjectKey";
    private static final String S3_OBJECT_KEY_ON_SUB_PATH = "foo/specific-file";
    private static final String S3_OBJECT_KEY_ON_SUB_PATH_OTHER = "foo/other-file";


    private static String bucket;
    private static String domainName;
    private static String resourceUrl;
    private static String rsaKeyPairId;
    private static PrivateKey rsaPrivateKey;
    private static File rsaKeyFile;
    private static Path rsaKeyFilePath;
    private static String keyGroupId;

    private static String ecKeyPairId;
    private static PrivateKey ecPrivateKey;
    private static File ecKeyFile;
    private static Path ecKeyFilePath;

    private static String originAccessId;
    private static String distributionId;

    @BeforeAll
    public static void init() throws Exception {
        IntegrationTestBase.setUp();
        initStaticFields();
    }

    private static class KeyTestCase {
        final String name;
        final String keyPairId;
        final PrivateKey privateKey;
        final Path keyFilePath;

        KeyTestCase(String name, String keyPairId, PrivateKey privateKey, Path keyFilePath) {
            this.name = name;
            this.keyPairId = keyPairId;
            this.privateKey = privateKey;
            this.keyFilePath = keyFilePath;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<KeyTestCase> keyCases() throws Exception {
        return Stream.of(
            new KeyTestCase("RSA", rsaKeyPairId, rsaPrivateKey, rsaKeyFilePath),
            new KeyTestCase("ECDSA", ecKeyPairId, ecPrivateKey, ecKeyFilePath)
        );
    }


    @Test
    void unsignedUrl_shouldReturn403Response() throws Exception {
        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(SdkHttpRequest.builder()
                                                                           .encodedPath(resourceUrl)
                                                                           .host(domainName)
                                                                           .method(SdkHttpMethod.GET)
                                                                           .protocol("https")
                                                                           .build())
                                                    .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedUrlWithCannedPolicy_producesValidUrl(KeyTestCase testCase) throws Exception {
        InputStream originalBucketContent = s3Client.getObject(r -> r.bucket(bucket).key(S3_OBJECT_KEY));
        Instant expirationDate = LocalDate.of(2050, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CannedSignerRequest request = CannedSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(testCase.keyFilePath)
                                                         .keyPairId(testCase.keyPairId)
                                                         .expirationDate(expirationDate).build();
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCannedPolicy(request);
        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(signedUrl.createHttpGetRequest())
                                                                               .build()).call();
        int expectedStatus = 200;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = response.responseBody().get();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedUrlWithCannedPolicy_withExpiredDate_shouldReturn403Response(KeyTestCase testCase) throws Exception {
        Instant expirationDate = LocalDate.of(2020, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCannedPolicy(r -> r.resourceUrl(resourceUrl)
                                                                                      .privateKey(testCase.privateKey)
                                                                                      .keyPairId(testCase.keyPairId)
                                                                                      .expirationDate(expirationDate));
        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(signedUrl.createHttpGetRequest())
                                                                               .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedUrlWithCustomPolicy_producesValidUrl(KeyTestCase testCase) throws Exception {
        InputStream originalBucketContent = s3Client.getObject(r -> r.bucket(bucket).key(S3_OBJECT_KEY));
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2050, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(testCase.keyFilePath)
                                                         .keyPairId(testCase.keyPairId)
                                                         .expirationDate(expirationDate)
                                                         .activeDate(activeDate).build();
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(request);
        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(signedUrl.createHttpGetRequest())
                                                                               .build()).call();
        int expectedStatus = 200;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = response.responseBody().get();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @Test
    void getSignedUrlWithCustomPolicy_withFutureActiveDate_shouldReturn403Response() throws Exception {
        Instant activeDate = LocalDate.of(2040, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2050, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(r -> r.resourceUrl(resourceUrl)
                                                                                      .privateKey(rsaPrivateKey)
                                                                                      .keyPairId(rsaKeyPairId)
                                                                                      .expirationDate(expirationDate)
                                                                                      .activeDate(activeDate));
        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(signedUrl.createHttpGetRequest())
                                                                               .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getCookiesForCannedPolicy_producesValidCookies(KeyTestCase testCase) throws Exception {
        InputStream originalBucketContent = s3Client.getObject(r -> r.bucket(bucket).key(S3_OBJECT_KEY));
        Instant expirationDate = LocalDate.of(2050, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CookiesForCannedPolicy cookies = cloudFrontUtilities.getCookiesForCannedPolicy(r -> r.resourceUrl(resourceUrl)
                                                                                             .privateKey(testCase.privateKey)
                                                                                             .keyPairId(testCase.keyPairId)
                                                                                             .expirationDate(expirationDate));

        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(cookies.createHttpGetRequest())
                                                                               .build()).call();
        int expectedStatus = 200;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = response.responseBody().get();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @Test
    void getCookiesForCannedPolicy_withExpiredDate_shouldReturn403Response() throws Exception {
        Instant expirationDate = LocalDate.of(2020, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CannedSignerRequest request = CannedSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(rsaKeyFilePath)
                                                         .keyPairId(rsaKeyPairId)
                                                         .expirationDate(expirationDate).build();
        CookiesForCannedPolicy cookies = cloudFrontUtilities.getCookiesForCannedPolicy(request);

        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(cookies.createHttpGetRequest())
                                                                               .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getCookiesForCustomPolicy_producesValidCookies(KeyTestCase testCase) throws Exception {
        InputStream originalBucketContent = s3Client.getObject(r -> r.bucket(bucket).key(S3_OBJECT_KEY));
        Instant activeDate = LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2050, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CookiesForCustomPolicy cookies = cloudFrontUtilities.getCookiesForCustomPolicy(r -> r.resourceUrl(resourceUrl)
                                                                                             .privateKey(testCase.privateKey)
                                                                                             .keyPairId(testCase.keyPairId)
                                                                                             .expirationDate(expirationDate)
                                                                                             .activeDate(activeDate));

        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(cookies.createHttpGetRequest())
                                                                               .build()).call();
        int expectedStatus = 200;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);

        InputStream retrievedBucketContent = response.responseBody().get();
        assertThat(retrievedBucketContent).hasSameContentAs(originalBucketContent);
    }

    @Test
    void getCookiesForCustomPolicy_withFutureActiveDate_shouldReturn403Response() throws Exception {
        Instant activeDate = LocalDate.of(2040, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        Instant expirationDate = LocalDate.of(2050, 1, 1).atStartOfDay().toInstant(ZoneOffset.of("Z"));
        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(rsaKeyFilePath)
                                                         .keyPairId(rsaKeyPairId)
                                                         .expirationDate(expirationDate)
                                                         .activeDate(activeDate).build();
        CookiesForCustomPolicy cookies = cloudFrontUtilities.getCookiesForCustomPolicy(request);

        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(cookies.createHttpGetRequest())
                                                                               .build()).call();
        int expectedStatus = 403;
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedUrlWithCustomPolicy_shouldAllowQueryParametersWhenUsingWildcard(KeyTestCase testCase) throws Exception {
        Instant expirationDate = LocalDate.of(2050, 1, 1)
                                          .atStartOfDay()
                                          .toInstant(ZoneOffset.of("Z"));

        Instant activeDate = LocalDate.of(2022, 1, 1)
                                      .atStartOfDay()
                                      .toInstant(ZoneOffset.of("Z"));

        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(testCase.keyFilePath)
                                                         .keyPairId(testCase.keyPairId)
                                                         .resourceUrlPattern(resourceUrl + "*")
                                                         .activeDate(activeDate)
                                                         .expirationDate(expirationDate)
                                                         .build();

        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(request);

        String urlWithDynamicParam = signedUrl.url() + "&foo=bar";
        URI modifiedUri = URI.create(urlWithDynamicParam);


        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(SdkHttpRequest.builder()
                                                                                                      .encodedPath(modifiedUri.getRawPath() + "?" + modifiedUri.getRawQuery())
                                                                                                      .host(modifiedUri.getHost())
                                                                                                      .method(SdkHttpMethod.GET)
                                                                                                      .protocol("https")
                                                                                                      .build())
                                                                               .build()).call();
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedUrlWithCustomPolicy_wildCardPath(KeyTestCase testCase) throws Exception {
        String resourceUri = "https://" + domainName;
        Instant expirationDate = LocalDate.of(2050, 1, 1)
                                          .atStartOfDay()
                                          .toInstant(ZoneOffset.of("Z"));

        Instant activeDate = LocalDate.of(2022, 1, 1)
                                      .atStartOfDay()
                                      .toInstant(ZoneOffset.of("Z"));

        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(resourceUri + "/foo/specific-file")
                                                         .privateKey(testCase.keyFilePath)
                                                         .keyPairId(testCase.keyPairId)
                                                         .resourceUrlPattern(resourceUri + "/foo/*")
                                                         .activeDate(activeDate)
                                                         .expirationDate(expirationDate)
                                                         .build();

        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(request);


        URI modifiedUri = URI.create(signedUrl.url().replace("/specific-file","/other-file"));
        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(SdkHttpRequest.builder()
                                                                                                      .encodedPath(modifiedUri.getRawPath() + "?" + modifiedUri.getRawQuery())
                                                                                                      .host(modifiedUri.getHost())
                                                                                                      .method(SdkHttpMethod.GET)
                                                                                                      .protocol("https")
                                                                                                      .build())
                                                                               .build()).call();
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keyCases")
    void getSignedUrlWithCustomPolicy_wildCardPolicyResource_allowsAnyPath(KeyTestCase testCase) throws Exception {
        Instant expirationDate = LocalDate.of(2050, 1, 1)
                                          .atStartOfDay()
                                          .toInstant(ZoneOffset.of("Z"));

        Instant activeDate = LocalDate.of(2022, 1, 1)
                                      .atStartOfDay()
                                      .toInstant(ZoneOffset.of("Z"));

        CustomSignerRequest request = CustomSignerRequest.builder()
                                                         .resourceUrl(resourceUrl)
                                                         .privateKey(testCase.keyFilePath)
                                                         .keyPairId(testCase.keyPairId)
                                                         .resourceUrlPattern("*")
                                                         .activeDate(activeDate)
                                                         .expirationDate(expirationDate)
                                                         .build();

        SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCustomPolicy(request);


        URI modifiedUri = URI.create(signedUrl.url().replace("/s3ObjectKey","/foo/other-file"));
        SdkHttpClient client = Apache5HttpClient.create();
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(SdkHttpRequest.builder()
                                                                                                      .encodedPath(modifiedUri.getRawPath() + "?" + modifiedUri.getRawQuery())
                                                                                                      .host(modifiedUri.getHost())
                                                                                                      .method(SdkHttpMethod.GET)
                                                                                                      .protocol("https")
                                                                                                      .build())
                                                                               .build()).call();
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
    }

    private static void initStaticFields() throws Exception {
        initializeRsaKeyFileAndPair();
        initializeEcKeyFileAndPair();
        originAccessId = getOrCreateOriginAccessIdentity();
        keyGroupId = getOrCreateKeyGroup();
        bucket = getOrCreateBucket();

        TestDistribution distribution = getOrCreateDistribution();

        domainName = distribution.domainName;
        resourceUrl = "https://" + domainName + "/" + S3_OBJECT_KEY;
        distributionId = distribution.id;
    }

    private static void initializeRsaKeyFileAndPair() throws Exception {
        rsaKeyFile = new RandomTempFile(UUID.randomUUID() + "-key.pem", 0);
        rsaKeyFile.deleteOnExit();
        rsaKeyFilePath = rsaKeyFile.toPath();

        String privateKeyName = RESOURCE_PREFIX + "private-key";
        String publicKeyName = RESOURCE_PREFIX + "public-key";
        try {
            GetSecretValueResponse getSecretResponse = secretsManagerClient.getSecretValue(r -> r.secretId(privateKeyName));
            Files.write(rsaKeyFile.toPath(), getSecretResponse.secretBinary().asByteArray(), StandardOpenOption.TRUNCATE_EXISTING);

            Optional<PublicKeySummary> key = cloudFrontClient.listPublicKeys()
                                                             .publicKeyList()
                                                             .items()
                                                             .stream()
                                                             .filter(k -> publicKeyName.equals(k.name()))
                                                             .findAny();
            if (key.isPresent()) {
                rsaPrivateKey = SigningUtils.loadPrivateKey(rsaKeyFilePath);
                rsaKeyPairId = key.get().id();
                return;
            }
        } catch (ResourceNotFoundException e) {
            // No private key, don't bother checking for a public one.
        }

        System.out.println("Creating keys.");

        // We were missing a private or public key. Initialize them both.
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        FileWriter writer = new FileWriter(rsaKeyFile);
        writer.write("-----BEGIN PRIVATE KEY-----\n");
        writer.write(ENCODER.encodeToString(keyPair.getPrivate().getEncoded()));
        writer.write("\n-----END PRIVATE KEY-----\n");
        writer.close();

        SdkBytes keyFileBytes = SdkBytes.fromByteArray(Files.readAllBytes(rsaKeyFilePath));
        try {
            secretsManagerClient.createSecret(r -> r.name(privateKeyName)
                                                    .secretBinary(keyFileBytes));
        } catch (ResourceExistsException e) {
            secretsManagerClient.putSecretValue(r -> r.secretId(privateKeyName)
                                                      .secretBinary(keyFileBytes));
        }

        String encodedKey = "-----BEGIN PUBLIC KEY-----\n"
                            + ENCODER.encodeToString(keyPair.getPublic().getEncoded())
                            + "\n-----END PUBLIC KEY-----\n";


        CreatePublicKeyResponse publicKeyResponse =
            cloudFrontClient.createPublicKey(r -> r.publicKeyConfig(k -> k.callerReference(CALLER_REFERENCE)
                                                                          .name(publicKeyName)
                                                                          .encodedKey(encodedKey)));
        rsaPrivateKey = keyPair.getPrivate();
        rsaKeyPairId = publicKeyResponse.publicKey().id();
    }

    private static void initializeEcKeyFileAndPair() throws Exception {
        ecKeyFile = new RandomTempFile(UUID.randomUUID() + "-key.pem", 0);
        ecKeyFile.deleteOnExit();
        ecKeyFilePath = ecKeyFile.toPath();

        String privateKeyName = RESOURCE_PREFIX + "private-key-ecdsa";
        String publicKeyName = RESOURCE_PREFIX + "public-key-ecdsa";

        try {
            GetSecretValueResponse getSecretResponse = secretsManagerClient.getSecretValue(r -> r.secretId(privateKeyName));
            Files.write(ecKeyFile.toPath(), getSecretResponse.secretBinary().asByteArray(),
                        StandardOpenOption.TRUNCATE_EXISTING);

            Optional<PublicKeySummary> key = cloudFrontClient.listPublicKeys()
                                                             .publicKeyList()
                                                             .items()
                                                             .stream()
                                                             .filter(k -> publicKeyName.equals(k.name()))
                                                             .findAny();
            if (key.isPresent()) {
                ecPrivateKey = SigningUtils.loadPrivateKey(ecKeyFilePath);
                ecKeyPairId = key.get().id();
                return;
            }
        } catch (ResourceNotFoundException e) {
            // No private key, don't bother checking for a public one.
        }

        System.out.println("Creating keys.");

        // We were missing a private or public key. Initialize them both.
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = kpg.generateKeyPair();

        FileWriter writer = new FileWriter(ecKeyFile);
        writer.write("-----BEGIN PRIVATE KEY-----\n");
        writer.write(ENCODER.encodeToString(keyPair.getPrivate().getEncoded()));
        writer.write("\n-----END PRIVATE KEY-----\n");
        writer.close();

        SdkBytes keyFileBytes = SdkBytes.fromByteArray(Files.readAllBytes(ecKeyFilePath));
        try {
            secretsManagerClient.createSecret(r -> r.name(privateKeyName)
                                                    .secretBinary(keyFileBytes));
        } catch (ResourceExistsException e) {
            secretsManagerClient.putSecretValue(r -> r.secretId(privateKeyName)
                                                      .secretBinary(keyFileBytes));
        }

        String encodedKey = "-----BEGIN PUBLIC KEY-----\n"
                            + ENCODER.encodeToString(keyPair.getPublic().getEncoded())
                            + "\n-----END PUBLIC KEY-----\n";


        CreatePublicKeyResponse publicKeyResponse =
            cloudFrontClient.createPublicKey(r -> r.publicKeyConfig(k -> k.callerReference(CALLER_REFERENCE)
                                                                          .name(publicKeyName)
                                                                          .encodedKey(encodedKey)));
        ecPrivateKey = keyPair.getPrivate();
        ecKeyPairId = publicKeyResponse.publicKey().id();
    }

    private static String getOrCreateOriginAccessIdentity() {
        String originAccessIdentityComment = RESOURCE_PREFIX + "origin-access-identity";
        Optional<CloudFrontOriginAccessIdentitySummary> originAccessIdentity =
            cloudFrontClient.listCloudFrontOriginAccessIdentities()
                            .cloudFrontOriginAccessIdentityList()
                            .items()
                            .stream()
                            .filter(i -> originAccessIdentityComment.equals(i.comment()))
                            .findAny();

        if (originAccessIdentity.isPresent()) {
            return originAccessIdentity.get().id();
        }

        System.out.println("Creating origin access identity.");

        CreateCloudFrontOriginAccessIdentityResponse response =
            cloudFrontClient.createCloudFrontOriginAccessIdentity(r -> r
                .cloudFrontOriginAccessIdentityConfig(c -> c.callerReference(CALLER_REFERENCE)
                                                            .comment(originAccessIdentityComment)));
        return response.cloudFrontOriginAccessIdentity().id();
    }

    private static String getOrCreateKeyGroup() {
        String keyGroupName = RESOURCE_PREFIX + "key-group";

        Optional<KeyGroupSummary> keyGroupSummary =
            cloudFrontClient.listKeyGroups(r -> {})
                            .keyGroupList()
                            .items()
                            .stream()
                            .filter(kg -> keyGroupName.equals(kg.keyGroup()
                                                                .keyGroupConfig()
                                                                .name()))
                            .findAny();

        if (keyGroupSummary.isPresent()) {
            // ensure that both keys are present in the keyGroup
            List<String> keysInGroup = keyGroupSummary.get().keyGroup().keyGroupConfig().items();
            List<String> expectedKeys = Arrays.asList(rsaKeyPairId, ecKeyPairId);
            if (!keysInGroup.containsAll(expectedKeys)) {
                System.out.println("Updating key group to include all keys");
                GetKeyGroupResponse keyGroupResp = cloudFrontClient.getKeyGroup(r -> r.id(keyGroupSummary.get().keyGroup().id()));
                cloudFrontClient.updateKeyGroup(r -> {
                    r
                        .id(keyGroupResp.keyGroup().id())
                        .ifMatch(keyGroupResp.eTag())
                        .keyGroupConfig(KeyGroupConfig.builder().name(keyGroupName).items(rsaKeyPairId, ecKeyPairId).build());
                });

                // KeyGroups update quickly, but it takes up to 1 minute to propagate to the cache
                // and there is not any other state we can easily query to wait on.
                System.out.println("Waiting for key group update to propagate.");
                Instant expectedPropagationTime = Instant.now().plusSeconds(60);
                Waiter.run(Instant::now)
                      .until((t) -> t.isAfter(expectedPropagationTime))
                      .orFailAfter(Duration.ofMinutes(1));
            }
            return keyGroupSummary.get().keyGroup().id();
        }

        System.out.println("Creating key group.");

        return cloudFrontClient.createKeyGroup(r -> r.keyGroupConfig(c -> c.name(keyGroupName)
                                                                           .items(rsaKeyPairId, ecKeyPairId)))
                               .keyGroup()
                               .id();
    }

    private static String getOrCreateBucket() throws IOException {
        String bucketNamePrefix = RESOURCE_PREFIX + "bucket";

        Optional<Bucket> bucket = s3Client.listBuckets()
                                          .buckets()
                                          .stream()
                                          .filter(b -> b.name().startsWith(bucketNamePrefix))
                                          .findAny();

        if (bucket.isPresent()) {
            return bucket.get().name();
        }

        System.out.println("Creating bucket.");

        String newBucketName = bucketNamePrefix + "-" + System.currentTimeMillis();
        s3Client.createBucket(r -> r.bucket(newBucketName));
        s3Client.waiter().waitUntilBucketExists(r -> r.bucket(newBucketName));

        File content = new RandomTempFile("testFile", 1000L);
        File content2 = new RandomTempFile("testFile2", 500L);
        s3Client.putObject(PutObjectRequest.builder().bucket(newBucketName).key(S3_OBJECT_KEY).build(), RequestBody.fromFile(content));
        s3Client.putObject(PutObjectRequest.builder().bucket(newBucketName).key(S3_OBJECT_KEY_ON_SUB_PATH).build(), RequestBody.fromFile(content2));
        s3Client.putObject(PutObjectRequest.builder().bucket(newBucketName).key(S3_OBJECT_KEY_ON_SUB_PATH_OTHER).build(), RequestBody.fromFile(content2));


        String bucketPolicy = "{\n"
                              + "\"Version\":\"2012-10-17\",\n"
                              + "\"Id\":\"PolicyForCloudFrontPrivateContent\",\n"
                              + "\"Statement\":[\n"
                              + "{\n"
                              + "\"Effect\":\"Allow\",\n"
                              + "\"Principal\":{\n"
                              + "\"AWS\":\"arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity " + originAccessId + "\"\n"
                              + "},\n"
                              + "\"Action\":\"s3:GetObject\",\n"
                              + "\"Resource\":\"arn:aws:s3:::" + newBucketName + "/*\"\n"
                              + "}\n"
                              + "]\n"
                              + "}";

        // The origin access identity might not be ready right away, if it was just created. Retry this until
        // identity catches up.
        Waiter.run(() -> s3Client.putBucketPolicy(r -> r.bucket(newBucketName).policy(bucketPolicy)))
              .ignoringException(S3Exception.class)
              .orFailAfter(Duration.ofMinutes(2));

        return newBucketName;
    }

    private static class TestDistribution {
        private final String id;
        private final String domainName;

        private TestDistribution(String id, String domainName) {
            this.id = id;
            this.domainName = domainName;
        }
    }

    private static TestDistribution getOrCreateDistribution() throws InterruptedException {
        String distributionComment = RESOURCE_PREFIX + "distribution";

        Optional<DistributionSummary> distribution =
            cloudFrontClient.listDistributions()
                            .distributionList()
                            .items()
                            .stream()
                            .filter(d -> distributionComment.equals(d.comment()))
                            .findAny();

        if (distribution.isPresent()) {
            DistributionSummary d = distribution.get();
            return new TestDistribution(d.id(), d.domainName());
        }

        System.out.println("Creating distribution.");

        DefaultCacheBehavior defaultCacheBehavior =
            DefaultCacheBehavior.builder()
                                .forwardedValues(f -> f.queryString(false)
                                                       .cookies(c -> c.forward("none"))
                                                       .headers(h -> h.quantity(0)))
                                .minTTL(10000L)
                                .maxTTL(10000L)
                                .defaultTTL(10000L)
                                .targetOriginId("1")
                                .viewerProtocolPolicy(ViewerProtocolPolicy.ALLOW_ALL)
                                .trustedKeyGroups(g -> g.enabled(true)
                                                        .quantity(1)
                                                        .items(keyGroupId))
                                .build();

        CacheBehavior cacheBehavior =
            CacheBehavior.builder()
                         .forwardedValues(f -> f.queryString(false)
                                                .cookies(c -> c.forward("none"))
                                                .headers(h -> h.quantity(0)))
                         .minTTL(10000L)
                         .maxTTL(10000L)
                         .defaultTTL(10000L)
                         .targetOriginId("1")
                         .viewerProtocolPolicy(ViewerProtocolPolicy.ALLOW_ALL)
                         .trustedKeyGroups(g -> g.enabled(true)
                                                 .quantity(1)
                                                 .items(keyGroupId))
                         .pathPattern("*")
                         .build();

        Origin origin = Origin.builder()
                              .domainName(bucket + ".s3.amazonaws.com")
                              .id("1")
                              .s3OriginConfig(c -> c.originAccessIdentity("origin-access-identity/cloudfront/" + originAccessId))
                              .build();

        DistributionConfig distributionConfiguration =
            DistributionConfig.builder()
                              .priceClass(PriceClass.PRICE_CLASS_100)
                              .defaultCacheBehavior(defaultCacheBehavior)
                              .aliases(a -> a.quantity(0))
                              .logging(l -> l.includeCookies(false)
                                             .enabled(false)
                                             .bucket(bucket)
                                             .prefix(""))
                              .callerReference(CALLER_REFERENCE)
                              .cacheBehaviors(b -> b.quantity(1)
                                                    .items(cacheBehavior))
                              .comment(distributionComment)
                              .defaultRootObject("")
                              .enabled(true)
                              .origins(o -> o.quantity(1)
                                             .items(origin))
                              .build();

        Distribution d = cloudFrontClient.createDistribution(r -> r.distributionConfig(distributionConfiguration))
                                         .distribution();
        System.out.println("Waiting for distribution to be deployed. This takes a while, but will be reused for "
                           + "the next test run on this account.");
        cloudFrontClient.waiter().waitUntilDistributionDeployed(r -> r.id(d.id()));
        return new TestDistribution(d.id(), d.domainName());
    }
}
