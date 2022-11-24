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

package software.amazon.awssdk.imds.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
@Immutable
public final class Token {
    private final String value;
    private final Duration ttl;
    private final Instant createdTime;

    public Token(String value, Duration ttl) {
        this.value = value;
        this.ttl = ttl;
        this.createdTime = Instant.now();
    }

    public String value() {
        return value;
    }

    public Duration ttl() {
        return ttl;
    }

    public Instant createdTime() {
        return createdTime;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(createdTime.plus(ttl));
    }

    @Override
    public String toString() {
        return ToString.builder("Token")
                       .add("value", value)
                       .add("ttl", ttl)
                       .add("createdTime", createdTime)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Token token = (Token) o;

        if (!Objects.equals(value, token.value)) {
            return false;
        }
        if (!Objects.equals(ttl, token.ttl)) {
            return false;
        }
        return Objects.equals(createdTime, token.createdTime);
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (ttl != null ? ttl.hashCode() : 0);
        result = 31 * result + (createdTime != null ? createdTime.hashCode() : 0);
        return result;
    }
}
