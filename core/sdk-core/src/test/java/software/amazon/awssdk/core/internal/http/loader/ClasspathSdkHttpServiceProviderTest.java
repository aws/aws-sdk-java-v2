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

package software.amazon.awssdk.core.internal.http.loader;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.utils.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class ClasspathSdkHttpServiceProviderTest {

    private static final Map<String, Integer> TEST_SYNC_PRIORITY =
        ImmutableMap.<String, Integer>builder()
                    .put(HighPrioritySyncHttpService.class.getName(), 1)
                    .build();

    private static final Map<String, Integer> TEST_ASYNC_PRIORITY =
        ImmutableMap.<String, Integer>builder()
                    .put(HighPriorityAsyncHttpService.class.getName(), 1)
                    .build();

    @Mock
    private SdkServiceLoader serviceLoader;

    private SdkHttpServiceProvider<SdkHttpService> provider;

    private SdkHttpServiceProvider<SdkAsyncHttpService> asyncProvider;

    @Before
    public void setup() {
        provider = new ClasspathSdkHttpServiceProvider<>(serviceLoader,
                                                         SdkHttpService.class,
                                                         TEST_SYNC_PRIORITY);

        asyncProvider = new ClasspathSdkHttpServiceProvider<>(serviceLoader,
                                                              SdkAsyncHttpService.class,
                                                              TEST_ASYNC_PRIORITY);
    }

    @Test
    public void noImplementationsFound_ReturnsEmptyOptional() {
        when(serviceLoader.loadServices(SdkHttpService.class))
                .thenReturn(iteratorOf());
        assertThat(provider.loadService()).isEmpty();
    }

    @Test
    public void oneImplementationsFound_ReturnsFulfilledOptional() {
        when(serviceLoader.loadServices(SdkHttpService.class))
                .thenReturn(iteratorOf(mock(SdkHttpService.class)));
        assertThat(provider.loadService()).isPresent();
    }

    @Test
    public void multipleSyncImplementationsFound_ReturnHighestPriority() {
        HighPrioritySyncHttpService highPrioritySyncHttpService = new HighPrioritySyncHttpService();
        SdkHttpService mock = mock(SdkHttpService.class);

        when(serviceLoader.loadServices(SdkHttpService.class))
                .thenReturn(iteratorOf(mock, highPrioritySyncHttpService));
        assertThat(provider.loadService()).contains(highPrioritySyncHttpService);

        SdkHttpService mock1 = mock(SdkHttpService.class);
        SdkHttpService mock2 = mock(SdkHttpService.class);
        when(serviceLoader.loadServices(SdkHttpService.class))
            .thenReturn(iteratorOf(mock1, mock2));
        assertThat(provider.loadService()).contains(mock1);
    }

    @Test
    public void multipleAsyncImplementationsFound_ReturnHighestPriority() {
        HighPriorityAsyncHttpService highPriorityAsyncHttpService = new HighPriorityAsyncHttpService();
        SdkAsyncHttpService mock = mock(SdkAsyncHttpService.class);

        when(serviceLoader.loadServices(SdkAsyncHttpService.class))
            .thenReturn(iteratorOf(mock, highPriorityAsyncHttpService));
        assertThat(asyncProvider.loadService()).contains(highPriorityAsyncHttpService);

        SdkAsyncHttpService mock1 = mock(SdkAsyncHttpService.class);
        SdkAsyncHttpService mock2 = mock(SdkAsyncHttpService.class);
        when(serviceLoader.loadServices(SdkAsyncHttpService.class))
            .thenReturn(iteratorOf(mock1, mock2));
        assertThat(asyncProvider.loadService()).contains(mock1);
    }

    @SafeVarargs
    private final <T> Iterator<T> iteratorOf(T... items) {
        return Arrays.asList(items).iterator();
    }

    /**
     * Test-local sync HTTP service whose class name is registered in {@link #TEST_SYNC_PRIORITY}. The provider only ever
     * inspects the class name, so the factory method is never invoked.
     */
    private static final class HighPrioritySyncHttpService implements SdkHttpService {
        @Override
        public SdkHttpClient.Builder createHttpClientBuilder() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Test-local async HTTP service whose class name is registered in {@link #TEST_ASYNC_PRIORITY}. The provider only ever
     * inspects the class name, so the factory method is never invoked.
     */
    private static final class HighPriorityAsyncHttpService implements SdkAsyncHttpService {
        @Override
        public SdkAsyncHttpClient.Builder createAsyncHttpClientFactory() {
            throw new UnsupportedOperationException();
        }
    }
}
