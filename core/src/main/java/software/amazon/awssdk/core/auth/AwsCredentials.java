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

package software.amazon.awssdk.core.auth;

import static software.amazon.awssdk.utils.StringUtils.trimToNull;

import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Provides access to the AWS credentials used for accessing AWS services: AWS access key ID and secret access key. These
 * credentials are used to securely sign requests to AWS services.
 *
 * <p>For more details on AWS access keys, see:
 * <a href="http://docs.amazonwebservices.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html#AccessKeys">
 *     http://docs.amazonwebservices.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html#AccessKeys</a></p>
 *
 * @see AwsCredentialsProvider
 */
@Immutable
public class AwsCredentials {
    /**
     * A set of AWS credentials without an access key or secret access key, indicating that anonymous access should be used.
     *
     * This should be accessed via {@link AnonymousCredentialsProvider#getCredentials()}.
     */
    @SdkInternalApi
    static final AwsCredentials ANONYMOUS_CREDENTIALS = new AwsCredentials(null, null, false);

    private final String accessKeyId;
    private final String secretAccessKey;

    /**
     * Constructs a new credentials object, with the specified AWS access key, AWS secret key and AWS session token.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with AWS.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with AWS.
     */
    protected AwsCredentials(String accessKeyId, String secretAccessKey) {
        this(accessKeyId, secretAccessKey, true);
    }

    private AwsCredentials(String accessKeyId, String secretAccessKey, boolean validateCredentials) {
        this.accessKeyId = trimToNull(accessKeyId);
        this.secretAccessKey = trimToNull(secretAccessKey);

        if (validateCredentials) {
            Validate.notNull(this.accessKeyId, "Access key ID cannot be blank.");
            Validate.notNull(this.secretAccessKey, "Secret access key cannot be blank.");
        }
    }

    /**
     * Constructs a new credentials object, with the specified AWS access key, AWS secret key and AWS session token.
     *
     * @param accessKeyId The AWS access key, used to identify the user interacting with AWS.
     * @param secretAccessKey The AWS secret access key, used to authenticate the user interacting with AWS.
     * */
    public static AwsCredentials create(String accessKeyId, String secretAccessKey) {
        return new AwsCredentials(accessKeyId, secretAccessKey);
    }

    /**
     * Retrieve the AWS access key, used to identify the user interacting with AWS.
     */
    public final String accessKeyId() {
        return accessKeyId;
    }

    /**
     * Retrieve the AWS secret access key, used to authenticate the user interacting with AWS.
     */
    public final String secretAccessKey() {
        return secretAccessKey;
    }

    @Override
    public String toString() {
        return ToString.builder("AwsCredentials")
                       .add("accessKeyId", accessKeyId)
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
        final AwsCredentials that = (AwsCredentials) o;
        return Objects.equals(accessKeyId, that.accessKeyId) &&
               Objects.equals(secretAccessKey, that.secretAccessKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessKeyId, secretAccessKey);
    }
}
