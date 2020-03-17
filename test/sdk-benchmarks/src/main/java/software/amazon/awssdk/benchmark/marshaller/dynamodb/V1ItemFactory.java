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

package software.amazon.awssdk.benchmark.marshaller.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public final class V1ItemFactory extends AbstractItemFactory<AttributeValue> {
    @Override
    protected AttributeValue av(String val) {
        return new AttributeValue()
            .withS(val);
    }

    @Override
    protected AttributeValue av(ByteBuffer val) {
        return new AttributeValue()
            .withB(val);
    }

    @Override
    protected AttributeValue av(List<AttributeValue> val) {
        return new AttributeValue()
            .withL(val);
    }

    @Override
    protected AttributeValue av(Map<String, AttributeValue> val) {
        return new AttributeValue()
            .withM(val);
    }
}
