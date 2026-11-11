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

package software.amazon.awssdk.services.kinesis;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;

public class KinesisClientCanOverrideDefaultRetryPolicyTest {
    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();
    private WireMockServer wireMock = new WireMockServer(0);

    @Test
    public void kinesisRetryPolicyIsUsedWhenNotOverridden() {
        URI endpointOverride = URI.create("http://localhost:" + wireMock.port());
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

        KinesisAsyncClient client = KinesisAsyncClient.builder()
                                                      .region(Region.US_EAST_1)
                                                      .credentialsProvider(credentialsProvider)
                                                      .endpointOverride(endpointOverride)
                                                      .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        // Standard retry strategy does not retry on subscribeToShard
        verifyRequestCount(1);
    }

    @Test
    public void retryStrategyClientBuilderOverrideIsUsed() {
        URI endpointOverride = URI.create("http://localhost:" + wireMock.port());
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

        KinesisAsyncClient client = KinesisAsyncClient.builder()
                                                      .region(Region.US_EAST_1)
                                                      .credentialsProvider(credentialsProvider)
                                                      .overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD))
                                                      .endpointOverride(endpointOverride)
                                                      .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        // Standard retry strategy does retry on subscribeToShard
        verifyRequestCount(3);
    }

    @Test
    public void retryStrategyPluginOverrideIsUsed() {
        URI endpointOverride = URI.create("http://localhost:" + wireMock.port());
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

        KinesisAsyncClient client = KinesisAsyncClient.builder()
                                                      .region(Region.US_EAST_1)
                                                      .credentialsProvider(credentialsProvider)
                                                      .endpointOverride(endpointOverride)
                                                      .addPlugin(c -> c.overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD)))
                                                      .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        // Standard retry strategy does retry on subscribeToShard
        verifyRequestCount(3);
    }

    @Test
    public void retryStrategyRequestPluginOverrideIsUsed() {
        URI endpointOverride = URI.create("http://localhost:" + wireMock.port());
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

        KinesisAsyncClient client = KinesisAsyncClient.builder()
                                                      .region(Region.US_EAST_1)
                                                      .credentialsProvider(credentialsProvider)
                                                      .endpointOverride(endpointOverride)
                                                      .build();
        SdkPlugin plugin = c -> c.overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD));
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client, plugin));
        // Standard retry strategy does retry on subscribeToShard
        verifyRequestCount(3);
    }

    @Test
    public void defaultRetryStrategyRetriesOtherOperations() {
        KinesisAsyncClient client = clientBuilder().build();
        assertThrows(ExecutionException.class, () -> client.listStreams().get(10, TimeUnit.SECONDS));
        // The default STANDARD retry strategy makes three attempts
        verifyRequestCount(3);
    }

    @Test
    public void legacyRetryModeFromProfileDoesNotRetrySubscribeToShard() {
        KinesisAsyncClient client = clientBuilder().overrideConfiguration(o -> o.defaultProfileFile(legacyProfile())
                                                                                .defaultProfileName("default"))
                                                   .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        verifyRequestCount(1);
    }

    @Test
    public void legacyRetryModeFromProfileRetriesOtherOperations() {
        KinesisAsyncClient client = clientBuilder().overrideConfiguration(o -> o.defaultProfileFile(legacyProfile())
                                                                                .defaultProfileName("default"))
                                                   .build();
        assertThrows(ExecutionException.class, () -> client.listStreams().get(10, TimeUnit.SECONDS));
        // The LEGACY retry policy makes four attempts
        verifyRequestCount(4);
    }

    @Test
    public void adaptiveRetryModeFromProfileDoesNotRetrySubscribeToShard() {
        KinesisAsyncClient client = clientWithProfile("retry_mode = adaptive");
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        verifyRequestCount(1);
    }

    @Test
    public void adaptiveRetryModeFromProfileRetriesOtherOperations() {
        KinesisAsyncClient client = clientWithProfile("retry_mode = adaptive");
        assertThrows(ExecutionException.class, () -> client.listStreams().get(10, TimeUnit.SECONDS));
        // The default ADAPTIVE_V2 retry strategy makes three attempts
        verifyRequestCount(3);
    }

    @Test
    public void retryModeFromPluginUsesMaxAttemptsFromClientProfile() {
        KinesisAsyncClient client =
            clientBuilder().overrideConfiguration(o -> o.defaultProfileFile(profile("max_attempts = 5"))
                                                        .defaultProfileName("default"))
                           .addPlugin(c -> c.overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD)))
                           .build();
        assertThrows(ExecutionException.class, () -> client.listStreams().get(10, TimeUnit.SECONDS));
        verifyRequestCount(5);
    }

    @Test
    public void maxAttemptsFromClientProfileIsUsed() {
        KinesisAsyncClient client = clientWithProfile("max_attempts = 5");
        assertThrows(ExecutionException.class, () -> client.listStreams().get(10, TimeUnit.SECONDS));
        verifyRequestCount(5);
    }

    private KinesisAsyncClientBuilder clientBuilder() {
        return KinesisAsyncClient.builder()
                                 .region(Region.US_EAST_1)
                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                 .endpointOverride(URI.create("http://localhost:" + wireMock.port()));
    }

    private KinesisAsyncClient clientWithProfile(String property) {
        return clientBuilder().overrideConfiguration(o -> o.defaultProfileFile(profile(property))
                                                           .defaultProfileName("default"))
                              .build();
    }

    private static ProfileFile legacyProfile() {
        return profile("retry_mode = legacy");
    }

    private static ProfileFile profile(String property) {
        return ProfileFile.builder()
                          .content(new StringInputStream("[default]\n" + property))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    private void callSubscribeToShard(KinesisAsyncClient client, SdkPlugin... plugins) throws Exception {
        List<SubscribeToShardEventStream> events = new ArrayList<>();
        SubscribeToShardRequest.Builder builder = SubscribeToShardRequest.builder();
        for (SdkPlugin plugin : plugins) {
            builder.overrideConfiguration(o -> o.addPlugin(plugin));
        }
        client.subscribeToShard(builder.build(),
                                SubscribeToShardResponseHandler.builder()
                                                               .subscriber(events::add)
                                                               .build())
              .get(10, TimeUnit.SECONDS);
    }

    private void verifyRequestCount(int count) {
        wireMock.verify(count, anyRequestedFor(anyUrl()));
    }

    @BeforeEach
    private void beforeEach() {
        // Keep the host's retry settings and profile files from changing the expected attempt counts
        clearRetrySettings();
        environmentVariableHelper.set(ProfileFileSystemSetting.AWS_CONFIG_FILE, "does-not-exist");
        environmentVariableHelper.set(ProfileFileSystemSetting.AWS_SHARED_CREDENTIALS_FILE, "does-not-exist");
        wireMock.start();
        wireMock.stubFor(post(anyUrl())
                             .willReturn(
                                 aResponse()
                                     .withStatus(500)));

    }

    @AfterEach
    private void afterEach() {
        wireMock.stop();
        environmentVariableHelper.reset();
        clearRetrySettings();
    }

    private void clearRetrySettings() {
        for (SdkSystemSetting setting : new SdkSystemSetting[] {SdkSystemSetting.AWS_RETRY_MODE,
                                                                 SdkSystemSetting.AWS_MAX_ATTEMPTS}) {
            environmentVariableHelper.remove(setting);
            System.clearProperty(setting.property());
        }
        environmentVariableHelper.remove(ProfileFileSystemSetting.AWS_PROFILE);
        System.clearProperty(ProfileFileSystemSetting.AWS_PROFILE.property());
        System.clearProperty(ProfileFileSystemSetting.AWS_CONFIG_FILE.property());
        System.clearProperty(ProfileFileSystemSetting.AWS_SHARED_CREDENTIALS_FILE.property());
    }

}
