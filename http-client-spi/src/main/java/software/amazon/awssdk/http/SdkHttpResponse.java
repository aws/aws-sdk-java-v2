/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http;

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * An immutable HTTP response without access to the response body. {@link SdkHttpFullResponse} should be used when access to a
 * response body stream is required.
 */
@SdkPublicApi
@Immutable
public interface SdkHttpResponse extends SdkHttpHeaders {
    /**
     * Returns the HTTP status text returned by the service.
     *
     * <p>If this was not provided by the service, empty will be returned.</p>
     */
    Optional<String> statusText();

    /**
     * Returns the HTTP status code (eg. 200, 404, etc.) returned by the service.
     *
     * <p>This will always be positive.</p>
     */
    int statusCode();
}
