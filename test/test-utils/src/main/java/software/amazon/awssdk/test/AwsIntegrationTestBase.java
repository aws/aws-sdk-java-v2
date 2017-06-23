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

package software.amazon.awssdk.test;

import java.io.InputStream;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;

public abstract class AwsIntegrationTestBase {

    /** Default Properties Credentials file path. */
    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-test-account";

    public static final AwsCredentialsProviderChain CREDENTIALS_PROVIDER_CHAIN =
        AwsCredentialsProviderChain.builder()
                                   .credentialsProviders(ProfileCredentialsProvider.builder()
                                                                                   .profileName(TEST_CREDENTIALS_PROFILE_NAME)
                                                                                   .build(),
                                                         new SystemPropertyCredentialsProvider(),
                                                         new EnvironmentVariableCredentialsProvider())
                                   .build();


    /**
     * Shared AWS credentials, loaded from a properties file.
     */
    private static AwsCredentials credentials;

    /**
     * Before of super class is guaranteed to be called before that of a subclass so the following
     * is safe. http://junit-team.github.io/junit/javadoc/latest/org/junit/Before.html
     */
    @BeforeClass
    public static void setUpCredentials() {
        if (credentials == null) {
            try {
                credentials = CREDENTIALS_PROVIDER_CHAIN.getCredentials();
            } catch (Exception ignored) {
                // Ignored.
            }
        }
    }

    /**
     * @return AWSCredentials to use during tests. Setup by base fixture
     */
    protected static AwsCredentials getCredentials() {
        return credentials;
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
    protected String getResourceAsString(String location) {
        try {
            InputStream resourceStream = getClass().getResourceAsStream(location);
            String resourceAsString = IoUtils.toString(resourceStream);
            resourceStream.close();
            return resourceAsString;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
