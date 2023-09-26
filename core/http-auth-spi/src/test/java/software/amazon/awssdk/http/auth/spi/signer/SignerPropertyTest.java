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

package software.amazon.awssdk.http.auth.spi.signer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class SignerPropertyTest {

    @Test
    public void equalsHashcode() {
        EqualsVerifier.forClass(SignerProperty.class)
                      .withNonnullFields("namespace", "name")
                      .verify();
    }

    @Test
    public void namesMustBeUnique() {
        String propertyName = UUID.randomUUID().toString();

        SignerProperty.create(getClass(), propertyName);
        assertThatThrownBy(() -> SignerProperty.create(getClass(), propertyName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(getClass().getName())
            .hasMessageContaining(propertyName);
    }
}
