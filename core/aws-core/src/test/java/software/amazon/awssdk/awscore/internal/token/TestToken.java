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

package software.amazon.awssdk.awscore.internal.token;

import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.auth.token.credentials.SdkToken;

public class TestToken implements SdkToken {

    private final String token;
    private final Instant expirationDate;
    private final String start_url;

    public static Builder builder() {
        return new Builder();
    }

    public TestToken(Builder builder) {

        this.token = builder.token;
        this.start_url = builder.start_url;
        this.expirationDate = builder.expirationDate;
    }

    @Override
    public String token() {
        return token;
    }

    @Override
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationDate);
    }

    public static class Builder {
        private String token;
        private Instant expirationDate;
        private String start_url;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder expirationDate(Instant expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder start_url(String start_url) {
            this.start_url = start_url;
            return this;
        }

        public TestToken build() {
            return new TestToken(this);
        }
    }

}
