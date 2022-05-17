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

package software.amazon.awssdk.testutils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.utils.SystemSetting;

public class EnvironmentVariableHelperTest {
    @Test
    public void testCanUseStaticRun() {
        assertThat(SystemSetting.getStringValueFromEnvironmentVariable("hello")).isEmpty();
        EnvironmentVariableHelper.run(helper -> {
            helper.set("hello", "world");
            assertThat(SystemSetting.getStringValueFromEnvironmentVariable("hello")).hasValue("world");
        });
        assertThat(SystemSetting.getStringValueFromEnvironmentVariable("hello")).isEmpty();
    }

    @Test
    public void testCanManuallyReset() {
        EnvironmentVariableHelper helper = new EnvironmentVariableHelper();
        assertThat(SystemSetting.getStringValueFromEnvironmentVariable("hello")).isEmpty();
        helper.set("hello", "world");
        assertThat(SystemSetting.getStringValueFromEnvironmentVariable("hello")).hasValue("world");
        helper.reset();
        assertThat(SystemSetting.getStringValueFromEnvironmentVariable("hello")).isEmpty();
    }
}
