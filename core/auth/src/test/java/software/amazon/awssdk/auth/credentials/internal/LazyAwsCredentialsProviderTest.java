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

package software.amazon.awssdk.auth.credentials.internal;

import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class LazyAwsCredentialsProviderTest {
    @SuppressWarnings("unchecked")
    private Supplier<AwsCredentialsProvider> credentialsConstructor = Mockito.mock(Supplier.class);

    private AwsCredentialsProvider credentials = Mockito.mock(AwsCredentialsProvider.class);

    @Before
    public void reset() {
        Mockito.reset(credentials, credentialsConstructor);
        Mockito.when(credentialsConstructor.get()).thenReturn(credentials);
    }

    @Test
    public void creationDoesntInvokeSupplier() {
        LazyAwsCredentialsProvider.create(credentialsConstructor);
        Mockito.verifyZeroInteractions(credentialsConstructor);
    }

    @Test
    public void resolveCredentialsInvokesSupplierExactlyOnce() {
        LazyAwsCredentialsProvider credentialsProvider = LazyAwsCredentialsProvider.create(credentialsConstructor);
        credentialsProvider.resolveCredentials();
        credentialsProvider.resolveCredentials();

        Mockito.verify(credentialsConstructor, Mockito.times(1)).get();
        Mockito.verify(credentials, Mockito.times(2)).resolveCredentials();
    }
}
