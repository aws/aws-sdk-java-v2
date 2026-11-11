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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Covers {@link ClientEndpointProvider#sanitizedEndpointString()}'s default implementation, which is the value generated
 * endpoint resolvers receive as the {@code SDK::Endpoint} built-in.
 *
 * <p>Every provider the SDK itself installs is a {@code StaticClientEndpointProvider}, which overrides the method with a
 * value computed once at construction. The default therefore runs only for an external implementor, and it exists to be
 * the single definition of the transformation that the overriding implementation caches. Both halves are asserted here:
 * what the transformation does, and that the two forms agree.
 */
class ClientEndpointProviderTest {
    /**
     * A provider that leaves {@code sanitizedEndpointString()} to the interface, which is what an external implementor
     * gets and what no SDK code path produces.
     */
    private static ClientEndpointProvider defaultImplementation(URI uri, boolean isEndpointOverridden) {
        return new ClientEndpointProvider() {
            @Override
            public URI clientEndpoint() {
                return uri;
            }

            @Override
            public boolean isEndpointOverridden() {
                return isEndpointOverridden;
            }
        };
    }

    @ParameterizedTest
    @CsvSource({
        // Query parameters are rejected by the rules engine's ParseURL, which is why they are stripped.
        "https://example.com/path?foo=bar,          https://example.com/path",
        "https://example.com?foo=bar,               https://example.com",
        "https://example.com/path?foo=bar&baz=qux,  https://example.com/path",
        // User-info is stripped for the same reason: it is not part of what the rules engine resolves against.
        "https://user:pass@example.com/path,        https://example.com/path",
        "https://user@example.com,                  https://example.com",
        "https://user:pass@example.com/path?foo=bar,https://example.com/path",
        // Everything else survives untouched.
        "https://example.com:8443/path,             https://example.com:8443/path",
        "http://example.com/path,                   http://example.com/path",
        "https://example.com/path#frag,             https://example.com/path#frag",
        "https://example.com/a/b/c,                 https://example.com/a/b/c"
    })
    void sanitizedEndpointString_stripsQueryAndUserInfo(String input, String expected) {
        assertThat(defaultImplementation(URI.create(input), true).sanitizedEndpointString()).isEqualTo(expected);
    }

    @Test
    void sanitizedEndpointString_returnsNullWhenNotOverridden() {
        URI uri = URI.create("https://example.com/path?foo=bar");

        assertThat(defaultImplementation(uri, false).sanitizedEndpointString()).isNull();
        assertThat(ClientEndpointProvider.create(uri, false).sanitizedEndpointString()).isNull();
    }

    /**
     * The claim that makes it safe for {@code StaticClientEndpointProvider} to compute this once at construction: the
     * cached value and the recomputed one cannot disagree, because there is one definition of the transformation.
     */
    @ParameterizedTest
    @CsvSource({
        "https://example.com/path?foo=bar",
        "https://user:pass@example.com/path?foo=bar",
        "https://example.com:8443/path",
        "https://example.com/path#frag",
        "http://example.com"
    })
    void sanitizedEndpointString_cachedFormMatchesRecomputedForm(String input) {
        URI uri = URI.create(input);

        assertThat(ClientEndpointProvider.create(uri, true).sanitizedEndpointString())
            .isEqualTo(defaultImplementation(uri, true).sanitizedEndpointString());
    }

    @Test
    void sanitizedEndpointString_overridingImplementationReturnsAStableReference() {
        ClientEndpointProvider provider = ClientEndpointProvider.forEndpointOverride(
            URI.create("https://example.com/path?foo=bar"));

        assertThat(provider.sanitizedEndpointString()).isSameAs(provider.sanitizedEndpointString());
    }
}
