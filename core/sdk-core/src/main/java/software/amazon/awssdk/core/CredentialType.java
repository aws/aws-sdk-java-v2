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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Class that identifies the type of credentials typically used by the service to authorize an api request.
 */
@SdkPublicApi
public final class CredentialType {
    /**
     * Credential type that uses Bearer Token Authorization to authorize a request.
     * <p>For more details, see:
     * <a href="https://oauth.net/2/bearer-tokens/">
     * https://oauth.net/2/bearer-tokens/</a></p>
     */
    public static final CredentialType TOKEN = of("TOKEN");

    private final String value;

    private CredentialType(String value) {
        this.value = value;
    }

    /**
     * Retrieves the Credential Type for a given value.
     * @param value Teh value to get CredentialType for.
     * @return {@link CredentialType} for the given value.
     */
    public static CredentialType of(String value) {
        Validate.paramNotNull(value, "value");
        return CredentialTypeCache.put(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CredentialType that = (CredentialType) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return ToString.builder("CredentialType{" +
                                "value='" + value + '\'' +
                                '}').build();
    }

    private static class CredentialTypeCache {
        private static final ConcurrentHashMap<String, CredentialType> VALUES = new ConcurrentHashMap<>();

        private CredentialTypeCache() {
        }

        private static CredentialType put(String value) {
            return VALUES.computeIfAbsent(value, v -> new CredentialType(value));
        }
    }
}
