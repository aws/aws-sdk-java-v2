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

package software.amazon.awssdk.services.signin.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Indicates that the cached Login token is missing or unusable (the file cannot be read, does not contain a JSON object,
 * a required member is missing or invalid, or the dpopKey is invalid), and the user must re-authenticate.
 *
 * <p>The {@code LoginCredentialsProvider} treats this exception as non-recoverable: it is raised to the caller immediately,
 * even when cached credentials exist, it is cached for a short (1-5 second) period, and no refresh backoff is applied.
 */
@SdkInternalApi
public final class InvalidLoginTokenException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private InvalidLoginTokenException(SdkClientException.Builder b) {
        super(b);
    }

    public static InvalidLoginTokenException create(String message) {
        return create(message, null);
    }

    public static InvalidLoginTokenException create(String message, Throwable cause) {
        return new InvalidLoginTokenException(SdkClientException.builder().message(message).cause(cause));
    }
}
