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

package software.amazon.awssdk.services;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.CompressionConfiguration;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonServiceClientConfiguration;
import software.amazon.awssdk.services.protocolrestjson.auth.scheme.ProtocolRestJsonAuthSchemeProvider;
import software.amazon.awssdk.services.protocolrestjson.endpoints.ProtocolRestJsonEndpointProvider;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Verify that configuration changes made by plugins are reflected in the SDK client configuration used by the request, and
 * that the plugin can see all SDK configuration options.
 */
public class SdkPluginTest {
    private static final AwsCredentialsProvider DEFAULT_CREDENTIALS = () -> AwsBasicCredentials.create("akid", "skid");

    public static Stream<TestCase<?>> testCases() {
        Map<String, AuthScheme<?>> defaultAuthSchemes =
            ImmutableMap.of(AwsV4AuthScheme.SCHEME_ID, AwsV4AuthScheme.create(),
                            NoAuthAuthScheme.SCHEME_ID, NoAuthAuthScheme.create());
        Map<String, AuthScheme<?>> nonDefaultAuthSchemes = new HashMap<>(defaultAuthSchemes);
        nonDefaultAuthSchemes.put(CustomAuthScheme.SCHEME_ID, new CustomAuthScheme());

        ScheduledExecutorService mockScheduledExecutor = mock(ScheduledExecutorService.class);
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);

        String profileFileContent =
            "[default]\n"
            + ProfileProperty.AWS_ACCESS_KEY_ID + " = akid-from-profile\n"
            + ProfileProperty.AWS_SECRET_ACCESS_KEY + " = skid-from-profile\n"
            + ProfileProperty.AWS_SESSION_TOKEN + " = stok-from-profile\n"
            + ProfileProperty.DEFAULTS_MODE + " = standard\n"
            + ProfileProperty.USE_DUALSTACK_ENDPOINT + " = true\n"
            + ProfileProperty.USE_FIPS_ENDPOINT + " = true";

        ProfileFile nonDefaultProfileFile =
            ProfileFile.builder()
                       .type(ProfileFile.Type.CONFIGURATION)
                       .content(new StringInputStream(profileFileContent))
                       .build();

