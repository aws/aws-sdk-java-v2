/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights
 * Reserved.
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
package software.amazon.awssdk.mapper.dynamodb.mapper;

import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.createMultipleObjectTestDataForTransactionLoad;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.Existence;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.ProjectionExpressionMode;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.TransactionLoadTestData;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.TransactionLoadTestDataRequest;
import static org.junit.Assert.assertEquals;

import com.amazonaws.AmazonServiceException;
import org.junit.Test;

public class TransactionLoadExpressionTest extends TransactionsTestBase {

    @Test
    public void testWithOneLoadExpression() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(6)
                                                                                             .withExistence(Existence.SOME)
                                                                                             .withUseSameObjectType(false)
                                                                                             .withProjectionExpressionMode(ProjectionExpressionMode.ONE);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }

    @Test
    public void testWithMultipleLoadExpressions() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(6)
                                                                                             .withExistence(Existence.SOME)
                                                                                             .withUseSameObjectType(false)
                                                                                             .withProjectionExpressionMode(ProjectionExpressionMode.MULTIPLE);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }

    @Test
    public void testWithMultipleLoadExpressionsButOnlyOneHavingAttributeNames() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(6)
                                                                                             .withExistence(Existence.SOME)
                                                                                             .withUseSameObjectType(false)
                                                                                             .withProjectionExpressionMode(ProjectionExpressionMode.ONE_ATTRIBUTE_NAMES);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }

    @Test
    public void testWithMultipleLoadExpressionsHavingAttributeNames() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(6)
                                                                                             .withExistence(Existence.SOME)
                                                                                             .withUseSameObjectType(false)
                                                                                             .withProjectionExpressionMode(ProjectionExpressionMode.MULTIPLE_ATTRIBUTE_NAMES);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }

    @Test
    public void testWithOneLoadExpressionContainingReservedWord() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(6)
                                                                                             .withExistence(Existence.SOME)
                                                                                             .withUseSameObjectType(false)
                                                                                             .withProjectionExpressionMode(ProjectionExpressionMode.ONE_RESERVED_WORD);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        try {
            executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
        } catch (AmazonServiceException ex) {
            assertEquals("ValidationException", ex.getErrorCode());
        }
    }
}
