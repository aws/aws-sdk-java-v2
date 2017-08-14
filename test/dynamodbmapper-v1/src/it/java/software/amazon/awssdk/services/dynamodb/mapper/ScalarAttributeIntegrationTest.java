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

package software.amazon.awssdk.services.dynamodb.mapper;

import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbScalarAttribute;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.pojos.AutoKeyAndVal;

/**
 * Status tests for {@code ScalarAttribute}.
 */
public class ScalarAttributeIntegrationTest extends AbstractKeyAndValIntegrationTestCase {

    /**
     * Test with a non-null enum val.
     */
    @Test
    public void testMarshalling() {
        final KeyAndBinaryUuid object = new KeyAndBinaryUuid();
        object.setVal(UUID.randomUUID());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * An object with an enumeration.
     */
    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static class KeyAndBinaryUuid extends AutoKeyAndVal<UUID> {
        @DynamoDbScalarAttribute(type = ScalarAttributeType.B)
        public UUID getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final UUID val) {
            super.setVal(val);
        }
    }

}
