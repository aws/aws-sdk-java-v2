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
import static software.amazon.awssdk.core.internal.http.loader.ClasspathSdkHttpServiceProvider.ASYNC_HTTP_SERVICES_PRIORITY;
import static software.amazon.awssdk.core.internal.http.loader.ClasspathSdkHttpServiceProvider.SYNC_HTTP_SERVICES_PRIORITY;

import java.util.Arrays;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.apache.ApacheSdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.http.nio.netty.NettySdkAsyncHttpService;

@RunWith(MockitoJUnitRunner.class)
public class ClasspathSdkHttpServiceProviderTest {

    @Mock
    private SdkServiceLoader serviceLoader;

    private SdkHttpServiceProvider<SdkHttpService> provider;

    private SdkHttpServiceProvider<SdkAsyncHttpService> asyncProvider;

    @Before
    public void setup() {
        provider = new ClasspathSdkHttpServiceProvider<>(serviceLoader,
                                                         SdkHttpService.class,
                                                         SYNC_HTTP_SERVICES_PRIORITY);

        asyncProvider = new ClasspathSdkHttpServiceProvider<>(serviceLoader,
                                                              SdkAsyncHttpService.class,
                                                              ASYNC_HTTP_SERVICES_PRIORITY);
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
        ApacheSdkHttpService apacheSdkHttpService = new ApacheSdkHttpService();
        SdkHttpService mock = mock(SdkHttpService.class);

        when(serviceLoader.loadServices(SdkHttpService.class))
                .thenReturn(iteratorOf(apacheSdkHttpService, mock));
        assertThat(provider.loadService()).contains(apacheSdkHttpService);

        SdkHttpService mock1 = mock(SdkHttpService.class);
        SdkHttpService mock2 = mock(SdkHttpService.class);
        when(serviceLoader.loadServices(SdkHttpService.class))
            .thenReturn(iteratorOf(mock1, mock2));
        assertThat(provider.loadService()).contains(mock1);
    }

    @Test
    public void multipleAsyncImplementationsFound_ReturnHighestPriority() {
        NettySdkAsyncHttpService netty = new NettySdkAsyncHttpService();
        SdkAsyncHttpService mock = mock(SdkAsyncHttpService.class);

        when(serviceLoader.loadServices(SdkAsyncHttpService.class))
            .thenReturn(iteratorOf(netty, mock));
        assertThat(asyncProvider.loadService()).contains(netty);

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
}
