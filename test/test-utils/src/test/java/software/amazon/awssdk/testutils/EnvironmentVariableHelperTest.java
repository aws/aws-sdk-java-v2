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

package software.amazon.awssdk.testutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class EnvironmentVariableHelperTest {

    private static Map<String, String> environmentVariables = new HashMap<>(System.getenv());

    @Rule
    public EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

    @AfterClass
    public static void ensureCleanup() {
        assertThat(System.getenv()).hasSameSizeAs(environmentVariables).containsAllEntriesOf(environmentVariables);
    }

    @Test
    public void helperAsRuleIsResetAfterEachUse() {
        helper.set("yo yo yo", "blah");
    }
}