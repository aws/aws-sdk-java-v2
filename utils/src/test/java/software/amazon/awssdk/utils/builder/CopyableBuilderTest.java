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

package software.amazon.awssdk.utils.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CopyableBuilderTest {

    @Test
    public void canApplyAFunctionToTheBuilder() {
        ClassToBuild builtClass = ClassToBuild.builder().name("jeffery").apply(this::upperCaseName).build();

        assertThat(builtClass.name).isEqualTo("JEFFERY");
    }

    @Test
    public void canCopyABuilder() {
        ClassToBuild.Builder builder = ClassToBuild.builder().name("Stanley");

        ClassToBuild.Builder copied = builder.copy().name("Alexander");

        assertThat(builder.build().name).isEqualTo("Stanley");
        assertThat(copied.build().name).isEqualTo("Alexander");
    }

    private ClassToBuild.Builder upperCaseName(ClassToBuild.Builder builder) {
        return builder.name(builder.name.toUpperCase());
    }


    private static class ClassToBuild implements ToCopyableBuilder<ClassToBuild.Builder, ClassToBuild> {

        private final String name;

        private ClassToBuild(Builder builder) {
            this.name = builder.name;
        }

        @Override
        public Builder toBuilder() {
            return new Builder(this);
        }

        public static Builder builder() {
            return new Builder();
        }

        static class Builder implements CopyableBuilder<Builder, ClassToBuild> {

            private String name;

            private Builder() {}

            private Builder(ClassToBuild source) {
                this.name = source.name;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            @Override
            public ClassToBuild build() {
                return new ClassToBuild(this);
            }
        }
    }
}
