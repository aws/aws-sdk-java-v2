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

package software.amazon.awssdk.http.auth.aws.crt;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * This is a place-holder class for this module, http-auth-aws-crt. In the event that we decide to move CRT-v4a signer
 * logic back to this dedicated module, no issues will arise. This module should be an optional dependency in consumers
 * (http-auth-aws), and should bring in the required dependencies (aws-crt).
 */
@SdkProtectedApi
public final class HttpAuthAwsCrt {
}
