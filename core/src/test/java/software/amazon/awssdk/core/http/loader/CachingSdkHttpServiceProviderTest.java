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

package software.amazon.awssdk.core.http.loader;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpService;

@RunWith(MockitoJUnitRunner.class)
public class CachingSdkHttpServiceProviderTest {

    @Mock
    private SdkHttpServiceProvider<SdkHttpService> delegate;

    private SdkHttpServiceProvider<SdkHttpService> provider;

    @Before
    public void setup() {
        provider = new CachingSdkHttpServiceProvider<>(delegate);
    }

    @Test(expected = NullPointerException.class)
    public void nullDelegate_ThrowsException() {
        new CachingSdkHttpServiceProvider<>(null);
    }

    @Test
    public void delegateReturnsEmptyOptional_DelegateCalledOnce() {
        when(delegate.loadService()).thenReturn(Optional.empty());
        provider.loadService();
        provider.loadService();
        verify(delegate, times(1)).loadService();
    }

    @Test
    public void delegateReturnsNonEmptyOptional_DelegateCalledOne() {
        when(delegate.loadService()).thenReturn(Optional.of(mock(SdkHttpService.class)));
        provider.loadService();
        provider.loadService();
        verify(delegate, times(1)).loadService();
    }

}
