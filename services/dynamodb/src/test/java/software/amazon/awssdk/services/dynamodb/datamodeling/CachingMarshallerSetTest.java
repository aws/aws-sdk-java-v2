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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.datamodeling.ConversionSchemas.CachingMarshallerSet;
import software.amazon.awssdk.services.dynamodb.datamodeling.ConversionSchemas.MarshallerSet;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.pojos.TestClass;

public class CachingMarshallerSetTest {

    private static final TestMarshallerSet MOCK = new TestMarshallerSet();
    private static final MarshallerSet SUT = new CachingMarshallerSet(MOCK);

    @Test
    public void testIt() throws Exception {
        ArgumentMarshaller marshaller = new ArgumentMarshaller() {
            @Override
            public AttributeValue marshall(Object value) {
                return null;
            }
        };

        MOCK.queue.add(marshaller);

        ArgumentMarshaller result = SUT.marshaller(
                TestClass.class.getMethod("getString"));

        Assert.assertSame(marshaller, result);

        result = SUT.marshaller(TestClass.class.getMethod("getString"));

        Assert.assertSame(marshaller, result);

        ArgumentMarshaller marshaller2 = new ArgumentMarshaller() {
            @Override
            public AttributeValue marshall(Object value) {
                return null;
            }
        };

        MOCK.queue.add(marshaller2);

        result = SUT.marshaller(TestClass.class.getMethod("getInt"));

        Assert.assertSame(marshaller2, result);
    }

    private static class TestMarshallerSet implements MarshallerSet {

        private final Deque<ArgumentMarshaller> queue =
                new ArrayDeque<ArgumentMarshaller>();

        private final Deque<ArgumentMarshaller> memberQueue =
                new ArrayDeque<ArgumentMarshaller>();

        @Override
        public ArgumentMarshaller marshaller(Method getter) {
            return queue.remove();
        }

        @Override
        public ArgumentMarshaller memberMarshaller(Type memberType) {
            return memberQueue.remove();
        }
    }
}
