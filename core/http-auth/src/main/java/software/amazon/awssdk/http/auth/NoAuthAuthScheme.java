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

package software.amazon.awssdk.http.auth;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * No-auth auth scheme ID.
 */
@SdkPublicApi
public final class NoAuthAuthScheme {
    /**
     * The scheme ID for the no-auth auth scheme.
     */
    public static final String SCHEME_ID = "smithy.api#noAuth";

    private NoAuthAuthScheme() {
    }
}
