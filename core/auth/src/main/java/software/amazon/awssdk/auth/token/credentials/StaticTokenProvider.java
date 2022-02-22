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

package software.amazon.awssdk.auth.token.credentials;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link SdkTokenProvider} that returns a set implementation of {@link SdkToken}.
 */
@SdkPublicApi
public final class StaticTokenProvider implements SdkTokenProvider {
    private final SdkToken token;

    private StaticTokenProvider(SdkToken token) {
        this.token = Validate.notNull(token, "Token must not be null.");
    }

    /**
     * Create a token provider that always returns the provided static token.
     */
    public static StaticTokenProvider create(SdkToken token) {
        return new StaticTokenProvider(token);
    }

    @Override
    public SdkToken resolveToken() {
        return token;
    }

    @Override
    public String toString() {
        return ToString.builder("StaticTokenProvider")
                       .add("token", token)
                       .build();
    }
}
