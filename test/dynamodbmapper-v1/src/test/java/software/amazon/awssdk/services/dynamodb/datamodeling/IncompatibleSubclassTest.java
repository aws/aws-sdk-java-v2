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

import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Verify that we fail fast in case of incompatible subclasses that try to
 * override the (now-removed) transformAttributes method.
 */
public class IncompatibleSubclassTest {

    @Test
    public void testCompatibleSubclass() {
        // Doesn't try to override one of the deprecated/removed
        // transformAttributes methods; should be fine.
        new CompatibleDynamoDbMapper();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleSubclass1() {
        // "Overrides" transformAttributes(Class, Map); should fail fast.
        new IncompatibleDynamoDbMapper1();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleSubclass2() {
        // "Overrides" transformAttributes(String, String, Map); should fail
        // fast.
        new IncompatibleDynamoDbMapper2();
    }

    private static class CompatibleDynamoDbMapper extends DynamoDbMapper {

        public CompatibleDynamoDbMapper() {
            super(null);
        }

        protected void transformAttributes(boolean innocuous) {
        }
    }

    private static class IncompatibleDynamoDbMapper1 extends DynamoDbMapper {

        public IncompatibleDynamoDbMapper1() {
            super(null);
        }

        protected Map<String, AttributeValue> transformAttributes(
                Class<?> clazz,
                Map<String, AttributeValue> attributeValues) {

            return null;
        }
    }

    private static class IncompatibleDynamoDbMapper2 extends DynamoDbMapper {

        public IncompatibleDynamoDbMapper2() {
            super(null);
        }

        protected Map<String, AttributeValue> transformAttributes(
                String hashKey,
                String rangeKey,
                Map<String, AttributeValue> attributeValues) {

            return null;
        }
    }
}
