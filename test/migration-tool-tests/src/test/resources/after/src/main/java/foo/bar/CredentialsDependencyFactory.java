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

package foo.bar;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public final class CredentialsDependencyFactory {

    private CredentialsDependencyFactory() {
    }

    public static AWSCredentialsProvider defaultCredentialsProviderChain() {
        return DefaultCredentialsProvider.builder().build();
    }

    public static AWSCredentialsProvider environmentCredentialsProvider() {
        return EnvironmentVariableCredentialsProvider.create();
    }

    public static AWSCredentialsProvider instanceProfileCredentialsProvider() {
        return InstanceProfileCredentialsProvider.create();
    }

    public static AWSCredentialsProvider profileCredentialsProvider() {
        return ProfileCredentialsProvider.builder().build();
    }

    public static AWSCredentialsProvider staticCredentialsProvider() {
        AwsCredentials credentials  = AwsBasicCredentials.create("accessKey", "secretKey");
        return StaticCredentialsProvider.create(credentials);
    }

    public static AWSCredentialsProvider staticSessionCredentialsProvider() {
        AwsCredentials credentials = new BasicSessionCredentials("accessKey", "secretKey", "session");
        return StaticCredentialsProvider.create(credentials);
    }
}
