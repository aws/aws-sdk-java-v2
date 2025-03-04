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

package software.amazon.awssdk.codegen.poet.auth.scheme;

import java.util.Objects;
import software.amazon.awssdk.codegen.model.service.AuthType;

/**
 * Represents an authentication option, encapsulating attributes such as the authentication type
 * and whether the payload should be unsigned. This class provides a clean and immutable way
 * to model these attributes as separate traits, as specified in the service models.
 *
 * <p>The primary purpose of this class is to hold authentication-related attributes, such as:
 * <ul>
 *     <li><b>authType</b>: Specifies the type of authentication to be used (e.g., SigV4, SigV4A etc).</li>
 *     <li><b>unsignedPayload</b>: Indicates whether the payload should be unsigned.</li>
 * </ul>
 */
public final class AuthTrait {

    private final AuthType authType;
    private final boolean unsignedPayload;

    private AuthTrait(AuthType authType, boolean unsignedPayload) {
        this.authType = Objects.requireNonNull(authType, "authType must not be null");
        this.unsignedPayload = unsignedPayload;
    }

    public static Builder builder() {
        return new Builder();
    }

    public AuthType authType() {
        return authType;
    }

    public boolean isUnsignedPayload() {
        return unsignedPayload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthTrait that = (AuthTrait) o;
        return unsignedPayload == that.unsignedPayload && authType == that.authType;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(authType);
        hashCode = 31 * hashCode + (unsignedPayload ? 1 : 0);
        return hashCode;
    }

    @Override
    public String toString() {
        return "AuthTrait{" +
               "authType=" + authType +
               ", unsignedPayload=" + unsignedPayload +
               '}';
    }

    public static class Builder {
        private AuthType authType;
        private boolean unsignedPayload = false;

        public Builder authType(AuthType authType) {
            this.authType = authType;
            return this;
        }

        public Builder unsignedPayload(boolean unsignedPayload) {
            this.unsignedPayload = unsignedPayload;
            return this;
        }

        public AuthTrait build() {
            return new AuthTrait(authType, unsignedPayload);
        }
    }
}
