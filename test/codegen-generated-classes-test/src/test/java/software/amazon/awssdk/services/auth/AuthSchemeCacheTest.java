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

package software.amazon.awssdk.services.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.multiauth.MultiauthClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.auth.scheme.ProtocolRestJsonAuthSchemeParams;
import software.amazon.awssdk.services.protocolrestjson.auth.scheme.ProtocolRestJsonAuthSchemeProvider;

class AuthSchemeCacheTest {

    @Mock
    private SdkHttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockHttpClient.clientName()).thenReturn("MockHttpClient");
        when(mockHttpClient.prepareRequest(any()))
            .thenThrow(new RuntimeException("stop"));
    }

    @Test
    void customClientProvider_isNotCached() {
        AtomicInteger resolveCount = new AtomicInteger(0);

        ProtocolRestJsonAuthSchemeProvider customProvider = params -> {
            resolveCount.incrementAndGet();
            return Collections.singletonList(
                AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build());
        };

        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .authSchemeProvider(customProvider)
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(resolveCount.get()).isEqualTo(1);

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(resolveCount.get()).isEqualTo(2);

        client.close();
    }

    @Test
    void perRequestOverride_usesOverrideProvider() {
        AtomicInteger requestProviderCount = new AtomicInteger(0);

        ProtocolRestJsonAuthSchemeProvider requestProvider = params -> {
            requestProviderCount.incrementAndGet();
            return Collections.singletonList(
                AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build());
        };

        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(requestProviderCount.get()).isEqualTo(0);

        assertThatThrownBy(() -> client.allTypes(r -> r.overrideConfiguration(
            c -> c.authSchemeProvider(requestProvider)
        ))).hasMessageContaining("stop");
        assertThat(requestProviderCount.get()).isEqualTo(1);

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(requestProviderCount.get()).isEqualTo(1);

        client.close();
    }

    @Test
    void defaultProvider_multipleOperationsSucceed() {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThatThrownBy(() -> client.deleteOperation(r -> {})).hasMessageContaining("stop");
        assertThatThrownBy(() -> client.deleteOperation(r -> {})).hasMessageContaining("stop");

        client.close();
    }

    @Test
    void defaultProvider_returnsUnmodifiableList() {
        List<AuthSchemeOption> options = ProtocolRestJsonAuthSchemeProvider.defaultProvider().resolveAuthScheme(
            ProtocolRestJsonAuthSchemeParams.builder().operation("AllTypes").region(Region.US_WEST_2).build());

        assertThat(options).isNotEmpty();
        assertThatThrownBy(() -> options.add(AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defaultProvider_cachesUnmodifiableListAndReusesInstance() throws Exception {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");

        Map<String, List<AuthSchemeOption>> cache = authSchemeCache(client);
        assertThat(cache).isNotEmpty();
        List<AuthSchemeOption> cached = cache.values().iterator().next();

        assertThatThrownBy(() -> cached.add(AuthSchemeOption.builder().schemeId(AwsV4AuthScheme.SCHEME_ID).build()))
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(cache.values().iterator().next()).isSameAs(cached);

        client.close();
    }

    @Test
    void perOpAuthService_differentOperationsSeparateCacheEntries() throws Exception {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        Map<String, List<AuthSchemeOption>> cache = authSchemeCache(client);
        assertThat(cache).hasSize(1);

        assertThatThrownBy(() -> client.deleteOperation(r -> {})).hasMessageContaining("stop");
        assertThat(cache).hasSize(2);

        client.close();
    }


    @Test
    void regionChange_causesCacheMiss() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        ExecutionInterceptor regionSwitcher = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                if (callCount.incrementAndGet() > 1) {
                    executionAttributes.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.EU_WEST_1);
                }
            }
        };

        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .overrideConfiguration(c -> c.addExecutionInterceptor(regionSwitcher))
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        Map<String, List<AuthSchemeOption>> cache = authSchemeCache(client);
        assertThat(cache).hasSize(1);

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(cache).hasSize(2);

        client.close();
    }

    @Test
    void cacheHit_returnsSameInstanceOnRepeatedCalls() throws Exception {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .build();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        Map<String, List<AuthSchemeOption>> cache = authSchemeCache(client);
        assertThat(cache).hasSize(1);
        List<AuthSchemeOption> firstResult = cache.values().iterator().next();

        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThatThrownBy(() -> client.allTypes(r -> {})).hasMessageContaining("stop");
        assertThat(cache).hasSize(1);
        assertThat(cache.values().iterator().next()).isSameAs(firstResult);

        client.close();
    }

    @Test
    void regionSetChange_causesCacheMiss() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        ExecutionInterceptor regionSetSwitcher = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                if (callCount.incrementAndGet() > 1) {
                    executionAttributes.putAttribute(AwsExecutionAttribute.AWS_SIGV4A_SIGNING_REGION_SET,
                        Collections.singleton("eu-west-1"));
                }
            }
        };

        MultiauthClient client = MultiauthClient.builder()
            .httpClient(mockHttpClient)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .overrideConfiguration(c -> c.addExecutionInterceptor(regionSetSwitcher))
            .build();

        assertThatThrownBy(() -> client.multiAuthWithOnlySigv4a(r -> {})).hasMessageContaining("stop");
        Map<String, List<AuthSchemeOption>> cache = authSchemeCache(client);
        assertThat(cache).hasSize(1);

        assertThatThrownBy(() -> client.multiAuthWithOnlySigv4a(r -> {})).hasMessageContaining("stop");
        assertThat(cache).hasSize(2);

        client.close();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<AuthSchemeOption>> authSchemeCache(Object client) throws Exception {
        Field field = client.getClass().getDeclaredField("authSchemeCache");
        field.setAccessible(true);
        return (Map<String, List<AuthSchemeOption>>) field.get(client);
    }
}
