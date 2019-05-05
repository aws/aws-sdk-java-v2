/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.cloudsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.cloudsearch.model.AccessPoliciesStatus;
import software.amazon.awssdk.services.cloudsearch.model.CreateDomainRequest;
import software.amazon.awssdk.services.cloudsearch.model.CreateDomainResponse;
import software.amazon.awssdk.services.cloudsearch.model.DefineIndexFieldRequest;
import software.amazon.awssdk.services.cloudsearch.model.DeleteDomainRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeDomainsRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeDomainsResponse;
import software.amazon.awssdk.services.cloudsearch.model.DescribeServiceAccessPoliciesRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeServiceAccessPoliciesResponse;
import software.amazon.awssdk.services.cloudsearch.model.DomainStatus;
import software.amazon.awssdk.services.cloudsearch.model.IndexDocumentsRequest;
import software.amazon.awssdk.services.cloudsearch.model.IndexField;
import software.amazon.awssdk.services.cloudsearch.model.IndexFieldType;
import software.amazon.awssdk.services.cloudsearch.model.ListDomainNamesRequest;
import software.amazon.awssdk.services.cloudsearch.model.ListDomainNamesResponse;
import software.amazon.awssdk.services.cloudsearch.model.UpdateServiceAccessPoliciesRequest;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class CloudSearchv2IntegrationTest extends AwsIntegrationTestBase {

    /** Name Prefix of the domains being created for test cases. */
    private static final String testDomainNamePrefix = "sdk-domain-";
    /** Name of the expression being created in the domain. */
    private static final String testExpressionName = "sdkexp"
                                                     + System.currentTimeMillis();
    /** Name of the test index being created in the domain. */
    private static final String testIndexName = "sdkindex"
                                                + System.currentTimeMillis();
    /** Name of the test suggester being created in the domain. */
    private static final String testSuggesterName = "sdksug"
                                                    + System.currentTimeMillis();
    /** Name of the test analysis scheme being created in the domain. */
    private static final String testAnalysisSchemeName = "analysis"
                                                         + System.currentTimeMillis();

    public static String POLICY = "{\n"
                                   + "  \"Statement\":[\n"
                                   + "    {\n"
                                   + "      \"Effect\":\"Allow\",\n"
                                   + "      \"Principal\":\"*\",\n"
                                   + "      \"Action\":[\"cloudsearch:search\"],\n"
                                   + "      \"Condition\":{\"IpAddress\":{\"aws:SourceIp\":\"203.0.113.1/32\"}}\n"
                                   + "    }\n"
                                   + "  ]\n"
                                   + "}";

    /** Reference to the cloud search client during the testing process. */
    private static CloudSearchClient cloudSearch = null;
    /**
     * Holds the name of the domain name at any point of time during test case
     * execution.
     */
    private static String testDomainName = null;

    /**
     * Sets up the credenitals and creates an instance of the Amazon Cloud
     * Search client used for different test case executions.
     */
    @BeforeClass
    public static void setUp() {
        cloudSearch = CloudSearchClient.builder().credentialsProvider(getCredentialsProvider()).build();
    }

    /**
     * Creates a new Amazon Cloud Search domain before every test case
     * execution. This is done to ensure that the state of domain is in
     * consistent state before and after the test case execution.
     */
    @Before
    public void createDomain() {

        testDomainName = testDomainNamePrefix + System.currentTimeMillis();

        cloudSearch.createDomain(CreateDomainRequest.builder()
                                                    .domainName(testDomainName).build());
    }

    /**
     * Deletes the Amazon Cloud Search domain after every test case execution.
     */
    @After
    public void deleteDomain() {
        cloudSearch.deleteDomain(DeleteDomainRequest.builder()
                                                    .domainName(testDomainName).build());
    }

    /**
     * Tests the create domain functionality. Checks if there are any existing
     * domains by querying using describe domains or list domain names API.
     * Creates a new domain using create domain API. Checks if the domain id,
     * name is set in the result and the domain name matches the name used
     * during creation. Also checks if the state of the domain in Created State.
     * Since this domain is created locally for this test case, it is deleted in
     * the finally block.
     */
    @Test
    public void testCreateDomains() {

        CreateDomainResponse createDomainResult = null;
        String domainName = "test-" + System.currentTimeMillis();
        try {
            DescribeDomainsResponse describeDomainResult = cloudSearch
                    .describeDomains(DescribeDomainsRequest.builder().build());
            ListDomainNamesResponse listDomainNamesResult = cloudSearch
                    .listDomainNames(ListDomainNamesRequest.builder().build());

            assertTrue(describeDomainResult.domainStatusList().size() >= 0);
            assertTrue(listDomainNamesResult.domainNames().size() >= 0);

            createDomainResult = cloudSearch
                    .createDomain(CreateDomainRequest.builder()
                                                     .domainName(domainName).build());

            describeDomainResult = cloudSearch
                    .describeDomains(DescribeDomainsRequest.builder()
                                                           .domainNames(domainName).build());
            DomainStatus domainStatus = describeDomainResult
                    .domainStatusList().get(0);

            assertTrue(domainStatus.created());
            assertFalse(domainStatus.deleted());
            assertNotNull(domainStatus.arn());
            assertEquals(domainStatus.domainName(), domainName);
            assertNotNull(domainStatus.domainId());
            assertTrue(domainStatus.processing());
        } finally {
            if (createDomainResult != null) {
                cloudSearch.deleteDomain(DeleteDomainRequest.builder()
                                                            .domainName(domainName).build());
            }
        }

    }

    /**
     * Tests the Index Documents API. Asserts that the status of the domain is
     * initially in the "RequiresIndexDocuments" state. After an index document
     * request is initiated, the status must be updated to "Processing" state.
     * Status is retrieved using the Describe Domains API
     */
    @Test
    public void testIndexDocuments() {

        IndexField indexField = IndexField.builder().indexFieldName(
                testIndexName).indexFieldType(IndexFieldType.LITERAL).build();

        cloudSearch.defineIndexField(DefineIndexFieldRequest.builder()
                                                            .domainName(testDomainName).indexField(indexField).build());
        DescribeDomainsResponse describeDomainResult = cloudSearch
                .describeDomains(DescribeDomainsRequest.builder()
                                                       .domainNames(testDomainName).build());
        DomainStatus status = describeDomainResult.domainStatusList().get(0);
        assertTrue(status.requiresIndexDocuments());

        cloudSearch.indexDocuments(IndexDocumentsRequest.builder()
                                                        .domainName(testDomainName).build());
        status = describeDomainResult.domainStatusList().get(0);
        assertTrue(status.processing());

    }

    /**
     * Tests the Access Policies API. Updates an Access Policy for the domain.
     * Retrieves the access policy and checks if the access policy retrieved is
     * same as the one updated.
     */
    @Test
    public void testAccessPolicies() {

        AccessPoliciesStatus accessPoliciesStatus = null;
        Instant yesterday = Instant.now().minus(Duration.ofDays(1));

        DescribeDomainsResponse describeDomainResult = cloudSearch
                .describeDomains(DescribeDomainsRequest.builder()
                                                       .domainNames(testDomainName).build());

        POLICY = POLICY.replaceAll("ARN", describeDomainResult
                .domainStatusList().get(0).arn());
        cloudSearch
                .updateServiceAccessPolicies(UpdateServiceAccessPoliciesRequest.builder()
                                                                               .domainName(testDomainName).accessPolicies(
                                POLICY).build());
        DescribeServiceAccessPoliciesResponse accessPolicyResult = cloudSearch
                .describeServiceAccessPolicies(DescribeServiceAccessPoliciesRequest.builder()
                                                                                   .domainName(testDomainName).build());
        accessPoliciesStatus = accessPolicyResult.accessPolicies();

        assertNotNull(accessPoliciesStatus);
        assertTrue(yesterday.isBefore(
                accessPoliciesStatus.status().creationDate()));
        assertTrue(yesterday.isBefore(
                accessPoliciesStatus.status().updateDate()));
        assertTrue(accessPoliciesStatus.options().length() > 0);
        assertNotNull(accessPoliciesStatus.status().state());

    }
}
