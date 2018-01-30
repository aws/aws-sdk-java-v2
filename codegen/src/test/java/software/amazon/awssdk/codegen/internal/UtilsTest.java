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

package software.amazon.awssdk.codegen.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class UtilsTest {
    final Map<String,String> capitalizedToUncapitalized = new HashMap<String,String>() {{
        put("A", "a");
        put("AB", "ab");
        put("ABC", "abc");
        put("ABCD", "abcd");

        put("AToken", "aToken");
        put("MFAToken", "mfaToken");
        put("AWSRequest", "awsRequest");

        put("MfaToken", "mfaToken");
        put("AwsRequest", "awsRequest");
    }};

    @Test
    public void testUnCapitalize() {
        capitalizedToUncapitalized.forEach((capitalized,unCapitalized) ->
                assertThat(Utils.unCapitialize(capitalized), is(equalTo(unCapitalized))));
    }
}
