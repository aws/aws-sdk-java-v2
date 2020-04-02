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

package software.amazon.awssdk.http.software.amazon.awssdk.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import software.amazon.awssdk.internal.http.NoneTlsKeyManagersProvider;

public class NoneTlsKeyManagersProviderTest {
    @Test
    public void getInstance_returnsNonNull() {
        assertThat(NoneTlsKeyManagersProvider.getInstance()).isNotNull();
    }

    @Test
    public void keyManagers_returnsNull() {
        assertThat(NoneTlsKeyManagersProvider.getInstance().keyManagers()).isNull();
    }

    @Test
    public void getInstance_returnsSingletonInstance() {
        NoneTlsKeyManagersProvider provider1 = NoneTlsKeyManagersProvider.getInstance();
        NoneTlsKeyManagersProvider provider2 = NoneTlsKeyManagersProvider.getInstance();
        assertThat(provider1 == provider2).isTrue();
    }
}
