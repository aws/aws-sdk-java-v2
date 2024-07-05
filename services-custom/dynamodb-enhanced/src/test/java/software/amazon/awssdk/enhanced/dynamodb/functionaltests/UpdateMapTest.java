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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeMapping.NESTED;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeMapping.SHALLOW;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecursiveRecordBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecursiveRecordImmutable;
import software.amazon.awssdk.enhanced.dynamodb.internal.DynamoDBEnhancedRequestConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.AddressBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.PersonBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.RelativeBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UpdateMapTest {
    @Test
    public void recursiveRecord_document() {
        TableSchema<PersonBean> tableSchema = TableSchema.fromClass(PersonBean.class);

        AddressBean dadAddress = new AddressBean();
        dadAddress.setStreet("Portland Ave");
        dadAddress.setPostCode("98200");
        dadAddress.setCity("Seattle");

        RelativeBean dad = new RelativeBean();
        dad.setAttribute2("goodbye");
        dad.setAddress(dadAddress);

        PersonBean stanley = new PersonBean();
        stanley.setAttribute1("hello");
        stanley.setRelativeBean(dad);


        Map<String, qAttributeValue> itemMapShallow = tableSchema.itemToMap(stanley, true,
                                                                    new DynamoDBEnhancedRequestConfiguration(SHALLOW));

        Map<String, AttributeValue> itemMapNested = tableSchema.itemToMap(stanley, true,
                                                                    new DynamoDBEnhancedRequestConfiguration(NESTED));


        assertThat(itemMapShallow).hasSize(3);
        assertThat(itemMapNested).hasSize(5);
    }

}
