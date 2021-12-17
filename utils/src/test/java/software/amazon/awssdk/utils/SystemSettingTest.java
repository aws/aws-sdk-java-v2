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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Java6Assertions.assertThat;
import org.junit.Test;

public class SystemSettingTest {

    @Test
    public void getNonDefaultStringValue_doesNotReturnDefaultValue() {
        TestSystemSetting setting = new TestSystemSetting("prop", "env", "default");

        assertThat(setting.getNonDefaultStringValue().isPresent()).isFalse();
    }

    private static class TestSystemSetting implements SystemSetting {
        private final String property;
        private final String environmentVariable;
        private final String defaultValue;

        public TestSystemSetting(String property, String environmentVariable, String defaultValue) {
            this.property = property;
            this.environmentVariable = environmentVariable;
            this.defaultValue = defaultValue;
        }

        @Override
        public String property() {
            return property;
        }

        @Override
        public String environmentVariable() {
            return environmentVariable;
        }

        @Override
        public String defaultValue() {
            return defaultValue;
        }
    }
}
