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

package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.services.simpledb.model.InvalidQueryExpressionException;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;
import software.amazon.awssdk.services.simpledb.model.SelectRequest;

/**
 * Integration tests for the exceptional cases of the SimpleDB Select operation.
 *
 * @author fulghum@amazon.com
 */
public class SelectIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that the Select operation throws a MissingParameterException when DomainName isn't
     * specified.
     */
    @Test
    public void testSelectMissingParameterException() {
        SelectRequest request = SelectRequest.builder().build();
        try {
            sdb.select(request);
            fail("Excepted MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that the Select operation throws a NoSuchDomainException when a non-existent domain
     * name is specified.
     */
    @Test
    public void testSelectNoSuchDomainException() {
        SelectRequest request = SelectRequest.builder().selectExpression("select * from foobarbazbarbashbar").build();
        try {
            sdb.select(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that the Select operation throws an InvalidQueryExpressionException when an invalid
     * query is passed.
     */
    @Test
    public void testSelectInvalidQueryExpressionException() {
        SelectRequest request = SelectRequest.builder().selectExpression("foobarbazbar").build();
        try {
            sdb.select(request);
            fail("Expected InvalidQueryExpressionException, but wasn't thrown");
        } catch (InvalidQueryExpressionException e) {
            assertValidException(e);
        }
    }

}
