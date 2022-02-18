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

import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Provides token which is used to securely authorize requests to AWS services.
 * A token is a string that the OAuth client uses to make requests to the resource server.
 *
 * <p>For more details on tokens, see:
 * <a href="https://oauth.net/2/access-tokens">
 * https://oauth.net/2/access-tokens</a></p>
 *
 * @see SdkTokenProvider
 */

@SdkPublicApi
public interface SdkToken {


    /**
     * Retrieves string field representing the literal token string.
     * A token is a string that the OAuth client uses to make requests to the resource server.
     */
    String token();


    /**
     * Retrieves the time at which the token expires.
     */
    Optional<Instant> expirationTime();
}
