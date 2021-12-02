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

package software.amazon.awssdk.services.elasticsearch;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.elasticsearch.model.AddTagsRequest;
import software.amazon.awssdk.services.elasticsearch.model.CreateElasticsearchDomainRequest;
import software.amazon.awssdk.services.elasticsearch.model.DeleteElasticsearchDomainRequest;
import software.amazon.awssdk.services.elasticsearch.model.DescribeElasticsearchDomainConfigRequest;
import software.amazon.awssdk.services.elasticsearch.model.DescribeElasticsearchDomainRequest;
import software.amazon.awssdk.services.elasticsearch.model.DescribeElasticsearchDomainsRequest;
import software.amazon.awssdk.services.elasticsearch.model.DomainInfo;
import software.amazon.awssdk.services.elasticsearch.model.EBSOptions;
import software.amazon.awssdk.services.elasticsearch.model.ElasticsearchDomainConfig;
import software.amazon.awssdk.services.elasticsearch.model.ElasticsearchDomainStatus;
import software.amazon.awssdk.services.elasticsearch.model.ListDomainNamesRequest;
import software.amazon.awssdk.services.elasticsearch.model.ListTagsRequest;
import software.amazon.awssdk.services.elasticsearch.model.Tag;
import software.amazon.awssdk.services.elasticsearch.model.VolumeType;
import software.amazon.awssdk.testutils.service.AwsTestBase;


public class ServiceIntegrationTest extends AwsTestBase {

    private static ElasticsearchClient es;

    private static final String DOMAIN_NAME = "java-es-test-" + System.currentTimeMillis();

    @BeforeClass
    public static void setup() throws IOException {
        setUpCredentials();
        es = ElasticsearchClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @AfterClass
    public static void tearDown() {
        es.deleteElasticsearchDomain(DeleteElasticsearchDomainRequest.builder().domainName(DOMAIN_NAME).build());
    }

    @Test
    public void testOperations() {

        String domainArn = testCreateDomain();

        testListDomainNames();

        testDescribeDomain();
        testDescribeDomains();
        testDescribeDomainConfig();

        testAddAndListTags(domainArn);

    }

    private String testCreateDomain() {
        ElasticsearchDomainStatus status = es.createElasticsearchDomain(
                CreateElasticsearchDomainRequest
                        .builder()
                        .ebsOptions(EBSOptions.builder()
                                              .ebsEnabled(true)
                                              .volumeSize(10)
                                              .volumeType(VolumeType.STANDARD)
                                              .build())
                        .domainName(DOMAIN_NAME)
                        .build()).domainStatus();

        assertEquals(DOMAIN_NAME, status.domainName());
        assertValidDomainStatus(status);

        return status.arn();
    }

    private void testDescribeDomain() {
        ElasticsearchDomainStatus status = es.describeElasticsearchDomain(
                DescribeElasticsearchDomainRequest.builder()
                        .domainName(DOMAIN_NAME).build()).domainStatus();
        assertEquals(DOMAIN_NAME, status.domainName());
        assertValidDomainStatus(status);
    }

    private void testDescribeDomains() {
        ElasticsearchDomainStatus status = es
                .describeElasticsearchDomains(
                        DescribeElasticsearchDomainsRequest.builder()
                                .domainNames(DOMAIN_NAME).build())
                .domainStatusList().get(0);
        assertEquals(DOMAIN_NAME, status.domainName());
        assertValidDomainStatus(status);
    }

    private void testListDomainNames() {
        List<String> domainNames = toDomainNameList(es.listDomainNames(
                ListDomainNamesRequest.builder().build()).domainNames());
        assertThat(domainNames, hasItem(DOMAIN_NAME));
    }

    private List<String> toDomainNameList(Collection<DomainInfo> domainInfos) {
        List<String> names = new LinkedList<String>();
        for (DomainInfo info : domainInfos) {
            names.add(info.domainName());
        }
        return names;
    }

    private void testDescribeDomainConfig() {
        ElasticsearchDomainConfig config = es
                .describeElasticsearchDomainConfig(
                        DescribeElasticsearchDomainConfigRequest.builder()
                                .domainName(DOMAIN_NAME).build()).domainConfig();
        assertValidDomainConfig(config);
    }

    private void testAddAndListTags(String arn) {
        Tag tag = Tag.builder().key("name").value("foo").build();

        es.addTags(AddTagsRequest.builder().arn(arn).tagList(tag).build());

        List<Tag> tags = es.listTags(ListTagsRequest.builder().arn(arn).build()).tagList();
        assertThat(tags, hasItem(tag));
    }

    private void assertValidDomainStatus(ElasticsearchDomainStatus status) {
        assertTrue(status.created());
        assertNotNull(status.arn());
        assertNotNull(status.accessPolicies());
        assertNotNull(status.advancedOptions());
        assertNotNull(status.domainId());
        assertNotNull(status.ebsOptions());
        assertNotNull(status.elasticsearchClusterConfig());
        assertNotNull(status.snapshotOptions());
    }

    private void assertValidDomainConfig(ElasticsearchDomainConfig config) {
        assertNotNull(config.accessPolicies());
        assertNotNull(config.advancedOptions());
        assertNotNull(config.ebsOptions());
        assertNotNull(config.elasticsearchClusterConfig());
        assertNotNull(config.snapshotOptions());
    }

}
