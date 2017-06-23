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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.simpledb.model.Attribute;
import software.amazon.awssdk.services.simpledb.model.CreateDomainRequest;
import software.amazon.awssdk.services.simpledb.model.DeletableItem;
import software.amazon.awssdk.services.simpledb.model.DeleteDomainRequest;
import software.amazon.awssdk.services.simpledb.model.DomainMetadataRequest;
import software.amazon.awssdk.services.simpledb.model.GetAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.GetAttributesResponse;
import software.amazon.awssdk.services.simpledb.model.Item;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;
import software.amazon.awssdk.services.simpledb.model.ReplaceableAttribute;
import software.amazon.awssdk.services.simpledb.model.ReplaceableItem;
import software.amazon.awssdk.services.simpledb.model.SelectRequest;
import software.amazon.awssdk.services.simpledb.model.SelectResponse;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Base class for SimpleDB integration tests; responsible for loading AWS account info for running
 * the tests, and instantiating a SimpleDB client for tests to use.
 *
 * @author fulghum@amazon.com
 */
public abstract class IntegrationTestBase extends AwsTestBase {
    private static final Log log = LogFactory.getLog(IntegrationTestBase.class);
    /**
     * System property allowing users to explicitly override where the AWS account info should be
     * loaded from.
     */
    private static final String AWS_ACCOUNT_PROPERTIES_FILE = "awsTestAccountPropertiesFile";

    /** Shared SimpleDB client for all tests to use. */
    protected static SimpleDBClient sdb;

    /** Shared SimpleDB async client for tests to use. */
    protected static SimpleDBAsyncClient sdbAsync;

    /**
     * Loads the AWS account info for the integration tests and creates a SimpleDB client for tests
     * to use.
     *
     * @throws IOException
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException, Exception {
        setUpCredentials();
        sdb = SimpleDBClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(Region.US_EAST_1)
                .build();
        sdbAsync = SimpleDBAsyncClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(Region.US_EAST_1)
                .build();
    }

    /**
     * Returns the properties file from which AWS account information can be loaded. The user can
     * set a system property to explicitly tell the integration tests what file to use, otherwise
     * this method will return a file in the current user's home directory.
     *
     * @return The properties file from which AWS account information can be loaded.
     */
    private static File getAccountInfoPropertiesFile() {
        /*
         * Users can override a system property to explicitly tell us what account info properties
         * file to use when running integration tests.
         */
        String property = System.getProperty(AWS_ACCOUNT_PROPERTIES_FILE);
        if (property != null) {
            return new File(property);
        }

        /*
         * Otherwise, we'll pull it from their home directory.
         */
        return new File(System.getProperty("user.home"), ".aws/awsTestAccount.properties");
    }

    /**
     * Returns a new list of ReplaceableItems from the specified items.
     *
     * @param items
     *            The items to include in the new list.
     * @return A new list of ReplaceableItems created from the specified items.
     */
    protected static List<ReplaceableItem> newReplaceableItemList(ReplaceableItem[] items) {
        List<ReplaceableItem> newList = new ArrayList<ReplaceableItem>();
        for (int index = 0; index < items.length; index++) {
            newList.add(items[index]);
        }

        return newList;
    }

    /**
     * Returns a list of ReplaceableAttributes from the specified attributes.
     *
     * @param attributes
     *            The attributes to include in the new list.
     * @return A new list of ReplaceableAttributes created from the specified attributes.
     */
    protected static List<ReplaceableAttribute> newReplaceableAttributeList(ReplaceableAttribute[] attributes) {
        List<ReplaceableAttribute> newList = new ArrayList<ReplaceableAttribute>();
        for (int index = 0; index < attributes.length; index++) {
            newList.add(attributes[index]);
        }

        return newList;
    }

    protected static List<String> newAttributeNameList(List<ReplaceableAttribute> attributes) {
        List<String> attributeNames = new ArrayList<String>();
        for (int i = 0; i < attributes.size(); i++) {
            ReplaceableAttribute attribute = (ReplaceableAttribute) attributes.get(i);
            attributeNames.add(attribute.name());
        }

        return attributeNames;
    }

