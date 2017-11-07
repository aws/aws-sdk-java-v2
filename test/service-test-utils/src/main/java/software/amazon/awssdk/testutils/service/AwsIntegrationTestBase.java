/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.testutils.service;

import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.auth.AwsCredentialsProviderChain;
import software.amazon.awssdk.core.auth.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.auth.ProfileCredentialsProvider;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.auth.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;

public abstract class AwsIntegrationTestBase {

    /** Default Properties Credentials file path. */
    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-test-account";

    public static final AwsCredentialsProviderChain CREDENTIALS_PROVIDER_CHAIN =
        AwsCredentialsProviderChain.builder()
                                   .credentialsProviders(ProfileCredentialsProvider.builder()
                                                                                   .profileName(TEST_CREDENTIALS_PROFILE_NAME)
                                                                                   .build(),
                                                         SystemPropertyCredentialsProvider.create(),
                                                         EnvironmentVariableCredentialsProvider.create())
                                   .build();


    /**
     * Shared AWS credentials, loaded from a properties file.
     */
    private static final AwsCredentials CREDENTIALS = CREDENTIALS_PROVIDER_CHAIN.getCredentials();

    /**
     * @return AWSCredentials to use during tests. Setup by base fixture
     * @deprecated by {@link #getCredentialsProvider()}
     */
    @Deprecated
    protected static AwsCredentials getCredentials() {
        return CREDENTIALS;
    }

    /**
     * @return AwsCredentialsProvider to use during tests. Setup by base fixture
     */
    protected static AwsCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(CREDENTIALS);
    }

    /**
     * Reads a system resource fully into a String
     *
     * @param location
     *            Relative or absolute location of system resource.
     * @return String contents of resource file
     * @throws RuntimeException
     *             if any error occurs
     */
    protected String getResourceAsString(Class<?> clazz, String location) {
        try (InputStream resourceStream = clazz.getResourceAsStream(location)) {
            String resourceAsString = IoUtils.toString(resourceStream);
            resourceStream.close();
            return resourceAsString;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
