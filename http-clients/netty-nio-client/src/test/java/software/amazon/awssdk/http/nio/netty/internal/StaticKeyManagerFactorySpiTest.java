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
import java.util.Arrays;
import java.util.stream.IntStream;
import javax.net.ssl.KeyManager;
import org.junit.Test;

/**
 * Tests for {@link StaticKeyManagerFactorySpi}.
 */
public class StaticKeyManagerFactorySpiTest {

    @Test(expected = NullPointerException.class)
    public void nullListInConstructor_throws() {
        new StaticKeyManagerFactorySpi(null);
    }

    @Test
    public void constructorCreatesArrayCopy() {
        KeyManager[] keyManagers = IntStream.range(0,8)
                .mapToObj(i -> mock(KeyManager.class))
                .toArray(KeyManager[]::new);

        KeyManager[] arg = Arrays.copyOf(keyManagers, keyManagers.length);
        StaticKeyManagerFactorySpi spi = new StaticKeyManagerFactorySpi(arg);
        for (int i = 0; i < keyManagers.length; ++i) {
            arg[i] = null;
        }

        assertThat(spi.engineGetKeyManagers()).containsExactly(keyManagers);
    }

    @Test
    public void engineGetKeyManagers_returnsProvidedList() {
        KeyManager[] keyManagers = IntStream.range(0,8)
                .mapToObj(i -> mock(KeyManager.class))
                .toArray(KeyManager[]::new);

        StaticKeyManagerFactorySpi spi = new StaticKeyManagerFactorySpi(keyManagers);

        assertThat(spi.engineGetKeyManagers()).containsExactly(keyManagers);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void engineInit_storeAndPasswords_throws() {
        StaticKeyManagerFactorySpi staticKeyManagerFactorySpi = new StaticKeyManagerFactorySpi(new KeyManager[0]);
        staticKeyManagerFactorySpi.engineInit(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void engineInit_spec_throws() {
        StaticKeyManagerFactorySpi staticKeyManagerFactorySpi = new StaticKeyManagerFactorySpi(new KeyManager[0]);
        staticKeyManagerFactorySpi.engineInit(null);
    }
}
