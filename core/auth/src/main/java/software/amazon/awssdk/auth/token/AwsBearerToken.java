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

package software.amazon.awssdk.auth.token;

import static software.amazon.awssdk.utils.StringUtils.trimToNull;

import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Provides access to the AWS token used for accessing AWS service. This token is used to securely authorize requests to AWS
 * services.
 * 
 * <p>For more details on bearer tokens, see:
 * <a href="https://oauth.net/2/bearer-tokens/">
 * https://oauth.net/2/bearer-tokens/</a></p>
 *
 * @see AwsToken
 */
@Immutable
@SdkPublicApi
public final class AwsBearerToken implements AwsToken {

    private final String token;
    private final Instant expirationTime;

    /**
     * Constructs a new token object, with the specified token string and optional expirationTime.
     *
     * @param token The token string, used to authorize the request.
     * @param expirationTime An optional field representing the time at which the token expires.
     */
    protected AwsBearerToken(String token, Instant expirationTime) {
        this(token, expirationTime, true);
    }

    private AwsBearerToken(String token, Instant expirationTime, boolean validateToken) {
        this.token = trimToNull(token);
        this.expirationTime = expirationTime;

        if (validateToken) {
            Validate.notNull(this.token, "Token cannot be blank.");
        }
    }

    /**
     * Constructs a new token object, with the specified token string and expirationTime.
     *
     * @param token The token string, used to authorize the request.
     * @param expirationTime Time at which the token expires.
     * */
    public static AwsBearerToken create(String token, Instant expirationTime) {
        return new AwsBearerToken(token, expirationTime, true);
    }


    /**
     * Constructs a new token object, with the provided token string.
     * @param token The token string, used to authorize the request.
     */
    public static AwsBearerToken create(String token) {
        return new AwsBearerToken(token, null);
    }

    /**
     * Retrieve the  token string, used to authorize the request.
     */
    @Override
    public String token() {
        return token;
    }

    /**
     * Retrieve  expirationTime representing the time at which the token expires.
     */
    @Override
    public Instant expirationTime() {
        return expirationTime;
    }

    @Override
    public String toString() {
        return ToString.builder("AwsBearerToken")
                       .add("token", token)
                       .add("expirationTime", expirationTime)
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
        AwsBearerToken that = (AwsBearerToken) o;
        return Objects.equals(token, that.token) &&
               Objects.equals(expirationTime, that.expirationTime);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(token());
        hashCode = 31 * hashCode + Objects.hashCode(expirationTime());
        return hashCode;
    }
}
