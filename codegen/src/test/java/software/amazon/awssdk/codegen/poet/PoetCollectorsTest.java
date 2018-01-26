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

package software.amazon.awssdk.codegen.poet;

import static org.assertj.core.api.Assertions.assertThat;

import com.squareup.javapoet.CodeBlock;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Validate functionality of the {@link PoetCollectors}.
 */
public class PoetCollectorsTest {
    @Test
    public void emptyCollectIsEmptyCodeBlock() {
        CodeBlock result = Stream.<CodeBlock>of().collect(PoetCollectors.toCodeBlock());
        assertThat(result).isEqualTo(CodeBlock.builder().build());
    }

    @Test
    public void emptyDelimitedCollectIsEmptyCodeBlock() {
        CodeBlock result = Stream.<CodeBlock>of().collect(PoetCollectors.toDelimitedCodeBlock(","));
        assertThat(result).isEqualTo(CodeBlock.builder().build());
    }

    @Test
    public void codeBlocksJoined() {
        CodeBlock a = CodeBlock.of("a");
        CodeBlock b = CodeBlock.of("b");
        CodeBlock ab = CodeBlock.builder().add(a).add(b).build();

        CodeBlock result = Stream.of(a, b).collect(PoetCollectors.toCodeBlock());
        assertThat(result).isEqualTo(ab);
    }

    @Test
    public void delimitedCodeBlocksJoined() {
        CodeBlock a = CodeBlock.of("a");
        CodeBlock b = CodeBlock.of("b");
        CodeBlock delimeter = CodeBlock.of(",");
        CodeBlock ab = CodeBlock.builder().add(a).add(delimeter).add(b).build();

        CodeBlock result = Stream.of(a, b).collect(PoetCollectors.toDelimitedCodeBlock(","));
        assertThat(result).isEqualTo(ab);
    }
}
