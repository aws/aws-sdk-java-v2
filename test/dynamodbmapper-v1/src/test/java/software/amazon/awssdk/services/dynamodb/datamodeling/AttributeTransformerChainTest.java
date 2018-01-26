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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.datamodeling.AttributeTransformer.Parameters;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class AttributeTransformerChainTest {
    @Test
    public void testTransformEmptyChain() {

        AttributeTransformer transformer = new AttributeTransformerChain(
                Collections.<AttributeTransformer>emptyList()
        );

        Map<String, AttributeValue> values =
                new HashMap<String, AttributeValue>();

        Parameters<?> params = new TestParameters<Object>(values);

        Map<String, AttributeValue> result = transformer.transform(params);
        Assert.assertSame(values, result);
        Assert.assertTrue(values.isEmpty());
    }

    @Test
    public void testUntransformEmptyChain() {

        AttributeTransformer transformer = new AttributeTransformerChain(
                Collections.<AttributeTransformer>emptyList()
        );

        Map<String, AttributeValue> values =
                new HashMap<String, AttributeValue>();

        Parameters<?> params = new TestParameters<Object>(values);

        Map<String, AttributeValue> result = transformer.untransform(params);
        Assert.assertSame(values, result);
        Assert.assertTrue(values.isEmpty());
    }

    @Test
    public void testTransform() {

        AttributeTransformer transformer1 = new TestTransformer(".one");
        AttributeTransformer transformer2 = new TestTransformer(".two");

        AttributeTransformer chain =
                new AttributeTransformerChain(transformer1, transformer2);

        Map<String, AttributeValue> values =
                new HashMap<String, AttributeValue>();

        values.put("test1", AttributeValue.builder().s("foo").build());
        values.put("test2", AttributeValue.builder().s("bar").build());

        Parameters<?> params = new TestParameters<Object>(values);

        Map<String, AttributeValue> result = chain.transform(params);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        Assert.assertEquals("foo.one.two", result.get("test1").s());
        Assert.assertEquals("bar.one.two", result.get("test2").s());
    }

    @Test
    public void testUntransform() {

        AttributeTransformer transformer1 = new TestTransformer(".one");
        AttributeTransformer transformer2 = new TestTransformer(".two");

        AttributeTransformer chain =
                new AttributeTransformerChain(transformer1, transformer2);

        Map<String, AttributeValue> values =
                new HashMap<String, AttributeValue>();

        values.put("test1", AttributeValue.builder().s("foo.one.two").build());
        values.put("test2", AttributeValue.builder().s("bar.one.two").build());

        Parameters<?> params = new TestParameters<Object>(values);

        Map<String, AttributeValue> result = chain.untransform(params);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        Assert.assertEquals("foo", result.get("test1").s());
        Assert.assertEquals("bar", result.get("test2").s());
    }

    @Test
    public void testRoundTrip() {

        AttributeTransformer transformer1 = new TestTransformer(".one");
        AttributeTransformer transformer2 = new TestTransformer(".two");

        AttributeTransformer chain =
                new AttributeTransformerChain(transformer1, transformer2);

        Map<String, AttributeValue> values =
                new HashMap<String, AttributeValue>();

        values.put("test1", AttributeValue.builder().s("foo").build());
        values.put("test2", AttributeValue.builder().s("bar").build());

        Parameters<?> params = new TestParameters<Object>(values);

        Map<String, AttributeValue> result = chain.transform(params);

        params = new TestParameters<Object>(result);

        result = chain.untransform(params);

        Assert.assertEquals(values, result);
    }

    private static class TestTransformer implements AttributeTransformer {

        private final String appendMe;

        public TestTransformer(final String appendMe) {
            this.appendMe = appendMe;
        }

        @Override
        public Map<String, AttributeValue> transform(
                final Parameters<?> parameters) {

            Map<String, AttributeValue> rval =
                    new HashMap<String, AttributeValue>();

            for (Map.Entry<String, AttributeValue> entry
                    : parameters.getAttributeValues().entrySet()) {

                rval.put(entry.getKey(), transform(entry.getValue()));
            }

            return rval;
        }

        @Override
        public Map<String, AttributeValue> untransform(
                final Parameters<?> parameters) {

            Map<String, AttributeValue> rval =
                    new HashMap<String, AttributeValue>();

            for (Map.Entry<String, AttributeValue> entry
                    : parameters.getAttributeValues().entrySet()) {

                rval.put(entry.getKey(), untransform(entry.getValue()));
            }

            return rval;
        }

        private AttributeValue transform(AttributeValue value) {
            return AttributeValue.builder().s(value.s() + appendMe).build();
        }

        private AttributeValue untransform(AttributeValue value) {
            String s = value.s();
            if (s.endsWith(appendMe)) {
                return AttributeValue.builder().s(
                        s.substring(0, s.length() - appendMe.length())).build();
            } else {
                return value;
            }
        }
    }
}
