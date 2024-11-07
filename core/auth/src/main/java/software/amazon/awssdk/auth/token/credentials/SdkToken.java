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
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/**
 * Bearer token, used for accessing AWS services. This interface has been superseded by {@link TokenIdentity}.
 *
 * <p>
 * To avoid unnecessary churn this class has not been marked as deprecated, but it's recommended to use
 * {@link TokenIdentity} when defining generic token providers because it provides the same functionality with
 * considerably fewer dependencies.
 */
@SdkPublicApi
public interface SdkToken extends TokenIdentity {
}
