package software.amazon.awssdk.services.cloudsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.cloudsearch.model.AccessPoliciesStatus;
import software.amazon.awssdk.services.cloudsearch.model.AnalysisScheme;
import software.amazon.awssdk.services.cloudsearch.model.AnalysisSchemeLanguage;
import software.amazon.awssdk.services.cloudsearch.model.AnalysisSchemeStatus;
import software.amazon.awssdk.services.cloudsearch.model.BuildSuggestersRequest;
import software.amazon.awssdk.services.cloudsearch.model.CreateDomainRequest;
import software.amazon.awssdk.services.cloudsearch.model.CreateDomainResponse;
import software.amazon.awssdk.services.cloudsearch.model.DefineAnalysisSchemeRequest;
import software.amazon.awssdk.services.cloudsearch.model.DefineExpressionRequest;
import software.amazon.awssdk.services.cloudsearch.model.DefineIndexFieldRequest;
import software.amazon.awssdk.services.cloudsearch.model.DefineSuggesterRequest;
import software.amazon.awssdk.services.cloudsearch.model.DefineSuggesterResponse;
import software.amazon.awssdk.services.cloudsearch.model.DeleteDomainRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeAnalysisSchemesRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeAnalysisSchemesResponse;
import software.amazon.awssdk.services.cloudsearch.model.DescribeDomainsRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeDomainsResponse;
import software.amazon.awssdk.services.cloudsearch.model.DescribeExpressionsRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeExpressionsResponse;
import software.amazon.awssdk.services.cloudsearch.model.DescribeIndexFieldsRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeIndexFieldsResponse;
import software.amazon.awssdk.services.cloudsearch.model.DescribeScalingParametersRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeScalingParametersResponse;
import software.amazon.awssdk.services.cloudsearch.model.DescribeServiceAccessPoliciesRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeServiceAccessPoliciesResponse;
import software.amazon.awssdk.services.cloudsearch.model.DescribeSuggestersRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeSuggestersResponse;
import software.amazon.awssdk.services.cloudsearch.model.DocumentSuggesterOptions;
import software.amazon.awssdk.services.cloudsearch.model.DomainStatus;
import software.amazon.awssdk.services.cloudsearch.model.Expression;
import software.amazon.awssdk.services.cloudsearch.model.ExpressionStatus;
import software.amazon.awssdk.services.cloudsearch.model.IndexDocumentsRequest;
import software.amazon.awssdk.services.cloudsearch.model.IndexField;
import software.amazon.awssdk.services.cloudsearch.model.IndexFieldStatus;
import software.amazon.awssdk.services.cloudsearch.model.IndexFieldType;
import software.amazon.awssdk.services.cloudsearch.model.ListDomainNamesRequest;
import software.amazon.awssdk.services.cloudsearch.model.ListDomainNamesResponse;
import software.amazon.awssdk.services.cloudsearch.model.PartitionInstanceType;
import software.amazon.awssdk.services.cloudsearch.model.ScalingParameters;
import software.amazon.awssdk.services.cloudsearch.model.Suggester;
import software.amazon.awssdk.services.cloudsearch.model.SuggesterStatus;
import software.amazon.awssdk.services.cloudsearch.model.TextOptions;
import software.amazon.awssdk.services.cloudsearch.model.UpdateScalingParametersRequest;
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
    public static String POLICY = "{\"Statement\": " + "[ "
                                  + "{\"Effect\":\"Allow\", " + "\"Action\":\"*\", "
                                  + "\"Condition\": " + "{ " + "\"IpAddress\": "
                                  + "{ \"aws:SourceIp\": [\"203.0.113.1/32\"]} "
                                  + "} " + "}" + "] " + "}";
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
    public static void setUp() throws Exception {
        cloudSearch = CloudSearchClient.builder().credentialsProvider(StaticCredentialsProvider.create(getCredentials())).build();
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

    /**
     * Test the Define Index Fields API. Asserts that the list of index fields
     * initially in the domain is ZERO. Creates a new index field for every
     * index field type mentioned in the enum
     * <code>com.amazonaws.services.cloudsearch.model.IndexFieldType<code>.
     *
     * Asserts that the number of index fields created is same as the number of the enum type mentioned.
     */
    @Test
    public void testIndexFields() {

        String indexFieldName = null;
        DescribeIndexFieldsRequest describeIndexFieldRequest = DescribeIndexFieldsRequest.builder()
                                                                                         .domainName(testDomainName).build();

        DescribeIndexFieldsResponse result = cloudSearch.describeIndexFields(describeIndexFieldRequest);

        assertTrue(result.indexFields().size() == 0);

        IndexField field = null;
        DefineIndexFieldRequest.Builder defineIndexFieldRequest = DefineIndexFieldRequest.builder()
                                                                                         .domainName(testDomainName);
        for (IndexFieldType type : IndexFieldType.knownValues()) {
            indexFieldName = type.toString();
            indexFieldName = indexFieldName.replaceAll("-", "");
            field = IndexField.builder().indexFieldType(type)
                              .indexFieldName(indexFieldName + "indexfield").build();
            defineIndexFieldRequest.indexField(field);
            cloudSearch.defineIndexField(defineIndexFieldRequest.build());
        }

        result = cloudSearch.describeIndexFields(describeIndexFieldRequest.toBuilder().deployed(false).build());
        List<IndexFieldStatus> indexFieldStatusList = result.indexFields();
        assertTrue(indexFieldStatusList.size() == IndexFieldType.knownValues().size());
    }

    /**
     * Tests the Define Expressions API. Asserts that the list of expressions in
     * the domain is ZERO. Creates a new Expression. Asserts that the Describe
     * Expression API returns the Expression created.
     */
    @Test
    public void testExpressions() {
        DescribeExpressionsRequest describeExpressionRequest = DescribeExpressionsRequest.builder()
                                                                                         .domainName(testDomainName).build();

        DescribeExpressionsResponse describeExpressionResult = cloudSearch
                .describeExpressions(describeExpressionRequest);

        assertTrue(describeExpressionResult.expressions().size() == 0);

        Expression expression = Expression.builder().expressionName(
                testExpressionName).expressionValue("1").build();
        cloudSearch.defineExpression(DefineExpressionRequest.builder()
                                                            .domainName(testDomainName).expression(expression).build());

        describeExpressionResult = cloudSearch
                .describeExpressions(describeExpressionRequest);
        List<ExpressionStatus> expressionStatus = describeExpressionResult
                .expressions();
        assertTrue(expressionStatus.size() == 1);

        Expression expressionRetrieved = expressionStatus.get(0).options();
        assertEquals(expression.expressionName(),
                     expressionRetrieved.expressionName());
        assertEquals(expression.expressionValue(),
                     expressionRetrieved.expressionValue());
    }

    /**
     * Tests the Define Suggesters API. Asserts that the number of suggesters is
     * ZERO initially in the domain. Creates a suggester for an text field and
     * asserts if the number of suggesters is 1 after creation. Builds the
     * suggesters into the domain and asserts that the domain status is in
     * "Processing" state.
     */
    @Test
    public void testSuggestors() {
        DescribeSuggestersRequest describeSuggesterRequest = DescribeSuggestersRequest.builder()
                                                                                      .domainName(testDomainName).build();
        DescribeSuggestersResponse describeSuggesterResult = cloudSearch
                .describeSuggesters(describeSuggesterRequest);

        assertTrue(describeSuggesterResult.suggesters().size() == 0);

        DefineIndexFieldRequest defineIndexFieldRequest = DefineIndexFieldRequest.builder()
                                                                                 .domainName(testDomainName)
                                                                                 .indexField(
                                                                                         IndexField.builder()
                                                                                                   .indexFieldName(testIndexName)
                                                                                                   .indexFieldType(
                                                                                                           IndexFieldType.TEXT)
                                                                                                   .textOptions(
                                                                                                           TextOptions.builder()
                                                                                                                      .analysisScheme(
                                                                                                                              "_en_default_")
                                                                                                                      .build())
                                                                                                   .build()).build();
        cloudSearch.defineIndexField(defineIndexFieldRequest);

        DocumentSuggesterOptions suggesterOptions = DocumentSuggesterOptions.builder()
                                                                            .sourceField(testIndexName).sortExpression("1")
                                                                            .build();
        Suggester suggester = Suggester.builder().suggesterName(
                testSuggesterName).documentSuggesterOptions(
                suggesterOptions).build();
        DefineSuggesterRequest defineSuggesterRequest = DefineSuggesterRequest.builder()
                                                                              .domainName(testDomainName).suggester(suggester)
                                                                              .build();
        DefineSuggesterResponse defineSuggesterResult = cloudSearch
                .defineSuggester(defineSuggesterRequest);
        SuggesterStatus status = defineSuggesterResult.suggester();
        assertNotNull(status);
        assertNotNull(status.options());
        assertEquals(status.options().suggesterName(), testSuggesterName);

        describeSuggesterResult = cloudSearch
                .describeSuggesters(describeSuggesterRequest);
        assertTrue(describeSuggesterResult.suggesters().size() == 1);

        cloudSearch.buildSuggesters(BuildSuggestersRequest.builder()
                                                          .domainName(testDomainName).build());
        DescribeDomainsResponse describeDomainsResult = cloudSearch
                .describeDomains(DescribeDomainsRequest.builder()
                                                       .domainNames(testDomainName).build());
        DomainStatus domainStatus = describeDomainsResult.domainStatusList()
                                                         .get(0);
        assertTrue(domainStatus.processing());
    }

    /**
     * Tests the Define Analysis Scheme API. Asserts that the number of analysis
     * scheme in a newly created domain is ZERO. Creates an new analysis scheme
     * for the domain. Creates a new index field and associates the analysis
     * scheme with the field. Asserts that the number of analysis scheme is ONE
     * and the matches the analysis scheme retrieved with the one created. Also
     * asserts if the describe index field API returns the index field that has
     * the analysis scheme linked.
     */
    @Test
    public void testAnalysisSchemes() {
        DescribeAnalysisSchemesRequest describeAnalysisSchemesRequest = DescribeAnalysisSchemesRequest.builder()
                                                                                                      .domainName(testDomainName)
                                                                                                      .build();
        DescribeAnalysisSchemesResponse describeAnalysisSchemesResult = cloudSearch
                .describeAnalysisSchemes(describeAnalysisSchemesRequest);
        assertTrue(describeAnalysisSchemesResult.analysisSchemes().size() == 0);

        AnalysisScheme analysisScheme = AnalysisScheme.builder()
                                                      .analysisSchemeName(testAnalysisSchemeName)
                                                      .analysisSchemeLanguage(AnalysisSchemeLanguage.AR).build();
        cloudSearch.defineAnalysisScheme(DefineAnalysisSchemeRequest.builder()
                                                                    .domainName(testDomainName).analysisScheme(
                        analysisScheme).build());

        ;

        DefineIndexFieldRequest defineIndexFieldRequest =
                DefineIndexFieldRequest.builder()
                                       .domainName(testDomainName)
                                       .indexField(IndexField.builder()
                                                             .indexFieldName(testIndexName)
                                                             .indexFieldType(IndexFieldType.TEXT)
                                                             .textOptions(TextOptions.builder()
                                                                                     .analysisScheme(testAnalysisSchemeName)
                                                                                     .build()).build()).build();
        cloudSearch.defineIndexField(defineIndexFieldRequest);

        describeAnalysisSchemesResult = cloudSearch.describeAnalysisSchemes(describeAnalysisSchemesRequest);
        assertTrue(describeAnalysisSchemesResult.analysisSchemes().size() == 1);

        AnalysisSchemeStatus schemeStatus = describeAnalysisSchemesResult
                .analysisSchemes().get(0);
        assertEquals(schemeStatus.options().analysisSchemeName(),
                     testAnalysisSchemeName);
        assertEquals(schemeStatus.options().analysisSchemeLanguage(),
                     AnalysisSchemeLanguage.AR);

        DescribeIndexFieldsResponse describeIndexFieldsResult = cloudSearch
                .describeIndexFields(DescribeIndexFieldsRequest.builder()
                                                               .domainName(testDomainName).fieldNames(
                                testIndexName).build());
        IndexFieldStatus status = describeIndexFieldsResult.indexFields()
                                                           .get(0);
        TextOptions textOptions = status.options().textOptions();
        assertEquals(textOptions.analysisScheme(), testAnalysisSchemeName);

    }

    /**
     * Tests the Scaling Parameters API. Updates the scaling parameters for the
     * domain. Retrieves the scaling parameters and checks if it is the same.
     */
    @Test
    public void testScalingParameters() {
        ScalingParameters scalingParameters = ScalingParameters.builder()
                                                               .desiredInstanceType(PartitionInstanceType.SEARCH_M1_SMALL)
                                                               .desiredReplicationCount(5)
                                                               .desiredPartitionCount(5).build();
        cloudSearch.updateScalingParameters(UpdateScalingParametersRequest.builder()
                                                                          .domainName(testDomainName)
                                                                          .scalingParameters(scalingParameters).build());

        DescribeScalingParametersResponse describeScalingParametersResult = cloudSearch
                .describeScalingParameters(DescribeScalingParametersRequest.builder()
                                                                           .domainName(testDomainName).build());
        ScalingParameters retrievedScalingParameters = describeScalingParametersResult
                .scalingParameters().options();
        assertEquals(retrievedScalingParameters.desiredInstanceType(),
                     scalingParameters.desiredInstanceType());
        assertEquals(retrievedScalingParameters.desiredReplicationCount(),
                     scalingParameters.desiredReplicationCount());
        assertEquals(retrievedScalingParameters.desiredPartitionCount(),
                     scalingParameters.desiredPartitionCount());
    }

}
