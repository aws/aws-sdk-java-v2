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

package software.amazon.awssdk.v2migration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

public final class CredentialsDependencyFactory {

    private CredentialsDependencyFactory() {
    }

    public static AWSCredentialsProvider defaultCredentialsProviderChain() {
        return new DefaultAWSCredentialsProviderChain();
    }

    public static AWSCredentialsProvider environmentCredentialsProvider() {
        return new EnvironmentVariableCredentialsProvider();
    }

    public static AWSCredentialsProvider instanceProfileCredentialsProvider() {
        return InstanceProfileCredentialsProvider.getInstance();
    }

    public static AWSCredentialsProvider profileCredentialsProvider() {
        return new ProfileCredentialsProvider();
    }

    public static AWSCredentialsProvider staticCredentialsProvider() {
        AWSCredentials credentials  = new BasicAWSCredentials("accessKey", "secretKey");
        return new AWSStaticCredentialsProvider(credentials);
    }

    public static AWSCredentialsProvider staticSessionCredentialsProvider() {
        AWSCredentials credentials = new BasicSessionCredentials("accessKey", "secretKey", "session");
        return new AWSStaticCredentialsProvider(credentials);
    }
}
