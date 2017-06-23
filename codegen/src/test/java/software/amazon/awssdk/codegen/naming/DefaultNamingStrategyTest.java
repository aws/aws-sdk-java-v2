/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package software.amazon.awssdk.codegen.naming;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class DefaultNamingStrategyTest {

    @Test
    public void canConvertStringsWithNonAlphasToClassNames() {
        NamingStrategy sut = new DefaultNamingStrategy(null, null);
        String anInvalidClassName = "a phrase-With_other.delimiters";
        assertThat(sut.getJavaClassName(anInvalidClassName), equalTo("APhraseWithOtherDelimiters"));
    }

    @Test
    public void canConvertAuthorizerStartingWithNumber() {
        NamingStrategy sut = new DefaultNamingStrategy(null, null);
        String anInvalidClassName = "35-authorizer-implementation";
        assertThat(sut.getAuthorizerClassName(anInvalidClassName), equalTo("I35AuthorizerImplementation"));
    }

}
