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

package software.amazon.awssdk.enhanced.dynamodb.internal.immutable;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

public class ImmutableIntrospectorTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @DynamoDbImmutable(builder = SimpleImmutableMixedStyle.Builder.class)
    private static final class SimpleImmutableMixedStyle {
        public String getAttribute1() {
            throw new UnsupportedOperationException();
        }

        public Integer attribute2() {
            throw new UnsupportedOperationException();
        }

        public Boolean isAttribute3() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public void setAttribute1(String attribute1) {
                throw new UnsupportedOperationException();
            }

            public Builder attribute2(Integer attribute2) {
                throw new UnsupportedOperationException();
            }

            public Void setAttribute3(Boolean attribute3) {
                throw new UnsupportedOperationException();
            }

            public SimpleImmutableMixedStyle build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void simpleImmutableMixedStyle() {
        ImmutableInfo<SimpleImmutableMixedStyle> immutableInfo =
            ImmutableIntrospector.getImmutableInfo(SimpleImmutableMixedStyle.class);

        assertThat(immutableInfo.immutableClass()).isSameAs(SimpleImmutableMixedStyle.class);
        assertThat(immutableInfo.builderClass()).isSameAs(SimpleImmutableMixedStyle.Builder.class);
        assertThat(immutableInfo.buildMethod().getReturnType()).isSameAs(SimpleImmutableMixedStyle.class);
        assertThat(immutableInfo.buildMethod().getParameterCount()).isZero();
        assertThat(immutableInfo.staticBuilderMethod()).isNotPresent();
        assertThat(immutableInfo.propertyDescriptors()).hasSize(3);
        assertThat(immutableInfo.propertyDescriptors()).anySatisfy(p -> {
            assertThat(p.name()).isEqualTo("attribute1");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(String.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(String.class);
        });
        assertThat(immutableInfo.propertyDescriptors()).anySatisfy(p -> {
            assertThat(p.name()).isEqualTo("attribute2");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(Integer.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(Integer.class);
        });
        assertThat(immutableInfo.propertyDescriptors()).anySatisfy(p -> {
            assertThat(p.name()).isEqualTo("attribute3");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(Boolean.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(Boolean.class);
        });
    }

    @DynamoDbImmutable(builder = SimpleImmutableWithPrimitives.Builder.class)
    private static final class SimpleImmutableWithPrimitives {
        public int attribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder attribute(int attribute) {
                throw new UnsupportedOperationException();
            }

            public SimpleImmutableWithPrimitives build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void simpleImmutableWithPrimitives() {
        ImmutableInfo<SimpleImmutableWithPrimitives> immutableInfo =
            ImmutableIntrospector.getImmutableInfo(SimpleImmutableWithPrimitives.class);

        assertThat(immutableInfo.immutableClass()).isSameAs(SimpleImmutableWithPrimitives.class);
        assertThat(immutableInfo.builderClass()).isSameAs(SimpleImmutableWithPrimitives.Builder.class);
        assertThat(immutableInfo.buildMethod().getReturnType()).isSameAs(SimpleImmutableWithPrimitives.class);
        assertThat(immutableInfo.buildMethod().getParameterCount()).isZero();
        assertThat(immutableInfo.staticBuilderMethod()).isNotPresent();
        assertThat(immutableInfo.propertyDescriptors()).hasOnlyOneElementSatisfying(p -> {
            assertThat(p.name()).isEqualTo("attribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(int.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(int.class);
        });
    }

    @DynamoDbImmutable(builder = SimpleImmutableWithTrickyNames.Builder.class)
    private static final class SimpleImmutableWithTrickyNames {
        public String isAttribute() {
            throw new UnsupportedOperationException();
        }

        public String getGetAttribute() {
            throw new UnsupportedOperationException();
        }

        public String getSetAttribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder isAttribute(String isAttribute) {
                throw new UnsupportedOperationException();
            }

            public Builder getAttribute(String getAttribute) {
                throw new UnsupportedOperationException();
            }

            public Builder setSetAttribute(String setAttribute) {
                throw new UnsupportedOperationException();
            }

            public SimpleImmutableWithTrickyNames build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void simpleImmutableWithTrickyNames() {
        ImmutableInfo<SimpleImmutableWithTrickyNames> immutableInfo =
            ImmutableIntrospector.getImmutableInfo(SimpleImmutableWithTrickyNames.class);

        assertThat(immutableInfo.immutableClass()).isSameAs(SimpleImmutableWithTrickyNames.class);
        assertThat(immutableInfo.builderClass()).isSameAs(SimpleImmutableWithTrickyNames.Builder.class);
        assertThat(immutableInfo.buildMethod().getReturnType()).isSameAs(SimpleImmutableWithTrickyNames.class);
        assertThat(immutableInfo.buildMethod().getParameterCount()).isZero();
        assertThat(immutableInfo.staticBuilderMethod()).isNotPresent();
        assertThat(immutableInfo.propertyDescriptors()).hasSize(3);
        assertThat(immutableInfo.propertyDescriptors()).anySatisfy(p -> {
            assertThat(p.name()).isEqualTo("isAttribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(String.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(String.class);
        });
        assertThat(immutableInfo.propertyDescriptors()).anySatisfy(p -> {
            assertThat(p.name()).isEqualTo("getAttribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(String.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(String.class);
        });
        assertThat(immutableInfo.propertyDescriptors()).anySatisfy(p -> {
            assertThat(p.name()).isEqualTo("setAttribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(String.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(String.class);
        });
    }

    @DynamoDbImmutable(builder = ImmutableWithNoMatchingSetter.Builder.class)
    private static final class ImmutableWithNoMatchingSetter {
        public int rightAttribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder wrongAttribute(int attribute) {
                throw new UnsupportedOperationException();
            }

            public ImmutableWithNoMatchingSetter build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableWithNoMatchingSetter() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("rightAttribute");
        exception.expectMessage("matching setter");
        ImmutableIntrospector.getImmutableInfo(ImmutableWithNoMatchingSetter.class);
    }

    @DynamoDbImmutable(builder = ImmutableWithGetterParams.Builder.class)
    private static final class ImmutableWithGetterParams {
        public int rightAttribute(String illegalParam) {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder rightAttribute(int rightAttribute) {
                throw new UnsupportedOperationException();
            }

            public ImmutableWithGetterParams build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableWithGetterParams() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("rightAttribute");
        exception.expectMessage("getter");
        exception.expectMessage("parameters");
        ImmutableIntrospector.getImmutableInfo(ImmutableWithGetterParams.class);
    }

    @DynamoDbImmutable(builder = ImmutableWithVoidAttribute.Builder.class)
    private static final class ImmutableWithVoidAttribute {
        public Void rightAttribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder rightAttribute(Void rightAttribute) {
                throw new UnsupportedOperationException();
            }

            public ImmutableWithVoidAttribute build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableWithVoidAttribute() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("rightAttribute");
        exception.expectMessage("getter");
        exception.expectMessage("void");
        ImmutableIntrospector.getImmutableInfo(ImmutableWithVoidAttribute.class);
    }

    @DynamoDbImmutable(builder = ImmutableWithNoMatchingGetter.Builder.class)
    private static final class ImmutableWithNoMatchingGetter {
        public static final class Builder {
            public Builder rightAttribute(int attribute) {
                throw new UnsupportedOperationException();
            }

            public ImmutableWithNoMatchingGetter build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableWithNoMatchingGetter() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("rightAttribute");
        exception.expectMessage("matching getter");
        ImmutableIntrospector.getImmutableInfo(ImmutableWithNoMatchingGetter.class);
    }

    @DynamoDbImmutable(builder = ImmutableWithNoBuildMethod.Builder.class)
    private static final class ImmutableWithNoBuildMethod {
        public int rightAttribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder rightAttribute(int attribute) {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableWithNoBuildMethod() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("build");
        ImmutableIntrospector.getImmutableInfo(ImmutableWithNoBuildMethod.class);
    }

    @DynamoDbImmutable(builder = ImmutableWithWrongSetter.Builder.class)
    private static final class ImmutableWithWrongSetter {
        public int rightAttribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder rightAttribute(String attribute) {
                throw new UnsupportedOperationException();
            }

            public ImmutableWithWrongSetter build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableWithWrongSetter() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("rightAttribute");
        exception.expectMessage("matching setter");
        ImmutableIntrospector.getImmutableInfo(ImmutableWithWrongSetter.class);
    }

    @DynamoDbImmutable(builder = ImmutableWithWrongBuildType.Builder.class)
    private static final class ImmutableWithWrongBuildType {
        public int rightAttribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder rightAttribute(int attribute) {
                throw new UnsupportedOperationException();
            }

            public String build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableWithWrongBuildType() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("build");
        ImmutableIntrospector.getImmutableInfo(ImmutableWithWrongBuildType.class);
    }

    private static final class ImmutableMissingAnnotation {
        public int rightAttribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder rightAttribute(int attribute) {
                throw new UnsupportedOperationException();
            }

            public ImmutableMissingAnnotation build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableMissingAnnotation() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("@DynamoDbImmutable");
        ImmutableIntrospector.getImmutableInfo(ImmutableMissingAnnotation.class);
    }

    @DynamoDbImmutable(builder = SimpleImmutableWithIgnoredGetter.Builder.class)
    private static final class SimpleImmutableWithIgnoredGetter {
        public int attribute() {
            throw new UnsupportedOperationException();
        }

        @DynamoDbIgnore
        public int ignoreMe() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder attribute(int attribute) {
                throw new UnsupportedOperationException();
            }

            public SimpleImmutableWithIgnoredGetter build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void simpleImmutableWithIgnoredGetter() {
        ImmutableInfo<SimpleImmutableWithIgnoredGetter> immutableInfo =
            ImmutableIntrospector.getImmutableInfo(SimpleImmutableWithIgnoredGetter.class);

        assertThat(immutableInfo.immutableClass()).isSameAs(SimpleImmutableWithIgnoredGetter.class);
        assertThat(immutableInfo.builderClass()).isSameAs(SimpleImmutableWithIgnoredGetter.Builder.class);
        assertThat(immutableInfo.buildMethod().getReturnType()).isSameAs(SimpleImmutableWithIgnoredGetter.class);
        assertThat(immutableInfo.buildMethod().getParameterCount()).isZero();
        assertThat(immutableInfo.staticBuilderMethod()).isNotPresent();
        assertThat(immutableInfo.propertyDescriptors()).hasOnlyOneElementSatisfying(p -> {
            assertThat(p.name()).isEqualTo("attribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(int.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(int.class);
        });
    }

    @DynamoDbImmutable(builder = SimpleImmutableWithIgnoredSetter.Builder.class)
    private static final class SimpleImmutableWithIgnoredSetter {
        public int attribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder attribute(int attribute) {
                throw new UnsupportedOperationException();
            }

            @DynamoDbIgnore
            public int ignoreMe() {
                throw new UnsupportedOperationException();
            }

            public SimpleImmutableWithIgnoredSetter build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void simpleImmutableWithIgnoredSetter() {
        ImmutableInfo<SimpleImmutableWithIgnoredSetter> immutableInfo =
            ImmutableIntrospector.getImmutableInfo(SimpleImmutableWithIgnoredSetter.class);

        assertThat(immutableInfo.immutableClass()).isSameAs(SimpleImmutableWithIgnoredSetter.class);
        assertThat(immutableInfo.builderClass()).isSameAs(SimpleImmutableWithIgnoredSetter.Builder.class);
        assertThat(immutableInfo.buildMethod().getReturnType()).isSameAs(SimpleImmutableWithIgnoredSetter.class);
        assertThat(immutableInfo.buildMethod().getParameterCount()).isZero();
        assertThat(immutableInfo.staticBuilderMethod()).isNotPresent();
        assertThat(immutableInfo.propertyDescriptors()).hasOnlyOneElementSatisfying(p -> {
            assertThat(p.name()).isEqualTo("attribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(int.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(int.class);
        });
    }

    private static class ExtendedImmutableBase {
        public int baseAttribute() {
            throw new UnsupportedOperationException();
        }

        public static class Builder {
            public Builder baseAttribute(int attribute) {
                throw new UnsupportedOperationException();
            }
        }
    }

    @DynamoDbImmutable(builder = ExtendedImmutable.Builder.class)
    private static final class ExtendedImmutable extends ExtendedImmutableBase {
        public int childAttribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder extends ExtendedImmutableBase.Builder {
            public Builder childAttribute(int attribute) {
                throw new UnsupportedOperationException();
            }

            public ExtendedImmutable build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void extendedImmutable() {
        ImmutableInfo<ExtendedImmutable> immutableInfo =
            ImmutableIntrospector.getImmutableInfo(ExtendedImmutable.class);

        assertThat(immutableInfo.immutableClass()).isSameAs(ExtendedImmutable.class);
        assertThat(immutableInfo.builderClass()).isSameAs(ExtendedImmutable.Builder.class);
        assertThat(immutableInfo.buildMethod().getReturnType()).isSameAs(ExtendedImmutable.class);
        assertThat(immutableInfo.buildMethod().getParameterCount()).isZero();
        assertThat(immutableInfo.staticBuilderMethod()).isNotPresent();
        assertThat(immutableInfo.propertyDescriptors()).hasSize(2);
        assertThat(immutableInfo.propertyDescriptors()).anySatisfy(p -> {
            assertThat(p.name()).isEqualTo("baseAttribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(int.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(int.class);
        });
        assertThat(immutableInfo.propertyDescriptors()).anySatisfy(p -> {
            assertThat(p.name()).isEqualTo("childAttribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(int.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(int.class);
        });
    }

    @DynamoDbImmutable(builder = ImmutableWithPrimitiveBoolean.Builder.class)
    private static final class ImmutableWithPrimitiveBoolean {
        public boolean isAttribute() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            public Builder attribute(boolean attribute) {
                throw new UnsupportedOperationException();
            }

            public ImmutableWithPrimitiveBoolean build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableWithPrimitiveBoolean() {
        ImmutableInfo<ImmutableWithPrimitiveBoolean> immutableInfo =
            ImmutableIntrospector.getImmutableInfo(ImmutableWithPrimitiveBoolean.class);

        assertThat(immutableInfo.immutableClass()).isSameAs(ImmutableWithPrimitiveBoolean.class);
        assertThat(immutableInfo.builderClass()).isSameAs(ImmutableWithPrimitiveBoolean.Builder.class);
        assertThat(immutableInfo.buildMethod().getReturnType()).isSameAs(ImmutableWithPrimitiveBoolean.class);
        assertThat(immutableInfo.buildMethod().getParameterCount()).isZero();
        assertThat(immutableInfo.staticBuilderMethod()).isNotPresent();
        assertThat(immutableInfo.propertyDescriptors()).hasOnlyOneElementSatisfying(p -> {
            assertThat(p.name()).isEqualTo("attribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(boolean.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(boolean.class);
        });
    }

    @DynamoDbImmutable(builder = ImmutableWithStaticBuilder.Builder.class)
    private static final class ImmutableWithStaticBuilder {
        public boolean isAttribute() {
            throw new UnsupportedOperationException();
        }

        public static Builder builder() {
            throw new UnsupportedOperationException();
        }

        public static final class Builder {
            private Builder() {
            }

            public Builder attribute(boolean attribute) {
                throw new UnsupportedOperationException();
            }

            public ImmutableWithStaticBuilder build() {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    public void immutableWithStaticBuilder() {
        ImmutableInfo<ImmutableWithStaticBuilder> immutableInfo =
            ImmutableIntrospector.getImmutableInfo(ImmutableWithStaticBuilder.class);

        assertThat(immutableInfo.immutableClass()).isSameAs(ImmutableWithStaticBuilder.class);
        assertThat(immutableInfo.builderClass()).isSameAs(ImmutableWithStaticBuilder.Builder.class);
        assertThat(immutableInfo.buildMethod().getReturnType()).isSameAs(ImmutableWithStaticBuilder.class);
        assertThat(immutableInfo.buildMethod().getParameterCount()).isZero();
        assertThat(immutableInfo.staticBuilderMethod())
            .hasValueSatisfying(m -> assertThat(m.getName()).isEqualTo("builder"));
        assertThat(immutableInfo.propertyDescriptors()).hasOnlyOneElementSatisfying(p -> {
            assertThat(p.name()).isEqualTo("attribute");
            assertThat(p.getter().getParameterCount()).isZero();
            assertThat(p.getter().getReturnType()).isSameAs(boolean.class);
            assertThat(p.setter().getParameterCount()).isEqualTo(1);
            assertThat(p.setter().getParameterTypes()[0]).isSameAs(boolean.class);
        });
    }
}