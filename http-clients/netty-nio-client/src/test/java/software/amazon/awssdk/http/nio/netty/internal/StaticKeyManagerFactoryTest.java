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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import java.util.stream.IntStream;
import javax.net.ssl.KeyManager;
import org.junit.Test;

/**
 * Tests for {@link StaticKeyManagerFactory}.
 */
public class StaticKeyManagerFactoryTest {

    @Test
    public void createReturnFactoryWithCorrectKeyManagers() {
        KeyManager[] keyManagers = IntStream.range(0,8)
                .mapToObj(i -> mock(KeyManager.class))
                .toArray(KeyManager[]::new);

        StaticKeyManagerFactory staticKeyManagerFactory = StaticKeyManagerFactory.create(keyManagers);

        assertThat(staticKeyManagerFactory.getKeyManagers())
                .containsExactly(keyManagers);
    }

}