    protected static List<DeletableItem> newDeletableItemList(List<ReplaceableItem> items) {
        List<DeletableItem> deletableItems = new ArrayList<DeletableItem>();
        for (int i = 0; i < items.size(); i++) {
            ReplaceableItem replaceableItem = (ReplaceableItem) items.get(i);
            deletableItems.add(DeletableItem.builder().name(replaceableItem.name()).attributes(newAttributeList(replaceableItem
                                                                                                     .attributes())).build());
        }

        return deletableItems;
    }

    protected static List<Attribute> newAttributeList(List<ReplaceableAttribute> replaceableAttributes) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        for (int i = 0; i < replaceableAttributes.size(); i++) {
            ReplaceableAttribute replaceableAttribute = (ReplaceableAttribute) replaceableAttributes.get(i);
            attributes.add(Attribute.builder().name(replaceableAttribute.name()).value(replaceableAttribute.value()).build());
        }

        return attributes;
    }

    /**
     * Tries to delete the specified domain.
     */
    protected static void deleteDomain(String domainName) {
        DeleteDomainRequest request = DeleteDomainRequest.builder().domainName(domainName).build();
        sdb.deleteDomain(request);
    }

    /**
     * Returns true if the specified domain exists, otherwise false.
     */
    protected boolean doesDomainExist(String domainName) {
        try {
            DomainMetadataRequest request = DomainMetadataRequest.builder().domainName(domainName).build();
            sdb.domainMetadata(request);
            return true;
        } catch (NoSuchDomainException e) {
            return false;
        }
    }

    /**
     * Creates the specified domain.
     */
    protected void createDomain(String domainName) {
        CreateDomainRequest request = CreateDomainRequest.builder().domainName(domainName).build();
        sdb.createDomain(request);
    }

    /**
     * Asserts that the specified expected items are present in the specified domain. This method
     * will query the specified domain and verify that all of the expected items, and their
     * associated attributes, are present in the domain.
     *
     * @param sdb
     *            The SimpleDB client to use when checking the contents of the specified domain.
     * @param expectedItems
     *            The items expected to be in the specified domain.
     * @param domainName
     *            The name of the domain to check for the expected items.
     */
    protected void assertItemsStoredInDomain(SimpleDBClient sdb, List<ReplaceableItem> expectedItems, String domainName) {
        try {
            Thread.sleep(4 * 1000);
        } catch (InterruptedException e) {
            // Ignored or expected.
        }

        SelectRequest request = SelectRequest.builder()
                .selectExpression("select * from `" + domainName + "`")
                .consistentRead(Boolean.TRUE)
                .build();
        SelectResponse selectResult = sdb.select(request);

        assertItemsPresent(expectedItems, selectResult.items());
    }

    /**
     * Asserts that the list of expected items is contained in the list of items. The
     *
     * @param expectedItems
     *            The list of items, and their attributes, that are expected to be found in the
     *            second list of items.
     * @param items
     *            The list of items being tested.
     */
    protected void assertItemsPresent(List<ReplaceableItem> expectedItems, List<Item> items) {
        Map<String, Map<String, String>> expectedAttributesByItemName = convertReplaceableItemListToMap(expectedItems);
        Map<String, Map<String, String>> retrievedAttributesByItemName = convertItemListToMap(items);

        for (Iterator itemNameIterator = expectedAttributesByItemName.keySet().iterator(); itemNameIterator.hasNext(); ) {
            String expectedItemName = (String) itemNameIterator.next();

            assertTrue(retrievedAttributesByItemName.containsKey(expectedItemName));

            Map<String, String> expectedAttributes = (Map) expectedAttributesByItemName.get(expectedItemName);
            Map<String, String> retrievedAttributes = (Map) retrievedAttributesByItemName.get(expectedItemName);

            for (Iterator attributeNameIterator = expectedAttributes.keySet().iterator(); attributeNameIterator
                    .hasNext(); ) {
                String expectedAttributeName = (String) attributeNameIterator.next();
                String expectedAttributeValue = (String) expectedAttributes.get(expectedAttributeName);

                assertTrue(retrievedAttributes.containsKey(expectedAttributeName));
                assertEquals(expectedAttributeValue, retrievedAttributes.get(expectedAttributeName));
            }
        }
    }

    /**
     * Returns a map of attribute names to attribute values for the specified list of attributes.
     *
     * @param attributeList
     *            The list of attributes to translate to a map.
     * @return A map of attribute names to attribute values for the specified list of attributes.
     */
    protected Map<String, String> convertAttributesToMap(List<Attribute> attributeList) {
        Map<String, String> attributeValuesByName = new HashMap<String, String>();
        for (Iterator iterator = attributeList.iterator(); iterator.hasNext(); ) {
            Attribute attribute = (Attribute) iterator.next();
            /*
             * TODO: Eventually we'll want to handle multiple values for a single attribute.
             */
            attributeValuesByName.put(attribute.name(), attribute.value());
        }

        return attributeValuesByName;
    }

    /**
     * Queries SimpleDB and returns true if the specified attribute names exist for the specified
     * item in the specified domain, otherwise, returns false if any attributes aren't found.
     *
     * @param sdb
     *            The client to use when querying SimpleDB.
     * @param itemName
     *            The name of the item that should contain the specified attributes.
     * @param domainName
     *            The domain containing the expected attributes.
     * @param attributeNames
     *            The names of the expected attributes.
     * @return True if the specified attribute names exist for the specified item in the specified
     *         domain, otherwise false.
     */
    protected boolean doAttributesExistForItem(SimpleDBClient sdb,
                                               String itemName,
                                               String domainName,
                                               List<String> attributeNames) {
        GetAttributesRequest request = GetAttributesRequest.builder()
                .domainName(domainName)
                .attributeNames(attributeNames)
                .itemName(itemName)
                .consistentRead(Boolean.TRUE)
                .build();

        GetAttributesResponse result = sdb.getAttributes(request);
        Map<String, String> attributeValuesByName = convertAttributesToMap(result.attributes());

        for (Iterator iterator = attributeNames.iterator(); iterator.hasNext(); ) {
            String expectedAttributeName = (String) iterator.next();
            if (!attributeValuesByName.containsKey(expectedAttributeName)) {
                return false;
            }
        }

        return true;
    }

    /*
     * Private helper methods
     */

    /**
     * Converts a list of items into a map of item names to attribute values, where the attribute
     * values are a map of attribute name to value. Multiple values per attribute are not supported.
     *
     * @param items
     *            The items to convert.
     * @return A new map of the specified items and their attributes.
     */
    private Map<String, Map<String, String>> convertItemListToMap(List<Item> items) {
        Map<String, Map<String, String>> attributesByItemName = new HashMap<String, Map<String, String>>();

        for (Iterator iterator = items.iterator(); iterator.hasNext(); ) {
            Item item = (Item) iterator.next();
            attributesByItemName.put(item.name(), convertAttributesToMap(item.attributes()));
        }

        return attributesByItemName;
    }

    /**
     * Converts a list of items into a map of item names to attribute values, where the attribute
     * values are a map of attribute name to value. Multiple values per attribute are not supported.
     *
     * @param items
     *            The items to convert.
     * @return A new map of the specified items and their attributes.
     */
    private Map<String, Map<String, String>> convertReplaceableItemListToMap(List<ReplaceableItem> items) {
        Map<String, Map<String, String>> attributesByItemName = new HashMap<String, Map<String, String>>();

        for (Iterator itemIterator = items.iterator(); itemIterator.hasNext(); ) {
            ReplaceableItem item = (ReplaceableItem) itemIterator.next();

            Map<String, String> attributeValuesByName = new HashMap<String, String>();

            for (Iterator attributeIterator = item.attributes().iterator(); attributeIterator.hasNext(); ) {
                ReplaceableAttribute attribute = (ReplaceableAttribute) attributeIterator.next();

                // TODO: what about attributes with multiple values?
                attributeValuesByName.put(attribute.name(), attribute.value());
            }

            attributesByItemName.put(item.name(), attributeValuesByName);
        }

        return attributesByItemName;
    }

}
