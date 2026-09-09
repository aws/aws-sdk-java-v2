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

package software.amazon.awssdk.mapper.dynamodb.test;

import software.amazon.awssdk.mapper.dynamodb.test.retry.RetryRule;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.mapper.dynamodb.test.util.InputStreamUtils;
import software.amazon.awssdk.mapper.dynamodb.test.util.SdkAsserts;
import software.amazon.awssdk.utils.IoUtils;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.utils.StringUtils;
import org.junit.Rule;

public abstract class AWSTestBase {

    /**
     * Shared AWS credentials, loaded from a properties file. Direct access to this field is
     * deprecated
     *
     * @deprecated Use the credentials resolved by this base class instead of accessing this field directly
     */
    @Deprecated
    public static AwsCredentials credentials;

    // Matches the profile that v2's shared AwsIntegrationTestBase/AwsTestBase resolve, and the profile the
    // catapult release/PR buildspec writes credentials under (alongside [default]). See
    // test/service-test-utils AwsIntegrationTestBase and AwsSdkJava2CatapultCDK test-specs.
    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-test-account";

    /**
     * ToD test can be configured to use Role ARN and will pull them from STS. These credentials are then available
     * for use during the test run. The location of the credentials file is passed to the test run in the form of
     * the environment variable TOD_CUSTOMER_CREDENTIAL_PATH.
     */

    private static final String TOD_CREDENTIAL_PATH = System.getenv("TOD_CUSTOMER_CREDENTIAL_PATH");

    private static final AwsCredentialsProviderChain chain = createChain();

    @Rule
    public RetryRule retry = new RetryRule(3, 2, TimeUnit.SECONDS);

    /**
     * @deprecated Use the credentials resolved by this base class instead of calling this directly
     */
    @Deprecated
    public static void setUpCredentials() {
        if (credentials == null) {
            try {
                credentials = chain.resolveCredentials();
            } catch (Exception ignored) {
            }
        }
    }

    protected void setRetryRule(RetryRule rule) {
        this.retry = rule;
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
            String resourceAsString = IoUtils.toUtf8String(resourceStream);
            resourceStream.close();
            return resourceAsString;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertNotEmpty(String str) {
        SdkAsserts.assertNotEmpty(str);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertFileEqualsStream(File expected, InputStream actual) {
        SdkAsserts.assertFileEqualsStream(expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertFileEqualsStream(String errmsg, File expected, InputStream actual) {
        SdkAsserts.assertFileEqualsStream(errmsg, expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertStreamEqualsStream(InputStream expected, InputStream actual) {
        SdkAsserts.assertStreamEqualsStream(expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertStreamEqualsStream(String errmsg, InputStream expectedInputStream, InputStream inputStream) {
        assertStreamEqualsStream(errmsg, expectedInputStream, inputStream);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertFileEqualsFile(File expected, File actual) {
        SdkAsserts.assertFileEqualsFile(expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertStringEqualsStream(String expected, InputStream actual) {
        SdkAsserts.assertStringEqualsStream(expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected boolean doesStreamEqualStream(InputStream expected, InputStream actual) throws IOException {
        return SdkAsserts.doesStreamEqualStream(expected, actual);
    }

    /**
     * @deprecated Use {@link InputStreamUtils#drainInputStream(InputStream)}
     */
    @Deprecated
    protected byte[] drainInputStream(InputStream inputStream) {
        return InputStreamUtils.drainInputStream(inputStream);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected boolean doesFileEqualStream(File expectedFile, InputStream inputStream) throws IOException {
        return SdkAsserts.doesFileEqualStream(expectedFile, inputStream);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertValidException(AwsServiceException e) {
        SdkAsserts.assertValidException(e);
    }

    private static AwsCredentialsProviderChain createChain() {
        if (StringUtils.isBlank(TOD_CREDENTIAL_PATH)) {
            // Mirror v2's shared AwsIntegrationTestBase: the aws-test-account profile first, then the default
            // provider chain (env vars, system properties, the [default] profile, and container/instance creds).
            return AwsCredentialsProviderChain.of(
                    ProfileCredentialsProvider.create(TEST_CREDENTIALS_PROFILE_NAME),
                    DefaultCredentialsProvider.create());
        }
        return AwsCredentialsProviderChain.of(
                ProfileCredentialsProvider.create("default"),
                ProfileCredentialsProvider.create(TEST_CREDENTIALS_PROFILE_NAME),
                EnvironmentVariableCredentialsProvider.create(),
                SystemPropertyCredentialsProvider.create());
    }
}
