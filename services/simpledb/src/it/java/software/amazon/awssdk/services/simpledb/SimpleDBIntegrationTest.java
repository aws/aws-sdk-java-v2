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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.services.simpledb.model.Attribute;
import software.amazon.awssdk.services.simpledb.model.AttributeDoesNotExistException;
import software.amazon.awssdk.services.simpledb.model.BatchDeleteAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.BatchPutAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.DeleteAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.DomainMetadataRequest;
import software.amazon.awssdk.services.simpledb.model.DomainMetadataResponse;
import software.amazon.awssdk.services.simpledb.model.GetAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.GetAttributesResponse;
import software.amazon.awssdk.services.simpledb.model.ListDomainsRequest;
import software.amazon.awssdk.services.simpledb.model.ListDomainsResponse;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;
import software.amazon.awssdk.services.simpledb.model.PutAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.ReplaceableAttribute;
import software.amazon.awssdk.services.simpledb.model.ReplaceableItem;
import software.amazon.awssdk.services.simpledb.model.SelectRequest;
import software.amazon.awssdk.services.simpledb.model.SelectResponse;
import software.amazon.awssdk.services.simpledb.model.UpdateCondition;

/**
 * Integration test for the Amazon SimpleDB Java client library. Runs through a test case that calls
 * all SimpleDB operations, starting with creating a new domain, then modifying the data in that
 * domain, running queries, and finally deleting the domain at the end of the test.
 *
 * @author fulghum@amazon.com
 */
public class SimpleDBIntegrationTest extends IntegrationTestBase {

    /**
     * All test data used in these integration tests.
     */
    private static final List<ReplaceableItem> ALL_TEST_DATA = newReplaceableItemList(new ReplaceableItem[] {
            ReplaceableItem.builder().name("foo").attributes(newReplaceableAttributeList(new ReplaceableAttribute[] {
                    ReplaceableAttribute.builder().name("1").value("2").replace(Boolean.TRUE).build(),
                    ReplaceableAttribute.builder().name("3").value("4").replace(Boolean.TRUE).build(),
                    ReplaceableAttribute.builder().name("5").value("6").replace(Boolean.TRUE).build()})).build(),
            ReplaceableItem.builder().name("boo").attributes(newReplaceableAttributeList(new ReplaceableAttribute[] {
                    ReplaceableAttribute.builder().name("X").value("Y").replace(Boolean.TRUE).build(),
                    ReplaceableAttribute.builder().name("Z").value("Q").replace(Boolean.TRUE).build()})).build(),
            ReplaceableItem.builder().name("baa").attributes(newReplaceableAttributeList(new ReplaceableAttribute[] {
                    ReplaceableAttribute.builder().name("A").value("B").replace(Boolean.TRUE).build(),
                    ReplaceableAttribute.builder().name("C").value("D").replace(Boolean.TRUE).build(),
                    ReplaceableAttribute.builder().name("E").value("F").replace(Boolean.TRUE).build()})).build()});
    /**
     * Sample item, named foo, with a few attributes, for all tests to use, particularly the
     * PutAttributes test code.
     */
    private static final ReplaceableItem FOO_ITEM = (ReplaceableItem) ALL_TEST_DATA.get(0);
    /**
     * List of two sample items with attributes for all tests to use, particularly the
     * BatchPutAttributes test code.
     */
    private static final List<ReplaceableItem> ITEM_LIST = newReplaceableItemList(new ReplaceableItem[] {
            (ReplaceableItem) ALL_TEST_DATA.get(1), (ReplaceableItem) ALL_TEST_DATA.get(2)});
    /**
     * Name of the domain used for all the integration tests.
     */
    private static String domainName = "createDomainIntegrationTest-" + new Date().getTime();

    /**
     * Responsible for cleaning up after all the integration tests, including deleting any data that
     * was created.
     */
    @AfterClass
    public static void tearDown() {
        try {
            deleteDomain(domainName);
        } catch (NoSuchDomainException e) {
            // Ignored or expected.
        }
    }

    /**
     * Runs through a series of service calls on Amazon SimpleDB, which hit all the operations
     * available in the SimpleDB client. Since the operations depend on the results of each other,
     * we need to run them in a certain order, so this method organizes the overall structure of the
     * tests.
     */
    @Test
    public void testSimpleDB() throws Exception {
        gotestOverriddenRequestCredentials();

        gotestCreateDomain();

        gotestPutAttributesWithNonMatchingUpdateCondition();
        gotestPutAttributes();
        gotestBatchPutAttributes();

        gotestGetAttributes();
        gotestSelect();

        gotestListDomains();
        gotestDomainMetadata();

        gotestEmptyStringValues();
        gotestNewlineValues();

        gotestDeleteAttributesWithNonMatchingUpdateCondition();
        gotestDeleteAttributes();
        gotestBatchDeleteAttributes();
        gotestDeleteDomain();
    }

