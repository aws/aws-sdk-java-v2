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

package software.amazon.awssdk.auth.token.internal;

import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class LazyTokenProviderTest {
    @SuppressWarnings("unchecked")
    private final Supplier<SdkTokenProvider> credentialsConstructor = Mockito.mock(Supplier.class);

    private final SdkTokenProvider credentials = Mockito.mock(SdkTokenProvider.class);

    @BeforeEach
    public void reset() {
        Mockito.reset(credentials, credentialsConstructor);
        Mockito.when(credentialsConstructor.get()).thenReturn(credentials);
    }

    @Test
    public void creationDoesntInvokeSupplier() {
        LazyTokenProvider.create(credentialsConstructor);
        Mockito.verifyNoMoreInteractions(credentialsConstructor);
    }

    @Test
    public void resolveCredentialsInvokesSupplierExactlyOnce() {
        LazyTokenProvider credentialsProvider = LazyTokenProvider.create(credentialsConstructor);
        credentialsProvider.resolveToken();
        credentialsProvider.resolveToken();

        Mockito.verify(credentialsConstructor).get();
        Mockito.verify(credentials, Mockito.times(2)).resolveToken();
    }

    @Test
    public void delegatesClosesInitializerAndValue() {
        CloseableSupplier initializer = Mockito.mock(CloseableSupplier.class);
        CloseableCredentialsProvider value = Mockito.mock(CloseableCredentialsProvider.class);

        Mockito.when(initializer.get()).thenReturn(value);

        LazyTokenProvider.create(initializer).close();

        Mockito.verify(initializer).close();
        Mockito.verify(value).close();
    }

    @Test
    public void delegatesClosesInitializerEvenIfGetFails() {
        CloseableSupplier initializer = Mockito.mock(CloseableSupplier.class);
        Mockito.when(initializer.get()).thenThrow(new RuntimeException());

        LazyTokenProvider.create(initializer).close();

        Mockito.verify(initializer).close();
    }

    private interface CloseableSupplier extends Supplier<SdkTokenProvider>, SdkAutoCloseable {}
    private interface CloseableCredentialsProvider extends SdkAutoCloseable, SdkTokenProvider {}
}
