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

package software.amazon.awssdk.awscore.endpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link AccountIdEndpointMode#value()} is what generated endpoint resolvers pass to the rules engine as the
 * {@code AWS::Auth::AccountIdEndpointMode} built-in, so these tests pin the wire form itself rather than any caller.
 */
class AccountIdEndpointModeTest {
    private final Locale defaultLocale = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    void value_returnsTheLowercaseWireForm() {
        assertThat(AccountIdEndpointMode.PREFERRED.value()).isEqualTo("preferred");
        assertThat(AccountIdEndpointMode.DISABLED.value()).isEqualTo("disabled");
        assertThat(AccountIdEndpointMode.REQUIRED.value()).isEqualTo("required");
    }

    /**
     * The value is a compile-time literal, so the same reference comes back every time. That is what removes the
     * per-request allocation the generated resolver used to pay for {@code name().toLowerCase()}.
     */
    @ParameterizedTest
    @EnumSource(AccountIdEndpointMode.class)
    void value_returnsTheSameReferenceEachCall(AccountIdEndpointMode mode) {
        assertThat(mode.value()).isSameAs(mode.value());
    }

    /**
     * {@code fromValue} and {@code value} must describe the same mapping in both directions, otherwise a
     * value the SDK emits is one it cannot read back.
     */
    @ParameterizedTest
    @EnumSource(AccountIdEndpointMode.class)
    void fromValue_roundTripsValue(AccountIdEndpointMode mode) {
        assertThat(AccountIdEndpointMode.fromValue(mode.value())).isSameAs(mode);
    }

    /**
     * The predecessor of this method was {@code name().toLowerCase()}, which uses the default locale. Under a Turkish
     * locale that produces a dotless i (U+0131), so {@code DISABLED} became {@code dısabled} and was handed to the rules
     * engine in that form.
     */
    @ParameterizedTest
    @EnumSource(AccountIdEndpointMode.class)
    void value_isIndependentOfTheDefaultLocale(AccountIdEndpointMode mode) {
        String underDefaultLocale = mode.value();

        Locale.setDefault(new Locale("tr", "TR"));

        assertThat(mode.value()).isEqualTo(underDefaultLocale);
        assertThat(mode.value()).doesNotContain("\u0131");
    }

    @Test
    void fromValue_isCaseInsensitive() {
        assertThat(AccountIdEndpointMode.fromValue("PREFERRED")).isSameAs(AccountIdEndpointMode.PREFERRED);
        assertThat(AccountIdEndpointMode.fromValue("Disabled")).isSameAs(AccountIdEndpointMode.DISABLED);
    }

    @Test
    void fromValue_nullReturnsNull() {
        assertThat(AccountIdEndpointMode.fromValue(null)).isNull();
    }

    @Test
    void fromValue_unrecognizedThrows() {
        assertThatThrownBy(() -> AccountIdEndpointMode.fromValue("nonsense"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nonsense");
    }
}
