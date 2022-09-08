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

import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.auth.token.credentials.SdkToken;

public class TestBearerToken implements SdkToken {

    private String token;
    private Instant expirationTime;

    @Override
    public String token() {
        return token;
    }

    @Override
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    private TestBearerToken(String token, Instant expirationTime) {
        this.token = token;
        this.expirationTime = expirationTime;
    }

    public static TestBearerToken create(String token, Instant expirationTime){
        return new TestBearerToken(token, expirationTime);
    }


    public static TestBearerToken create(String token){
        return new TestBearerToken(token, null);
    }
}
