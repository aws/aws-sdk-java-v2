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

package software.amazon.awssdk.services.s3.s3express;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.internal.s3express.DefaultS3ExpressSessionCredentials;
import software.amazon.awssdk.services.s3.model.CreateSessionRequest;
import software.amazon.awssdk.services.s3.model.SessionCredentials;

/**
 * Session credentials used by {@link S3ExpressAuthScheme}.
 *
 * <p>While these contain the same information as {@link AwsCredentialsIdentity}, they are only valid for accessing S3 express,
 * and they are only valid for use when accessing a single bucket. S3 express is able to validate these credentials more quickly
 * than standard {@link AwsCredentialsIdentity}s, reducing the latency required for each request.
 *
 * <p>Users of the SDK should not need to create this themselves. These credentials are created automatically by the
 * {@link S3ExpressAuthScheme} that is automatically included with {@link S3Client} when
 * {@link S3ClientBuilder#disableS3ExpressSessionAuth(Boolean)} is not true.
 *
 * @see S3ExpressAuthScheme
 * @see S3Client#createSession(CreateSessionRequest)
 */
@SdkPublicApi
public interface S3ExpressSessionCredentials extends AwsCredentialsIdentity {

    /**
     * Retrieve the S3 express token.
     */
    String sessionToken();

    /**
     * Create S3 express session credentials for the provided access key ID, secret access key and session token.
     */
    static S3ExpressSessionCredentials create(String accessKeyId, String secretAccessKey, String sessionToken) {
        return new DefaultS3ExpressSessionCredentials(accessKeyId, secretAccessKey, sessionToken);
    }

    /**
     * Create S3 express session credentials for the provided {@link SessionCredentials}.
     */
    static S3ExpressSessionCredentials fromSessionResponse(SessionCredentials credentials) {
        return create(credentials.accessKeyId(),
                      credentials.secretAccessKey(),
                      credentials.sessionToken());
    }
}
