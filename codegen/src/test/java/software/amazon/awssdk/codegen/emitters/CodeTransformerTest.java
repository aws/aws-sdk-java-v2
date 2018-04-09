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

package software.amazon.awssdk.codegen.emitters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class CodeTransformerTest {

    @Test
    public void chainCallsProcessorsInOrderAndPassedResultOfPreviousProcessor() {
        String input = "hello", intermediate = "world", output = "earth";
        CodeTransformer firstProcessor = mockProcessor(input, intermediate), secondProcessor = mockProcessor(intermediate, output);
        CodeTransformer chain = CodeTransformer.chain(firstProcessor, secondProcessor);

        assertThat(chain.apply(input), equalTo(output));
    }

    private CodeTransformer mockProcessor(String accepts, String returns) {
        CodeTransformer processor = mock(CodeTransformer.class);
        when(processor.apply(accepts)).thenReturn(returns);
        return processor;
    }
}