    /*
     * Helper methods for individual components of the overall integration test. Note that these
     * aren't annotated as @Test methods, since we need to control the order in which they run, so
     * we call them directly in order from the real test method.
     */

    /**
     * Tests that overridden request credentials are correctly used when specified.
     */
    private void gotestOverriddenRequestCredentials() throws Exception {
        sdb.listDomains(ListDomainsRequest.builder().build());

        ListDomainsRequest listDomainsRequest = ListDomainsRequest.builder().build();
        listDomainsRequest.setRequestCredentials(new AwsCredentials("foo", "bar"));
        try {
            sdb.listDomains(listDomainsRequest);
            fail("Expected an authentication exception from bogus request credentials.");
        } catch (Exception e) {
            // Ignored or expected.
        }
    }

    /**
     * Tests that empty string values are correct represented as the empty string, and not null.
     */
    private void gotestEmptyStringValues() throws Exception {
        ReplaceableAttribute emptyValueAttribute = ReplaceableAttribute.builder().name("empty").value("").build();
        PutAttributesRequest request = PutAttributesRequest.builder().domainName(domainName).itemName("emptyStringTestItem")
                .attributes(new ReplaceableAttribute[] {emptyValueAttribute}).build();
        sdb.putAttributes(request);

        List<Attribute> attributes = sdb.getAttributes(GetAttributesRequest.builder()
                .domainName(domainName)
                .itemName("emptyStringTestItem")
                .consistentRead(Boolean.TRUE).build())
                .attributes();

        assertEquals(1, attributes.size());
        assertEquals("empty", ((Attribute) attributes.get(0)).name());
        assertNotNull(((Attribute) attributes.get(0)).value());
        assertEquals("", ((Attribute) attributes.get(0)).value());
    }

    /**
     * Tests that values containing newlines are parsed correctly. If character coalescing isn't
     * enabled on the XML parser, then an element's character data could be split into multiple
     * character events and we could potentially lose one.
     */
    private void gotestNewlineValues() throws Exception {
        String value = "foo\nbar\nbaz";
        ReplaceableAttribute newlineValueAttribute = ReplaceableAttribute.builder().name("newline").value(value).build();
        PutAttributesRequest request = PutAttributesRequest.builder().domainName(domainName).itemName("newlineTestItem")
                .attributes(new ReplaceableAttribute[] {newlineValueAttribute}).build();
        sdb.putAttributes(request);

        List<Attribute> attributes = sdb.getAttributes(
                GetAttributesRequest.builder().domainName(domainName).itemName("newlineTestItem").consistentRead(Boolean.TRUE).build())
                                        .attributes();

        assertEquals(1, attributes.size());
        assertEquals("newline", ((Attribute) attributes.get(0)).name());
        assertNotNull(((Attribute) attributes.get(0)).value());
        assertEquals(value, ((Attribute) attributes.get(0)).value());
    }

    /**
     * Tests that the createDomain operation correctly creates a new domain.
     */
    private void gotestCreateDomain() {
        assertFalse(doesDomainExist(domainName));
        createDomain(domainName);

        // Wait a few seconds for eventual consistency
        try {
            Thread.sleep(1000 * 5);
        } catch (Exception e) {
            // Ignored or expected.
        }
        assertTrue(doesDomainExist(domainName));
    }

    /**
     * Tests that the deleteDomain operation correctly deletes a domain.
     */
    private void gotestDeleteDomain() {
        deleteDomain(domainName);
        assertFalse(doesDomainExist(domainName));
    }

    /**
     * Tests that the listDomains operation correctly returns a list of domains that includes the
     * domain we previously created in this test.
     */
    private void gotestListDomains() {
        ListDomainsResponse listDomainsResult = sdb.listDomains(ListDomainsRequest.builder().build());
        List<String> domainNames = listDomainsResult.domainNames();
        assertTrue(domainNames.contains(domainName));
    }

