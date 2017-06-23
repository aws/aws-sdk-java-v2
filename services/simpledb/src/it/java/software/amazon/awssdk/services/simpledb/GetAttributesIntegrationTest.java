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
import software.amazon.awssdk.services.simpledb.model.GetAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;

/**
 * Integration tests for the exceptional cases of the SimpleDB GetAttributes operation.
 *
 * @author fulghum@amazon.com
 */
public class GetAttributesIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that the GetAttributes operation throws a NoSuchDomainException when an invalid domain
     * is specified.
     */
    @Test
    public void testGetAttributesNoSuchDomainException() {
        GetAttributesRequest request = GetAttributesRequest.builder()
                .itemName("foobar")
                .domainName("foobarbazbarbashbar")
                .build();
        try {
            sdb.getAttributes(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that the GetAttributes operation throws a MissingParameterException if either domain
     * name or item name aren't specified.
     */
    @Test
    public void testGetAttributesMissingParameterException() {
        GetAttributesRequest request = GetAttributesRequest.builder()
                .itemName("foobar")
                .build();
        try {
            sdb.getAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }

        request = GetAttributesRequest.builder()
                .domainName("foobar")
                .build();
        try {
            sdb.getAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

}
