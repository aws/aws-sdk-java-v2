/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.loader;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.utils.SdkSystemSetting;

@RunWith(MockitoJUnitRunner.class)
public class ClasspathSdkHttpServiceProviderTest {

    @Mock
    private SdkServiceLoader serviceLoader;

    private SdkHttpServiceProvider<SdkHttpService> provider;

    @Before
    public void setup() {
        provider = new ClasspathSdkHttpServiceProvider<>(serviceLoader,
                                                         SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL,
                                                         SdkHttpService.class);
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

    @Test(expected = SdkClientException.class)
    public void multipleImplementationsFound_ThrowsException() {
        when(serviceLoader.loadServices(SdkHttpService.class))
                .thenReturn(iteratorOf(mock(SdkHttpService.class), mock(SdkHttpService.class)));
        provider.loadService();
    }

    @SafeVarargs
    private final <T> Iterator<T> iteratorOf(T... items) {
        return Arrays.asList(items).iterator();
    }
}