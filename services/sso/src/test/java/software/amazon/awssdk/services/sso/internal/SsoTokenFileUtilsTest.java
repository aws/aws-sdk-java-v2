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

package software.amazon.awssdk.services.sso.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.sso.internal.SsoTokenFileUtils.generateCachedTokenPath;
import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;

import org.junit.Test;

public class SsoTokenFileUtilsTest {

    @Test
    public void generateTheCorrectPathTest() {
        String startUrl = "https//d-abc123.awsapps.com/start";
        String directory = "~/.aws/sso/cache";
        assertThat(generateCachedTokenPath(startUrl, directory).toString())
            .isEqualTo(userHomeDirectory() + "/.aws/sso/cache/6a888bdb653a4ba345dd68f21b896ec2e218c6f4.json");
    }

}