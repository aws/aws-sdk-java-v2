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

package software.amazon.awssdk.auth.credentials;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class StaticCredentialsProviderTest {
    @Test
    public void getAwsCredentials_ReturnsSameCredentials() throws Exception {
        final AwsCredentials credentials = new AwsCredentials("akid", "skid");
        final AwsCredentials actualCredentials =
                StaticCredentialsProvider.create(credentials).getCredentials();
        assertEquals(credentials, actualCredentials);
    }

    @Test
    public void getSessionAwsCredentials_ReturnsSameCredentials() throws Exception {
        final AwsSessionCredentials credentials = AwsSessionCredentials.create("akid", "skid", "token");
        final AwsCredentials actualCredentials = StaticCredentialsProvider.create(credentials).getCredentials();
        assertEquals(credentials, actualCredentials);
    }

    @Test(expected = RuntimeException.class)
    public void nullCredentials_ThrowsIllegalArgumentException() {
        StaticCredentialsProvider.create(null);
    }
}