        return Stream.of(
            new TestCase<URI>("endpointOverride")
                .nonDefaultValue(URI.create("https://example.aws"))
                .clientSetter(SdkClientBuilder::endpointOverride)
                .pluginSetter(ProtocolRestJsonServiceClientConfiguration.Builder::endpointOverride)
                .pluginValidator((c, v) -> assertThat(v).isEqualTo(c.endpointOverride()))
                .beforeTransmissionValidator((r, a, v) -> {
                    assertThat(v).isEqualTo(removePathAndQueryString(r.httpRequest().getUri()));
                }),
            new TestCase<ProtocolRestJsonEndpointProvider>("endpointProvider")
                .defaultClientValue(ProtocolRestJsonEndpointProvider.defaultProvider())
                .defaultRequestValue(ProtocolRestJsonEndpointProvider.defaultProvider())
                .nonDefaultValue(a -> CompletableFuture.completedFuture(Endpoint.builder()
                                                                                .url(URI.create("https://example.aws"))
                                                                                .build()))
                .clientSetter(ProtocolRestJsonClientBuilder::endpointProvider)
                .requestSetter(RequestOverrideConfiguration.Builder::endpointProvider)
                .pluginSetter(ProtocolRestJsonServiceClientConfiguration.Builder::endpointProvider)
                .pluginValidator((c, v) -> assertThat(c.endpointProvider()).isEqualTo(v))
                .beforeTransmissionValidator((r, a, v) -> {
                    assertThat(removePathAndQueryString(r.httpRequest().getUri()))
                        .isEqualTo(v.resolveEndpoint(x -> {}).join().url());
                }),
            new TestCase<Map<String, AuthScheme<?>>>("authSchemes")
                .defaultClientValue(defaultAuthSchemes)
                .defaultRequestValue(defaultAuthSchemes)
                .nonDefaultValue(nonDefaultAuthSchemes)
                .clientSetter((b, v) -> v.forEach((x, scheme) -> b.putAuthScheme(scheme)))
                .pluginSetter((b, v) -> v.forEach((x, scheme) -> b.putAuthScheme(scheme)))
                .pluginValidator((c, v) -> v.forEach((id, s) -> assertThat(c.authSchemes()).containsEntry(id, s)))
                .beforeTransmissionValidator((r, a, v) -> v.forEach((id, s) -> {
                    assertThat(a.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES)).containsEntry(id, s);
                })),
            new TestCase<Region>("region")
                .defaultClientValue(Region.US_WEST_2)
                .defaultRequestValue(Region.US_WEST_2)
                .nonDefaultValue(Region.US_EAST_1)
                .clientSetter(AwsClientBuilder::region)
                .pluginSetter(ProtocolRestJsonServiceClientConfiguration.Builder::region)
                .pluginValidator((c, v) -> assertThat(c.region()).isEqualTo(v))
                .beforeTransmissionValidator((r, a, v) -> {
                    assertThat(r.httpRequest()
                                .firstMatchingHeader("Authorization")).get()
                                                                      .asString()
                                                                      .contains(v.id());
                    assertThat(r.httpRequest().getUri().getHost()).contains(v.id());
                }),
            new TestCase<AwsCredentialsProvider>("credentialsProvider")
                .defaultClientValue(DEFAULT_CREDENTIALS)
                .defaultRequestValue(DEFAULT_CREDENTIALS)
                .nonDefaultValue(DEFAULT_CREDENTIALS::resolveCredentials)
                .clientSetter(AwsClientBuilder::credentialsProvider)
                .requestSetter(AwsRequestOverrideConfiguration.Builder::credentialsProvider)
                .pluginSetter(ProtocolRestJsonServiceClientConfiguration.Builder::credentialsProvider)
                .pluginValidator((c, v) -> assertThat(c.credentialsProvider()).isEqualTo(v))
                .beforeTransmissionValidator((r, a, v) -> {
                    assertThat(r.httpRequest()
                                .firstMatchingHeader("Authorization")).get()
                                                                      .asString()
                                                                      .contains(v.resolveCredentials().accessKeyId());
                }),
            new TestCase<ProtocolRestJsonAuthSchemeProvider>("authSchemeProvider")
                .defaultClientValue(ProtocolRestJsonAuthSchemeProvider.defaultProvider())
                .defaultRequestValue(ProtocolRestJsonAuthSchemeProvider.defaultProvider())
                .nonDefaultValue(p -> singletonList(AuthSchemeOption.builder().schemeId(NoAuthAuthScheme.SCHEME_ID).build()))
                .clientSetter(ProtocolRestJsonClientBuilder::authSchemeProvider)
                .pluginSetter(ProtocolRestJsonServiceClientConfiguration.Builder::authSchemeProvider)
                .pluginValidator((c, v) -> assertThat(c.authSchemeProvider()).isEqualTo(v))
                .beforeTransmissionValidator((r, a, v) -> {
                    assertThat(a.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                                .authSchemeOption()
                                .schemeId()).isEqualTo(NoAuthAuthScheme.SCHEME_ID);
                    assertThat(r.httpRequest().firstMatchingHeader("Authorization")).isNotPresent();
                }),
            new TestCase<Map<String, List<String>>>("override.headers")
                .defaultClientValue(emptyMap())
                .defaultRequestValue(emptyMap())
                .nonDefaultValue(singletonMap("foo", singletonList("bar")))
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.headers(v)))
                .requestSetter(AwsRequestOverrideConfiguration.Builder::headers)
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.headers(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().headers()).isEqualTo(v))
                .beforeTransmissionValidator((r, a, v) -> {
                    v.forEach((key, value) -> assertThat(r.httpRequest().headers().get(key)).isEqualTo(value));
                }),
            new TestCase<RetryPolicy>("override.retryPolicy")
                .defaultClientValue(null) // TODO: Plugins should see default values
                .defaultRequestValue(null)
                .nonDefaultValue(RetryPolicy.builder(RetryMode.STANDARD).numRetries(1).build())
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.retryPolicy(v)))
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.retryPolicy(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().retryPolicy().orElse(null)).isEqualTo(v))
                .beforeTransmissionValidator((r, a, v) -> {
                    assertThat(r.httpRequest().firstMatchingHeader("amz-sdk-request"))
                        .hasValue("attempt=1; max=" + (v.numRetries() + 1));
                }),
            new TestCase<List<ExecutionInterceptor>>("override.executionInterceptors")
                .defaultClientValue(emptyList())
                .defaultRequestValue(emptyList())
                .nonDefaultValue(singletonList(new FlagSettingInterceptor()))
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.executionInterceptors(v)))
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.executionInterceptors(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().executionInterceptors()).isEqualTo(v))
                .beforeTransmissionValidator((r, a, v) -> {
                    if (v.stream().anyMatch(i -> i instanceof FlagSettingInterceptor)) {
                        assertThat(a.getAttribute(FlagSettingInterceptor.FLAG)).isEqualTo(true);
                    } else {
                        assertThat(a.getAttribute(FlagSettingInterceptor.FLAG)).isNull();
                    }
                }),
            new TestCase<ScheduledExecutorService>("override.scheduledExecutorService")
                .defaultClientValue(null)
                .defaultRequestValue(null)
                .nonDefaultValue(mockScheduledExecutor)
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.scheduledExecutorService(v)))
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.scheduledExecutorService(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().scheduledExecutorService().orElse(null)).isEqualTo(v))
                .clientConfigurationValidator((c, v) -> {
                    ScheduledExecutorService configuredService = c.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
                    if (mockScheduledExecutor.equals(v)) {
                        // The SDK should decorate the non-default-value. Ensure that's what happened.
                        Runnable runnable = () -> {};
                        configuredService.submit(runnable);
                        assertThat(v).isEqualTo(mockScheduledExecutor);
                        Mockito.verify(v, times(1)).submit(eq(runnable));
                    } else {
                        // This isn't our configured mock, so it shouldn't decorate the value.
                        assertThat(configuredService).isEqualTo(v);
                    }
                }),
            new TestCase<Map<SdkAdvancedClientOption<?>, ?>>("override.advancedOptions")
                .defaultClientValue(emptyMap())
                .defaultRequestValue(emptyMap())
                .nonDefaultValue(singletonMap(SdkAdvancedClientOption.USER_AGENT_PREFIX, "foo"))
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.advancedOptions(v)))
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.advancedOptions(v))))
                .pluginValidator((c, v) -> {
                    v.forEach((o, ov) -> assertThat(c.overrideConfiguration().advancedOption(o).orElse(null)).isEqualTo(ov));
                })
                .clientConfigurationValidator((c, v) -> v.forEach((o, ov) -> assertThat(c.option(o)).isEqualTo(ov))),
            new TestCase<Duration>("override.apiCallTimeout")
                .defaultClientValue(null)
                .defaultRequestValue(null)
                .nonDefaultValue(Duration.ofSeconds(5))
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.apiCallTimeout(v)))
                .requestSetter(AwsRequestOverrideConfiguration.Builder::apiCallTimeout)
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.apiCallTimeout(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().apiCallTimeout().orElse(null)).isEqualTo(v))
                .clientConfigurationValidator((c, v) -> assertThat(c.option(SdkClientOption.API_CALL_TIMEOUT)).isEqualTo(v)),
            new TestCase<Duration>("override.apiCallAttemptTimeout")
                .defaultClientValue(null)
                .defaultRequestValue(null)
                .nonDefaultValue(Duration.ofSeconds(3))
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.apiCallAttemptTimeout(v)))
                .requestSetter(AwsRequestOverrideConfiguration.Builder::apiCallAttemptTimeout)
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.apiCallAttemptTimeout(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().apiCallAttemptTimeout().orElse(null)).isEqualTo(v))
                .clientConfigurationValidator((c, v) -> assertThat(c.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT)).isEqualTo(v)),
            new TestCase<Supplier<ProfileFile>>("override.defaultProfileFileSupplier")
                .defaultClientValue(null)
                .defaultRequestValue(null)
                .nonDefaultValue(() -> nonDefaultProfileFile)
                .clientSetter((b, v) -> {
                    if (v != null) {
                        b.credentialsProvider(null)
                         .overrideConfiguration(c -> c.defaultProfileFileSupplier(v));
                    } else {
                        // Do not override the creds if the profile file supplier is null, otherwise we won't have any creds.
                        b.overrideConfiguration(c -> c.defaultProfileFileSupplier(null));
                    }
                })
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.defaultProfileFileSupplier(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().defaultProfileFileSupplier().orElse(null)).isEqualTo(v))
                .clientConfigurationValidator((c, v) -> {
                    Supplier<ProfileFile> supplier = c.option(SdkClientOption.PROFILE_FILE_SUPPLIER);
                    assertThat(supplier).isEqualTo(v);

                    Optional<Profile> defaultProfile = supplier.get().profile("default");
                    defaultProfile.ifPresent(profile -> {
                        // TODO: The codegen'd client configuration objects that plugins get cannot set the profile file and have
                        // it affect credentials, because the credentials are cached within the configuration objects. That
                        // should be fixed.
                        // AwsCredentialsIdentity credentials = c.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER).resolveIdentity().join();
                        // profile.property(ProfileProperty.AWS_ACCESS_KEY_ID).ifPresent(akid -> {
                        //     assertThat(credentials.accessKeyId()).isEqualTo(akid);
                        // });
                        // profile.property(ProfileProperty.AWS_SECRET_ACCESS_KEY).ifPresent(skid -> {
                        //     assertThat(credentials.secretAccessKey()).isEqualTo(skid);
                        // });
                        // profile.property(ProfileProperty.AWS_SESSION_TOKEN).ifPresent(stok -> {
                        //     assertThat(credentials).asInstanceOf(InstanceOfAssertFactories.type(AwsSessionCredentialsIdentity.class))
                        //                            .extracting(AwsSessionCredentialsIdentity::sessionToken)
                        //                            .isEqualTo(stok);
                        // });
                        profile.booleanProperty(ProfileProperty.USE_FIPS_ENDPOINT).ifPresent(ufe -> {
                            assertThat(c.option(AwsClientOption.FIPS_ENDPOINT_ENABLED)).isEqualTo(ufe);
                        });
                        profile.property(ProfileProperty.DEFAULTS_MODE).ifPresent(d -> {
                            assertThat(c.option(AwsClientOption.DEFAULTS_MODE)).isEqualTo(DefaultsMode.fromValue(d));
                        });
                        profile.booleanProperty(ProfileProperty.USE_DUALSTACK_ENDPOINT).ifPresent(ude -> {
                            assertThat(c.option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED)).isEqualTo(ude);
                        });
                    });
                }),
            new TestCase<ProfileFile>("override.defaultProfileFile")
                .defaultClientValue(null)
                .defaultRequestValue(null)
                .nonDefaultValue(nonDefaultProfileFile)
                .clientSetter((b, v) -> {
                    if (v != null) {
                        b.credentialsProvider(null)
                         .overrideConfiguration(c -> c.defaultProfileFile(v));
                    } else {
                        // Do not override the creds if the profile file is null, otherwise we won't have any creds.
                        b.overrideConfiguration(c -> c.defaultProfileFile(null));
                    }
                })
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.defaultProfileFile(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().defaultProfileFile().orElse(null)).isEqualTo(v))
                .clientConfigurationValidator((c, v) -> assertThat(c.option(SdkClientOption.PROFILE_FILE)).isEqualTo(v)),
            new TestCase<String>("override.defaultProfileName")
                .defaultClientValue(null)
                .defaultRequestValue(null)
                .nonDefaultValue("some-profile")
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.defaultProfileName(v)))
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.defaultProfileName(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().defaultProfileName().orElse(null)).isEqualTo(v))
                .clientConfigurationValidator((c, v) -> {
                    // TODO: when default values are fixed, validate that the "v" profile is actually used by checking some
                    // setting (e.g. region, credentials).
                    assertThat(c.option(SdkClientOption.PROFILE_NAME)).isEqualTo(v);
                }),
            new TestCase<List<MetricPublisher>>("override.metricPublishers")
                .defaultClientValue(emptyList())
                .defaultRequestValue(emptyList())
                .nonDefaultValue(singletonList(mockMetricPublisher))
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.metricPublishers(v)))
                .requestSetter(AwsRequestOverrideConfiguration.Builder::metricPublishers)
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.metricPublishers(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().metricPublishers()).isEqualTo(v))
                .clientConfigurationValidator((c, v) -> {
                    assertThat(c.option(SdkClientOption.METRIC_PUBLISHERS)).containsAll(v);
                }),
            new TestCase<ExecutionAttributes>("override.executionAttributes")
                .defaultClientValue(new ExecutionAttributes())
                .defaultRequestValue(new ExecutionAttributes())
                .nonDefaultValue(new ExecutionAttributes().putAttribute(FlagSettingInterceptor.FLAG, true))
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.executionAttributes(v)))
                .requestSetter(AwsRequestOverrideConfiguration.Builder::executionAttributes)
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.executionAttributes(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().executionAttributes()).isEqualTo(v))
                .beforeTransmissionValidator((r, a, v) -> {
                    assertThat(a.getAttribute(FlagSettingInterceptor.FLAG)).isTrue();
                }),
            new TestCase<CompressionConfiguration>("override.compressionConfiguration")
                .defaultClientValue(null)
                .defaultRequestValue(null)
                .nonDefaultValue(CompressionConfiguration.builder()
                                                         .requestCompressionEnabled(true)
                                                         .minimumCompressionThresholdInBytes(1)
                                                         .build())
                .clientSetter((b, v) -> b.overrideConfiguration(c -> c.compressionConfiguration(v)))
                .requestSetter(AwsRequestOverrideConfiguration.Builder::compressionConfiguration)
                .pluginSetter((b, v) -> b.overrideConfiguration(b.overrideConfiguration().copy(c -> c.compressionConfiguration(v))))
                .pluginValidator((c, v) -> assertThat(c.overrideConfiguration().compressionConfiguration().orElse(null)).isEqualTo(v))
                .clientConfigurationValidator((c, v) -> assertThat(c.option(SdkClientOption.COMPRESSION_CONFIGURATION)).isEqualTo(v))
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void validateTestCaseData(TestCase<T> testCase) {
        assertThat(testCase.defaultClientValue).isNotEqualTo(testCase.nonDefaultValue);
        assertThat(testCase.defaultRequestValue).isNotEqualTo(testCase.nonDefaultValue);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void clientPluginSeesDefaultValue(TestCase<T> testCase) {
        ProtocolRestJsonClientBuilder clientBuilder = defaultClientBuilder();

        AtomicInteger timesCalled = new AtomicInteger(0);
        SdkPlugin plugin = config -> {
            ProtocolRestJsonServiceClientConfiguration.Builder conf =
                (ProtocolRestJsonServiceClientConfiguration.Builder) config;
            testCase.pluginValidator.accept(conf, testCase.defaultClientValue);
            timesCalled.incrementAndGet();
        };

        ProtocolRestJsonClient client = clientBuilder.addPlugin(plugin).build();
        // TODO: Uncomment when the default values in the test case actually match what is used
        // if (testCase.clientConfigurationValidator != null) {
        //     testCase.clientConfigurationValidator.accept(extractClientConfiguration(client), testCase.defaultClientValue);
        // }
        assertThat(timesCalled).hasValue(1);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void requestPluginSeesDefaultValue(TestCase<T> testCase) {
        ProtocolRestJsonClientBuilder clientBuilder = defaultClientBuilder();

        AtomicInteger timesCalled = new AtomicInteger(0);
        SdkPlugin plugin = config -> {
            ProtocolRestJsonServiceClientConfiguration.Builder conf =
                (ProtocolRestJsonServiceClientConfiguration.Builder) config;
            testCase.pluginValidator.accept(conf, testCase.defaultRequestValue);
            timesCalled.incrementAndGet();
        };

        ProtocolRestJsonClient client = clientBuilder.httpClient(succeedingHttpClient()).build();
        // TODO: Uncomment when the default values in the test case actually match what is used
        // if (testCase.clientConfigurationValidator != null) {
        //     testCase.clientConfigurationValidator.accept(extractClientConfiguration(client), testCase.defaultRequestValue);
        // }
        client.allTypes(r -> r.overrideConfiguration(c -> c.addPlugin(plugin)));
        assertThat(timesCalled).hasValue(1);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void clientPluginSeesCustomerClientConfiguredValue(TestCase<T> testCase) {
        ProtocolRestJsonClientBuilder clientBuilder = defaultClientBuilder();
        testCase.clientSetter.accept(clientBuilder, testCase.nonDefaultValue);

        AtomicInteger timesCalled = new AtomicInteger(0);
        SdkPlugin plugin = config -> {
            ProtocolRestJsonServiceClientConfiguration.Builder conf =
                (ProtocolRestJsonServiceClientConfiguration.Builder) config;
            testCase.pluginValidator.accept(conf, testCase.nonDefaultValue);
            timesCalled.incrementAndGet();
        };

        ProtocolRestJsonClient client = clientBuilder.addPlugin(plugin).build();

        // TODO: Uncomment when the default values in the test case actually match what is used
        // if (testCase.clientConfigurationValidator != null) {
        //     testCase.clientConfigurationValidator.accept(extractClientConfiguration(client), testCase.nonDefaultValue);
        // }

        assertThat(timesCalled).hasValue(1);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void requestPluginSeesCustomerClientConfiguredValue(TestCase<T> testCase) {
        ProtocolRestJsonClientBuilder clientBuilder = defaultClientBuilder();
        testCase.clientSetter.accept(clientBuilder, testCase.nonDefaultValue);

        AtomicInteger timesCalled = new AtomicInteger(0);
        SdkPlugin plugin = config -> {
            ProtocolRestJsonServiceClientConfiguration.Builder conf =
                (ProtocolRestJsonServiceClientConfiguration.Builder) config;
            testCase.pluginValidator.accept(conf, testCase.nonDefaultValue);
            timesCalled.incrementAndGet();
        };

        ProtocolRestJsonClient client = clientBuilder.httpClient(succeedingHttpClient()).build();
        if (testCase.clientConfigurationValidator != null) {
            testCase.clientConfigurationValidator.accept(extractClientConfiguration(client), testCase.nonDefaultValue);
        }
        client.allTypes(r -> r.overrideConfiguration(c -> c.addPlugin(plugin)));
        assertThat(timesCalled).hasValue(1);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    @Disabled("Request-level values are currently higher-priority than plugin settings.") // TODO(sra-identity-auth)
    public <T> void requestPluginSeesCustomerRequestConfiguredValue(TestCase<T> testCase) {
        if (testCase.requestSetter == null) {
            System.out.println("No request setting available.");
            return;
        }

        ProtocolRestJsonClientBuilder clientBuilder = defaultClientBuilder();

        AtomicInteger timesCalled = new AtomicInteger(0);
        SdkPlugin plugin = config -> {
            ProtocolRestJsonServiceClientConfiguration.Builder conf =
                (ProtocolRestJsonServiceClientConfiguration.Builder) config;
            testCase.pluginValidator.accept(conf, testCase.nonDefaultValue);
            timesCalled.incrementAndGet();
        };

        AwsRequestOverrideConfiguration overrideConfig =
            AwsRequestOverrideConfiguration.builder()
                                           .addPlugin(plugin)
                                           .applyMutation(c -> testCase.requestSetter.accept(c, testCase.nonDefaultValue))
                                           .build();

        ProtocolRestJsonClient client = clientBuilder.httpClient(succeedingHttpClient()).build();

        // TODO: Uncomment when the default values in the test case actually match what is used
        // if (testCase.clientConfigurationValidator != null) {
        //     testCase.clientConfigurationValidator.accept(extractClientConfiguration(client), testCase.defaultClientValue);
        // }

        client.allTypes(r -> r.overrideConfiguration(overrideConfig));
        assertThat(timesCalled).hasValue(1);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void clientPluginSetValueIsUsed(TestCase<T> testCase) {
        ProtocolRestJsonClientBuilder clientBuilder = defaultClientBuilder();
        testCase.clientSetter.accept(clientBuilder, testCase.defaultClientValue);

        AtomicInteger timesPluginCalled = new AtomicInteger(0);
        SdkPlugin plugin = config -> {
            timesPluginCalled.incrementAndGet();
            ProtocolRestJsonServiceClientConfiguration.Builder conf =
                (ProtocolRestJsonServiceClientConfiguration.Builder) config;
            testCase.pluginSetter.accept(conf, testCase.nonDefaultValue);
        };

        AtomicInteger timesInterceptorCalled = new AtomicInteger(0);
        ExecutionInterceptor validatingInterceptor = new ExecutionInterceptor() {
            @Override
            public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
                timesInterceptorCalled.getAndIncrement();
                if (testCase.beforeTransmissionValidator != null) {
                    testCase.beforeTransmissionValidator.accept(context, executionAttributes, testCase.nonDefaultValue);
                }
            }
        };

        ProtocolRestJsonClient client =
            clientBuilder.httpClient(succeedingHttpClient())
                         .addPlugin(plugin)
                         .overrideConfiguration(c -> c.addExecutionInterceptor(validatingInterceptor))
                         .build();

        if (testCase.clientConfigurationValidator != null) {
            testCase.clientConfigurationValidator.accept(extractClientConfiguration(client), testCase.nonDefaultValue);
        }

        client.allTypes();

        assertThat(timesPluginCalled).hasValue(1);
        assertThat(timesInterceptorCalled).hasValueGreaterThanOrEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void requestPluginSetValueIsUsed(TestCase<T> testCase) {
        ProtocolRestJsonClientBuilder clientBuilder = defaultClientBuilder();
        testCase.clientSetter.accept(clientBuilder, testCase.defaultClientValue);

        AtomicInteger timesPluginCalled = new AtomicInteger(0);
        SdkPlugin plugin = config -> {
            timesPluginCalled.incrementAndGet();
            ProtocolRestJsonServiceClientConfiguration.Builder conf =
                (ProtocolRestJsonServiceClientConfiguration.Builder) config;
            testCase.pluginSetter.accept(conf, testCase.nonDefaultValue);
        };

        AtomicInteger timesInterceptorCalled = new AtomicInteger(0);
        ExecutionInterceptor validatingInterceptor = new ExecutionInterceptor() {
            @Override
            public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
                timesInterceptorCalled.incrementAndGet();
                if (testCase.beforeTransmissionValidator != null) {
                    testCase.beforeTransmissionValidator.accept(context, executionAttributes, testCase.nonDefaultValue);
                }
            }
        };

        AwsRequestOverrideConfiguration requestConfig =
            AwsRequestOverrideConfiguration.builder()
                                           .addPlugin(plugin)
                                           .applyMutation(c -> {
                                               // TODO(sra-identity-auth): request-level plugins should see request-level
                                               // configuration
                                               // if (testCase.requestSetter != null) {
                                               //     testCase.requestSetter.accept(c, testCase.defaultRequestValue);
                                               // }
                                           })
                                           .build();

        ProtocolRestJsonClient client =
            clientBuilder.httpClient(succeedingHttpClient())
                         .overrideConfiguration(c -> c.addExecutionInterceptor(validatingInterceptor))
                         .build();

        // TODO: Uncomment when the default values in the test case actually match what is used
        // if (testCase.clientConfigurationValidator != null) {
        //     testCase.clientConfigurationValidator.accept(extractClientConfiguration(client), testCase.defaultClientValue);
        // }

        client.allTypes(r -> r.overrideConfiguration(requestConfig));

        assertThat(timesPluginCalled).hasValue(1);
        assertThat(timesInterceptorCalled).hasValueGreaterThanOrEqualTo(1);
    }

    private static ProtocolRestJsonClientBuilder defaultClientBuilder() {
        return ProtocolRestJsonClient.builder()
                                     .region(Region.US_WEST_2)
                                     .credentialsProvider(DEFAULT_CREDENTIALS);
    }

    private SdkClientConfiguration extractClientConfiguration(ProtocolRestJsonClient client) {
        try {
            // Naughty, but we need to be able to verify some things that can't be easily observed with unprotected means.
            Class<? extends ProtocolRestJsonClient> clientClass = client.getClass();
            Field configField = clientClass.getDeclaredField("clientConfiguration");
            configField.setAccessible(true);
            return (SdkClientConfiguration) configField.get(client);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class TestCase<T> {
        private final String configName;
        T defaultClientValue; // TODO: Can there not be a difference between client and request here?
        T defaultRequestValue;
        T nonDefaultValue;
        BiConsumer<ProtocolRestJsonClientBuilder, T> clientSetter;
        BiConsumer<AwsRequestOverrideConfiguration.Builder, T> requestSetter;
        BiConsumer<ProtocolRestJsonServiceClientConfiguration.Builder, T> pluginSetter;

        BiConsumer<ProtocolRestJsonServiceClientConfiguration.Builder, T> pluginValidator;
        BiConsumer<SdkClientConfiguration, T> clientConfigurationValidator;
        TriConsumer<Context.BeforeTransmission, ExecutionAttributes, T> beforeTransmissionValidator;

        TestCase(String configName) {
            this.configName = configName;
        }

        public TestCase<T> defaultClientValue(T defaultClientValue) {
            this.defaultClientValue = defaultClientValue;
            return this;
        }

        public TestCase<T> defaultRequestValue(T defaultRequestValue) {
            this.defaultRequestValue = defaultRequestValue;
            return this;
        }

        public TestCase<T> nonDefaultValue(T nonDefaultValue) {
            this.nonDefaultValue = nonDefaultValue;
            return this;
        }

        public TestCase<T> clientSetter(BiConsumer<ProtocolRestJsonClientBuilder, T> clientSetter) {
            this.clientSetter = clientSetter;
            return this;
        }

        public TestCase<T> requestSetter(BiConsumer<AwsRequestOverrideConfiguration.Builder, T> requestSetter) {
            this.requestSetter = requestSetter;
            return this;
        }

        public TestCase<T> pluginSetter(BiConsumer<ProtocolRestJsonServiceClientConfiguration.Builder, T> pluginSetter) {
            this.pluginSetter = pluginSetter;
            return this;
        }

        public TestCase<T> pluginValidator(BiConsumer<ProtocolRestJsonServiceClientConfiguration.Builder, T> pluginValidator) {
            this.pluginValidator = pluginValidator;
            return this;
        }

        public TestCase<T> clientConfigurationValidator(BiConsumer<SdkClientConfiguration, T> clientConfigurationValidator) {
            this.clientConfigurationValidator = clientConfigurationValidator;
            return this;
        }

        public TestCase<T> beforeTransmissionValidator(TriConsumer<Context.BeforeTransmission, ExecutionAttributes, T> beforeTransmissionValidator) {
            this.beforeTransmissionValidator = beforeTransmissionValidator;
            return this;
        }

        @Override
        public String toString() {
            return configName;
        }
    }

    private static SdkHttpClient succeedingHttpClient() {
        MockSyncHttpClient client = new MockSyncHttpClient();
        client.stubNextResponse200();
        return client;
    }

    private static URI removePathAndQueryString(URI uri) {
        String uriString = uri.toString();
        return URI.create(uriString.substring(0, uriString.indexOf('/', "https://".length())));
    }

    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    private static class CustomAuthScheme implements AuthScheme<NoAuthAuthScheme.AnonymousIdentity> {
        private static final String SCHEME_ID = "foo";
        private static final AuthScheme<NoAuthAuthScheme.AnonymousIdentity> DELEGATE = NoAuthAuthScheme.create();

        @Override
        public String schemeId() {
            return SCHEME_ID;
        }

        @Override
        public IdentityProvider<NoAuthAuthScheme.AnonymousIdentity> identityProvider(IdentityProviders providers) {
            return DELEGATE.identityProvider(providers);
        }

        @Override
        public HttpSigner<NoAuthAuthScheme.AnonymousIdentity> signer() {
            return DELEGATE.signer();
        }
    }

    private static class FlagSettingInterceptor implements ExecutionInterceptor {
        private static final ExecutionAttribute<Boolean> FLAG = new ExecutionAttribute<>("InterceptorAdded");

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            executionAttributes.putAttribute(FLAG, true);
        }
    }
}
