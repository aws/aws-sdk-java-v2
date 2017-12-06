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

import static org.junit.Assert.assertThat;
import static software.amazon.awssdk.utils.StringUtils.isBlank;

import java.io.IOException;
import java.io.InputStream;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.core.auth.AwsCredentialsProviderChain;
import software.amazon.awssdk.core.auth.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.auth.ProfileCredentialsProvider;
import software.amazon.awssdk.core.auth.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;

public abstract class AwsTestBase {
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
     * @deprecated Extend from {@link AwsIntegrationTestBase} to access credentials
     */
    @Deprecated
    public static void setUpCredentials() {
        // Ignored
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
            return IoUtils.toString(resourceStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated Use {@link #isValidAmazonServiceException} in a hamcrest matcher
     */
    @Deprecated
    protected void assertValidException(AmazonServiceException e) {
        assertThat(e, isValidAmazonServiceException());
    }

    public static Matcher<AmazonServiceException> isValidAmazonServiceException() {
        return new TypeSafeMatcher<AmazonServiceException>() {
            private StringBuilder sb = new StringBuilder();
            @Override
            protected boolean matchesSafely(AmazonServiceException item) {
                isNotBlank(item.getRequestId(), "requestId");
                isNotBlank(item.getMessage(), "message");
                isNotBlank(item.getErrorCode(), "errorCode");
                isNotBlank(item.getServiceName(), "serviceName");
                return sb.length() == 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(sb.toString());
            }

            private void isNotBlank(String value, String fieldName) {
                if (isBlank(value)) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(fieldName).append(" should not be null or blank");
                }
            }
        };
    }
}
