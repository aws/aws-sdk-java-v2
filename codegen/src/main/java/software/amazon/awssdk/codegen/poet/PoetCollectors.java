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

package software.amazon.awssdk.codegen.poet;

import com.squareup.javapoet.CodeBlock;
import java.util.StringJoiner;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A set of {@link Collector} implementations, similar to {@link Collectors}, but for poet types.
 */
public final class PoetCollectors {
    private PoetCollectors() {}

    /**
     * Join a stream of code blocks into one code block.
     */
    public static Collector<CodeBlock, ?, CodeBlock> toCodeBlock() {
        return Collector.of(CodeBlock::builder, CodeBlock.Builder::add,
                            PoetCollectors::parallelNotSupported, CodeBlock.Builder::build);
    }

    /**
     * Join a stream of code blocks into one code block, separated by the provided delimiter. Useful for joining an arbitrary
     * number of method parameters, code statements, etc.
     */
    public static Collector<CodeBlock, ?, CodeBlock> toDelimitedCodeBlock(String delimiter) {
        return Collector.of(() -> new CodeBlockJoiner(delimiter), CodeBlockJoiner::add,
                            PoetCollectors::parallelNotSupported, CodeBlockJoiner::join);
    }

    /**
     * A joiner, similar to {@link StringJoiner}, used by {@link #toDelimitedCodeBlock(String)} in order to join multiple code
     * blocks into one block, delimited by a character.
     *
     * <p>This class is not thread-safe.</p>
     */
    private static class CodeBlockJoiner {
        private final CodeBlock.Builder builder = CodeBlock.builder();
        private final String delimiter;
        private boolean builderEmpty = true;

        /**
         * Create a code block joiner that will separate statements by the provided delimiter.
         * @param delimiter The delimiter that should separate each code statement.
         */
        private CodeBlockJoiner(String delimiter) {
            this.delimiter = delimiter;
        }

        /**
         * Add a code block to the joined result.
         */
        private void add(CodeBlock block) {
            if (!builderEmpty) {
                builder.add(CodeBlock.of(delimiter));
            } else {
                builderEmpty = false;
            }
            builder.add(block);
        }

        /**
         * Join all code blocks and return the joined result.
         */
        private CodeBlock join() {
            return builder.build();
        }
    }

    /**
     * A convenience method that can be passed as a {@link Collector}'s "combiner" to indicate parallel collecting is not
     * supported.
     */
    private static <T> T parallelNotSupported(Object... ignoredParams) {
        throw new UnsupportedOperationException("Parallel collecting is not supported.");
    }
}
