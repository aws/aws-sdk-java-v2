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

package software.amazon.awssdk.regions.providers;

import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LazyAwsRegionProviderTest {
    @SuppressWarnings("unchecked")
    private Supplier<AwsRegionProvider> regionProviderConstructor = Mockito.mock(Supplier.class);

    private AwsRegionProvider regionProvider = Mockito.mock(AwsRegionProvider.class);

    @Before
    public void reset() {
        Mockito.reset(regionProvider, regionProviderConstructor);
        Mockito.when(regionProviderConstructor.get()).thenReturn(regionProvider);
    }

    @Test
    public void creationDoesntInvokeSupplier() {
        new LazyAwsRegionProvider(regionProviderConstructor);
        Mockito.verifyZeroInteractions(regionProviderConstructor);
    }

    @Test
    public void getRegionInvokesSupplierExactlyOnce() {
        LazyAwsRegionProvider lazyRegionProvider = new LazyAwsRegionProvider(regionProviderConstructor);
        lazyRegionProvider.getRegion();
        lazyRegionProvider.getRegion();

        Mockito.verify(regionProviderConstructor, Mockito.times(1)).get();
        Mockito.verify(regionProvider, Mockito.times(2)).getRegion();
    }
}