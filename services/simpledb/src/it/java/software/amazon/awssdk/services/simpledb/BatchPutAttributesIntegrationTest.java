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
import software.amazon.awssdk.services.simpledb.model.BatchPutAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.DuplicateItemNameException;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;
import software.amazon.awssdk.services.simpledb.model.ReplaceableAttribute;
import software.amazon.awssdk.services.simpledb.model.ReplaceableItem;

/**
 * Integration tests for the exceptional cases of the SimpleDB BatchPutAttributes operation.
 *
 * @author fulghum@amazon.com
 */
public class BatchPutAttributesIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that calling BatchPutAttributes with two items with the same name causes a
     * DuplicateItemNameException to be thrown.
     */
    @Test
    public void testBatchPutAttributesDuplicateItemNameException() {

        final ReplaceableItem item = ReplaceableItem.builder()
                                                    .name("foo")
                                                    .attributes(ReplaceableAttribute.builder()
                                                                                    .name("foo")
                                                                                    .value("bar")
                                                                                    .replace(Boolean.TRUE)
                                                                                    .build())
                                                    .build();
        BatchPutAttributesRequest request = BatchPutAttributesRequest.builder()
                                                                     .items(item, item)
                                                                     .build();

        try {
            sdb.batchPutAttributes(request);
            fail("Expected DuplicateItemNameException, but wasn't thrown");
        } catch (DuplicateItemNameException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that when a domain name, item name, attribute name, or attribute value isn't supplied
     * to a BatchPutAttributes call, a MissingParameterException is thrown.
     */
    @Test
    public void testBatchPutAttributesMissingParameterException() {
        ReplaceableAttribute attribute = ReplaceableAttribute.builder()
                                                             .name("foo")
                                                             .value("foo")
                                                             .replace(Boolean.TRUE)
                                                             .build();

        BatchPutAttributesRequest request = BatchPutAttributesRequest.builder()
                                                                     .items(ReplaceableItem.builder()
                                                                                           .name("foo")
                                                                                           .attributes(attribute)
                                                                                           .build())
                                                                     .build();
        try {
            sdb.batchPutAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }

        attribute = ReplaceableAttribute.builder().name("foo").replace(Boolean.TRUE).build();

        request = BatchPutAttributesRequest.builder()
                                           .items(ReplaceableItem.builder()
                                                                 .name("foo")
                                                                 .attributes(attribute)
                                                                 .build())
                                           .domainName("foo")
                                           .build();
        try {
            sdb.batchPutAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }

        attribute = ReplaceableAttribute.builder().value("bar").replace(Boolean.TRUE).build();
        request = BatchPutAttributesRequest.builder()
                                           .items(ReplaceableItem.builder().name("foo").attributes(attribute).build())
                                           .domainName("foo")
                                           .build();
        try {
            sdb.batchPutAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }

        attribute = ReplaceableAttribute.builder()
                                        .name("foo")
                                        .value("bar")
                                        .replace(Boolean.TRUE)
                                        .build();
        request = BatchPutAttributesRequest.builder()
                                           .items(ReplaceableItem.builder().attributes(attribute).build())
                                           .domainName("foo")
                                           .build();
        try {
            sdb.batchPutAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that when a non-existent domain name is specified to the BatchPutAttributes operation,
     * a NoSuchDomainException is thrown.
     */
    @Test
    public void testBatchPutAttributesNoSuchDomainException() {
        BatchPutAttributesRequest request = BatchPutAttributesRequest.builder()
                                                                     .items(ReplaceableItem.builder()
                                                                                           .name("foo")
                                                                                           .attributes(
                                                                                                   ReplaceableAttribute.builder()
                                                                                                                       .name("foo")
                                                                                                                       .value("bar")
                                                                                                                       .replace(
                                                                                                                               Boolean.TRUE)
                                                                                                                       .build())
                                                                                           .build())
                                                                     .domainName("ADomainNameThatDoesntExist")
                                                                     .build();
        try {
            sdb.batchPutAttributes(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);
        }
    }

}