    /**
     * Tests that after calling the PutAttributes operation to add attributes of an item to a
     * domain, the attributes are correctly stored in the domain.
     */
    private void gotestPutAttributes() {
        PutAttributesRequest request = PutAttributesRequest.builder()
                .domainName(domainName)
                .itemName(FOO_ITEM.name())
                .attributes(FOO_ITEM.attributes())
                .build();
        sdb.putAttributes(request);

        assertItemsStoredInDomain(sdb, newReplaceableItemList(new ReplaceableItem[] {FOO_ITEM}), domainName);
    }

    /**
     * Tests that attributes are not added to an item in a domain when PutAttributes is called with
     * an UpdateCondition that isn't satisfied.
     */
    private void gotestPutAttributesWithNonMatchingUpdateCondition() {
        PutAttributesRequest request = PutAttributesRequest.builder()
                .domainName(domainName)
                .itemName(FOO_ITEM.name())
                .attributes(FOO_ITEM.attributes())
                .expected(UpdateCondition.builder().exists(Boolean.TRUE).name("non-existant-attribute-name")
                                                 .value("non-existant-attribute-value").build())
                .build();

        try {
            sdb.putAttributes(request);
            fail("Expected an AttributeDoesNotExist error code, but didn't received one");
        } catch (AttributeDoesNotExistException e) {
            assertEquals("AttributeDoesNotExist", e.getErrorCode());
        }
    }

    /**
     * Tests that after calling the BatchPutAttributes operation to add items to a domain, the items
     * and attributes are correctly stored in the domain.
     */
    private void gotestBatchPutAttributes() {
        BatchPutAttributesRequest request = BatchPutAttributesRequest.builder()
                .domainName(domainName)
                .items(ITEM_LIST)
                .build();
        sdb.batchPutAttributes(request);

        assertItemsStoredInDomain(sdb, ITEM_LIST, domainName);
    }

    /**
     * Tests that selecting data from a domain returns the expected items and attributes.
     */
    private void gotestSelect() {
        // First try to select without consistent reads...
        SelectRequest request = SelectRequest.builder()
                .selectExpression("select * from `" + domainName + "`")
                .build();
        SelectResponse selectResult = sdb.select(request);
        assertNull(selectResult.nextToken());
        assertItemsPresent(ITEM_LIST, selectResult.items());
        assertItemsPresent(newReplaceableItemList(new ReplaceableItem[] {FOO_ITEM}), selectResult.items());

        // Try again with the consistent read parameter enabled...
        sdb.select(request.toBuilder().consistentRead(Boolean.TRUE).build());
        selectResult = sdb.select(request);
        assertNull(selectResult.nextToken());
        assertItemsPresent(ITEM_LIST, selectResult.items());
        assertItemsPresent(newReplaceableItemList(new ReplaceableItem[] {FOO_ITEM}), selectResult.items());
    }

    /**
     * Tests that the domain metadata for the domain we created previously matches what we've put
     * into the domain.
     */
    private void gotestDomainMetadata() {
        DomainMetadataRequest request = DomainMetadataRequest.builder()
                .domainName(domainName)
                .build();
        DomainMetadataResponse domainMetadataResult = sdb.domainMetadata(request);

        int expectedItemCount = 0;
        int expectedAttributeValueCount = 0;
        int expectedAttributeNameCount = 0;
        for (Iterator iterator = ALL_TEST_DATA.iterator(); iterator.hasNext(); ) {
            ReplaceableItem item = (ReplaceableItem) iterator.next();

            expectedItemCount++;
            expectedAttributeNameCount += item.attributes().size();
            expectedAttributeValueCount += item.attributes().size();
        }

        assertEquals(expectedItemCount, domainMetadataResult.itemCount().intValue());
        assertEquals(expectedAttributeNameCount, domainMetadataResult.attributeNameCount().intValue());
        assertEquals(expectedAttributeValueCount, domainMetadataResult.attributeValueCount().intValue());
        assertNotNull(domainMetadataResult.timestamp());
    }

    /**
     * Tests that the GetAttributes operation returns the attributes we previously stored in our
     * test domain.
     */
    private void gotestGetAttributes() {
        GetAttributesRequest request = GetAttributesRequest.builder()
                .domainName(domainName)
                .itemName(FOO_ITEM.name())
                .consistentRead(Boolean.TRUE)
                .attributeNames(new String[] {(FOO_ITEM.attributes().get(0)).name(),
                                                 (FOO_ITEM.attributes().get(1)).name()})
                .build();

        GetAttributesResponse attributesResult = sdb.getAttributes(request);

        List<Attribute> attributes = attributesResult.attributes();
        Map<String, String> attributeValuesByName = convertAttributesToMap(attributes);

        assertEquals(2, attributeValuesByName.size());

        ReplaceableAttribute[] replaceableAttributes = new ReplaceableAttribute[] {
                (ReplaceableAttribute) FOO_ITEM.attributes().get(0),
                (ReplaceableAttribute) FOO_ITEM.attributes().get(1)};
        for (int index = 0; index < replaceableAttributes.length; index++) {
            ReplaceableAttribute expectedAttribute = replaceableAttributes[index];

            String expectedAttributeName = expectedAttribute.name();
            assertTrue(attributeValuesByName.containsKey(expectedAttributeName));
            assertEquals(expectedAttribute.value(), attributeValuesByName.get(expectedAttributeName));
        }
    }

