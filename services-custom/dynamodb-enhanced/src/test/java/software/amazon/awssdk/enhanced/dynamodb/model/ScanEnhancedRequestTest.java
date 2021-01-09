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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class ScanEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        ScanEnhancedRequest builtObject = ScanEnhancedRequest.builder().build();

        assertThat(builtObject.exclusiveStartKey(), is(nullValue()));
        assertThat(builtObject.consistentRead(), is(nullValue()));
        assertThat(builtObject.filterExpression(), is(nullValue()));
        assertThat(builtObject.attributesToProject(), is(nullValue()));
        assertThat(builtObject.limit(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
        exclusiveStartKey.put("id", stringValue("id-value"));
        exclusiveStartKey.put("sort", numberValue(7));

        Map<String, AttributeValue> expressionValues = singletonMap(":test-key", stringValue("test-value"));
        Expression filterExpression = Expression.builder()
                                                .expression("test-expression")
                                                .expressionValues(expressionValues)
                                                .build();

        String[] attributesToProjectArray = {"one", "two"};
        String additionalElement = "three";
        List<String> attributesToProject = new ArrayList<>(Arrays.asList(attributesToProjectArray));
        attributesToProject.add(additionalElement);

        ScanEnhancedRequest builtObject = ScanEnhancedRequest.builder()
                                                             .exclusiveStartKey(exclusiveStartKey)
                                                             .consistentRead(false)
                                                             .filterExpression(filterExpression)
                                                             .attributesToProject(attributesToProjectArray)
                                                             .addAttributeToProject(additionalElement)
                                                             .limit(3)
                                                             .build();

        assertThat(builtObject.exclusiveStartKey(), is(exclusiveStartKey));
        assertThat(builtObject.consistentRead(), is(false));
        assertThat(builtObject.filterExpression(), is(filterExpression));
        assertThat(builtObject.attributesToProject(), is(attributesToProject));
        assertThat(builtObject.limit(), is(3));
    }

    @Test
    public void test_withNestedAttributeAddedFirst() {

        String[] attributesToProjectArray = {"one", "two"};
        String additionalElement = "three";
        ScanEnhancedRequest builtObject = ScanEnhancedRequest.builder()
                .addNestedAttributesToProject(NestedAttributeName.create("foo", "bar"))
                .attributesToProject(attributesToProjectArray)
                .addAttributeToProject(additionalElement)
                .build();
        List<String> attributesToProject = Arrays.asList("one", "two", "three");
        assertThat(builtObject.attributesToProject(), is(attributesToProject));
    }


    @Test
    public void test_nestedAttributesToProjectWithNestedAttributeAddedLast() {

        String[] attributesToProjectArray = {"one", "two"};
        String additionalElement = "three";

        ScanEnhancedRequest builtObjectOne = ScanEnhancedRequest.builder()
                .attributesToProject(attributesToProjectArray)
                .addAttributeToProject(additionalElement)
                .addNestedAttributesToProject(NestedAttributeName.create("foo", "bar"))
                .build();
        List<String> attributesToProjectNestedLast = Arrays.asList("one", "two", "three", "foo.bar");
        assertThat(builtObjectOne.attributesToProject(), is(attributesToProjectNestedLast));

    }

    @Test
    public void test_nestedAttributesToProjectWithNestedAttributeAddedInBetween() {

        String[] attributesToProjectArray = {"one", "two"};
        String additionalElement = "three";

        ScanEnhancedRequest builtObjectOne = ScanEnhancedRequest.builder()
                .attributesToProject(attributesToProjectArray)
                .addNestedAttributesToProject(NestedAttributeName.create("foo", "bar"))
                .addAttributeToProject(additionalElement)
                .build();
        List<String> attributesToProjectNestedLast = Arrays.asList("one", "two", "foo.bar", "three");
        assertThat(builtObjectOne.attributesToProject(), is(attributesToProjectNestedLast));

    }
    @Test
    public void test_nestedAttributesToProjectOverwrite() {

        String[] attributesToProjectArray = {"one", "two"};
        String additionalElement = "three";
        String[] overwrite = { "overwrite"};

        ScanEnhancedRequest builtObjectTwo = ScanEnhancedRequest.builder()
                .attributesToProject(attributesToProjectArray)
                .addAttributeToProject(additionalElement)
                .addNestedAttributesToProject(NestedAttributeName.create("foo", "bar"))
                .attributesToProject(overwrite)
                .build();
        assertThat(builtObjectTwo.attributesToProject(), is(Arrays.asList(overwrite)));
    }

    @Test
    public void test_nestedAttributesNullStringElement() {

        String[] attributesToProjectArray = {"one", "two", null};
        String additionalElement = "three";
        assertFails(() -> ScanEnhancedRequest.builder()
                .attributesToProject(attributesToProjectArray)
                .addAttributeToProject(additionalElement)
                .addAttributeToProject(null)
                .addNestedAttributesToProject(NestedAttributeName.create("foo", "bar"))
                .build());

        assertFails(() -> ScanEnhancedRequest.builder()
                .attributesToProject("foo", "bar", null)
                .build());

    }

    @Test
    public void test_nestedAttributesNullNestedAttributeElement() {
        List<NestedAttributeName> attributeNames = new ArrayList<>();
        attributeNames.add(NestedAttributeName.create("foo"));
        attributeNames.add(null);
        assertFails(() -> ScanEnhancedRequest.builder()
                .addNestedAttributesToProject(attributeNames)
                .build());
        assertFails(() -> ScanEnhancedRequest.builder()
                .addNestedAttributesToProject(NestedAttributeName.create("foo", "bar"), null)
                .build());
        NestedAttributeName nestedAttributeName = null;
        ScanEnhancedRequest.builder()
                .addNestedAttributeToProject(nestedAttributeName)
                .build();
        assertFails(() -> ScanEnhancedRequest.builder()
                .addNestedAttributesToProject(nestedAttributeName)
                .build());
    }



    @Test
    public void toBuilder() {
        ScanEnhancedRequest builtObject = ScanEnhancedRequest.builder().exclusiveStartKey(null).build();

        ScanEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

}
