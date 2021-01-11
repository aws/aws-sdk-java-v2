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

package software.amazon.awssdk.auth.credentials;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Provides access to the AWS credentials used for accessing AWS services: AWS access key ID and secret access key. These
 * credentials are used to securely sign requests to AWS services.
 *
 * <p>For more details on AWS access keys, see:
 * <a href="http://docs.amazonwebservices.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html#AccessKeys">
 * http://docs.amazonwebservices.com/AWSSecurityCredentials/1.0/AboutAWSCredentials.html#AccessKeys</a></p>
 *
 * @see AwsCredentialsProvider
 */
@SdkPublicApi
public interface AwsCredentials {

    /**
     * Retrieve the AWS access key, used to identify the user interacting with AWS.
     */
    String accessKeyId();

    /**
     * Retrieve the AWS secret access key, used to authenticate the user interacting with AWS.
     */
    String secretAccessKey();
}
