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

package software.amazon.awssdk.services.dynamodb.document.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.document.utils.FluentHashSet;

public class ItemValueConformerTest {

    @Test
    public void byteBuffer() {
        byte[] bytes = {1, 2, 3};
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        byte[] bytesTo = (byte[]) new ItemValueConformer().transform(bb);
        assertTrue(Arrays.equals(bytesTo, bytes));
    }

    @Test
    public void emptySet() {
        Set<?> from = new HashSet<Object>();
        Set<?> to = (Set<?>) new ItemValueConformer().transform(from);
        assertTrue(to.size() == 0);
    }

    @Test
    public void stringSet() {
        Set<?> from = new FluentHashSet<String>("a", "b");
        Set<?> to = (Set<?>) new ItemValueConformer().transform(from);
        assertTrue(to.size() == 2);
        assertTrue(to.contains("a"));
        assertTrue(to.contains("b"));
    }

    @Test
    public void bytesSet() {
        byte[] bytes123 = {1, 2, 3};
        byte[] bytes456 = {4, 5, 6};
        Set<?> from = new FluentHashSet<byte[]>(bytes123, bytes456);
        @SuppressWarnings("unchecked")
        Set<byte[]> to = (Set<byte[]>) new ItemValueConformer().transform(from);
        assertTrue(to.size() == 2);
        boolean a = false, b = false;
        for (byte[] bytes : to) {
            if (Arrays.equals(bytes123, bytes)) {
                a = true;
            } else if (Arrays.equals(bytes456, bytes)) {
                b = true;
            }
        }
        assertTrue(a);
        assertTrue(b);
    }

    @Test
    public void byteBufferSet() {
        byte[] bytes123 = {1, 2, 3};
        byte[] bytes456 = {4, 5, 6};
        Set<?> from = new FluentHashSet<ByteBuffer>(ByteBuffer.wrap(bytes123), ByteBuffer.wrap(bytes456));
        @SuppressWarnings("unchecked")
        Set<byte[]> to = (Set<byte[]>) new ItemValueConformer().transform(from);
        assertTrue(to.size() == 2);
        boolean a = false, b = false;
        for (byte[] bytes : to) {
            if (Arrays.equals(bytes123, bytes)) {
                a = true;
            } else if (Arrays.equals(bytes456, bytes)) {
                b = true;
            }
        }
        assertTrue(a);
        assertTrue(b);
    }

    @Test
    public void bigDecimalSet() {
        Set<?> from = new FluentHashSet<BigDecimal>(BigDecimal.ZERO, BigDecimal.TEN);
        Set<?> to = (Set<?>) new ItemValueConformer().transform(from);
        assertTrue(to.size() == 2);
        assertTrue(to.contains(BigDecimal.ZERO));
        assertTrue(to.contains(BigDecimal.TEN));
    }

    @Test
    public void bigNumberSet() {
        Set<?> from = new FluentHashSet<Number>(Integer.MAX_VALUE, Double.MAX_VALUE);
        Set<?> to = (Set<?>) new ItemValueConformer().transform(from);
        assertTrue(to.size() == 2);

        assertFalse(to.contains(Integer.MAX_VALUE));
        assertFalse(to.contains(Double.MAX_VALUE));

        assertTrue(to.contains(new BigDecimal(String.valueOf(Integer.MAX_VALUE))));
        assertTrue(to.contains(new BigDecimal(String.valueOf(Double.MAX_VALUE))));
    }

    @Test
    public void emptyMap() {
        Map<?, ?> from = new HashMap<Object, Object>();
        Map<?, ?> to = (Map<?, ?>) new ItemValueConformer().transform(from);
        assertTrue(to.size() == 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void uknownType() {
        new ItemValueConformer().transform(new Object());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void uknownsetType() {
        new ItemValueConformer().transform(new FluentHashSet<Object>(new Object()));
    }
}