    /**
     * Tests that existing attributes are correctly removed after calling the DeleteAttributes
     * operation.
     */
    private void gotestDeleteAttributes() {
        List<String> attributeNames = Arrays.asList(new String[] {
                ((ReplaceableAttribute) FOO_ITEM.attributes().get(0)).name(),
                ((ReplaceableAttribute) FOO_ITEM.attributes().get(1)).name()
        });
        List<Attribute> attributeList = new ArrayList<Attribute>();
        for (Iterator iterator = attributeNames.iterator(); iterator.hasNext(); ) {
            String attributeName = (String) iterator.next();
            attributeList.add(Attribute.builder().name(attributeName).build());
        }

        assertTrue(doAttributesExistForItem(sdb, FOO_ITEM.name(), domainName, attributeNames));

        DeleteAttributesRequest request = DeleteAttributesRequest.builder()
                .domainName(domainName)
                .itemName(FOO_ITEM.name())
                .attributes(attributeList)
                .expected(UpdateCondition.builder().exists(Boolean.FALSE).name("non-existant-attribute-name").build())
                .build();
        sdb.deleteAttributes(request);

        // Wait a few seconds for eventual consistency...
        try {
            Thread.sleep(5 * 1000);
        } catch (Exception e) {
            // Ignored or expected.
        }

        assertFalse(doAttributesExistForItem(sdb, FOO_ITEM.name(), domainName, attributeNames));
    }

    /**
     * Tests that we can correctly call BatchDeleteAttributes to delete multiple items in one call.
     */
    private void gotestBatchDeleteAttributes() {
        // Add some test data
        BatchPutAttributesRequest request = BatchPutAttributesRequest.builder()
                .domainName(domainName)
                .items(ITEM_LIST)
                .build();
        sdb.batchPutAttributes(request);
        assertItemsStoredInDomain(sdb, ITEM_LIST, domainName);

        // Delete 'em
        sdb.batchDeleteAttributes(BatchDeleteAttributesRequest.builder().domainName(domainName).items(newDeletableItemList(ITEM_LIST)).build());

        // Assert none of the items are still in the domain
        for (int i = 0; i < ITEM_LIST.size(); i++) {
            ReplaceableItem expectedItem = (ReplaceableItem) ITEM_LIST.get(i);
            assertFalse(doAttributesExistForItem(sdb, expectedItem.name(), domainName,
                                                 newAttributeNameList(expectedItem.attributes())));
        }
    }

    /**
     * Tests that attributes are not removed when delete attributes is called with an update
     * condition that isn't satisfied.
     */
    private void gotestDeleteAttributesWithNonMatchingUpdateCondition() {
        List<String> attributeNames = Arrays.asList(new String[] {
                (FOO_ITEM.attributes().get(0)).name(),
                (FOO_ITEM.attributes().get(1)).name()});
        List<Attribute> attributeList = new ArrayList<Attribute>();
        for (Iterator iterator = attributeNames.iterator(); iterator.hasNext(); ) {
            String attributeName = (String) iterator.next();
            attributeList.add(Attribute.builder().name(attributeName).build());
        }

        assertTrue(doAttributesExistForItem(sdb, FOO_ITEM.name(), domainName, attributeNames));

        DeleteAttributesRequest request = DeleteAttributesRequest.builder()
                .domainName(domainName)
                .itemName(FOO_ITEM.name())
                .attributes(attributeList)
                .expected(UpdateCondition.builder().exists(Boolean.TRUE).name("non-existant-attribute-name")
                                                 .value("non-existant-attribute-value").build()).build();

        try {
            sdb.deleteAttributes(request);
            fail("Expected an AttributeDoesNotExist error code, but didn't received one");
        } catch (AttributeDoesNotExistException e) {
            assertEquals("AttributeDoesNotExist", e.getErrorCode());
        }
    }

}
