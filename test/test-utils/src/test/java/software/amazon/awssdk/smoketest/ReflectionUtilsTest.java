/*
 * Copyright (c) 2016. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.smoketest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

@SuppressWarnings("unused")
public class ReflectionUtilsTest {

    @Test
    public void canSetAllNonStaticFields() {
        TestClass instance = ReflectionUtils.newInstanceWithAllFieldsSet(TestClass.class);

        assertThat(instance.stringProperty, is(notNullValue()));
        assertThat(instance.booleanProperty, is(notNullValue()));
        assertThat(instance.longProperty, is(notNullValue()));
        assertThat(instance.intProperty, is(notNullValue()));
        assertThat(instance.primitiveBooleanProperty, is(true));
    }

    @Test
    public void canGiveSupplierForCustomType() {
        final TestClass complex = mock(TestClass.class);
        ClassWithComplexClass instance = ReflectionUtils.newInstanceWithAllFieldsSet(ClassWithComplexClass.class, new ReflectionUtils.RandomSupplier<TestClass>() {
            @Override
            public TestClass getNext() {
                return complex;
            }

            @Override
            public Class<TestClass> targetClass() {
                return TestClass.class;
            }
        });

        assertThat(instance.complexClass, is(sameInstance(complex)));
    }

    public static class ClassWithComplexClass {
        private TestClass complexClass;
    }

    public static class TestClass {
        private String stringProperty;
        private Boolean booleanProperty;
        private Integer intProperty;
        private Long longProperty;
        private boolean primitiveBooleanProperty;
    }

}
