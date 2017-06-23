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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpService;

public class SdkHttpServiceProviderChainTest {

    @Test(expected = NullPointerException.class)
    public void nullProviders_ThrowsException() {
        new SdkHttpServiceProviderChain<>(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyProviders_ThrowsException() {
        new SdkHttpServiceProviderChain<>();
    }

    @Test
    public void allProvidersReturnEmpty_ReturnsEmptyOptional() {
        SdkHttpServiceProvider<SdkHttpService> delegateOne = mock(SdkHttpServiceProvider.class);
        SdkHttpServiceProvider<SdkHttpService> delegateTwo = mock(SdkHttpServiceProvider.class);
        when(delegateOne.loadService()).thenReturn(Optional.empty());
        when(delegateTwo.loadService()).thenReturn(Optional.empty());
        final Optional<SdkHttpService> actual = new SdkHttpServiceProviderChain<>(delegateOne, delegateTwo).loadService();
        assertThat(actual).isEmpty();
    }

    @Test
    public void firstProviderReturnsNonEmpty_DoesNotCallSecondProvider() {
        SdkHttpServiceProvider<SdkHttpService> delegateOne = mock(SdkHttpServiceProvider.class);
        SdkHttpServiceProvider<SdkHttpService> delegateTwo = mock(SdkHttpServiceProvider.class);
        when(delegateOne.loadService()).thenReturn(Optional.of(mock(SdkHttpService.class)));
        final Optional<SdkHttpService> actual = new SdkHttpServiceProviderChain<>(delegateOne, delegateTwo).loadService();
        assertThat(actual).isPresent();
        verify(delegateTwo, never()).loadService();
    }

}