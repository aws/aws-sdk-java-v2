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

package software.amazon.awssdk.services.s3.internal.s3express;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link S3ExpressSessionCredentials}.
 */
@SdkInternalApi
public class DefaultS3ExpressSessionCredentials implements S3ExpressSessionCredentials {
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;

    public DefaultS3ExpressSessionCredentials(String accessKeyId, String secretAccessKey, String sessionToken) {
        this.accessKeyId = Validate.notBlank(accessKeyId, "Parameter accessKeyId cannot be blank");
        this.secretAccessKey = Validate.notBlank(secretAccessKey, "Parameter secretAccessKey cannot be blank");
        this.sessionToken = sessionToken;
    }

    @Override
    public String accessKeyId() {
        return accessKeyId;
    }

    @Override
    public String secretAccessKey() {
        return secretAccessKey;
    }

    @Override
    public String sessionToken() {
        return sessionToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3ExpressSessionCredentials that = (S3ExpressSessionCredentials) o;

        if (!accessKeyId.equals(that.accessKeyId())) {
            return false;
        }

        if (!secretAccessKey.equals(that.secretAccessKey())) {
            return false;
        }
        return sessionToken != null ? sessionToken.equals(that.sessionToken()) : that.sessionToken() == null;
    }

    @Override
    public int hashCode() {
        int result = accessKeyId.hashCode();
        result = 31 * result + secretAccessKey.hashCode();
        result = 31 * result + (sessionToken != null ? sessionToken.hashCode() : 0);
        return result;
    }
}
