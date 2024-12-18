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

package software.amazon.awssdk.codegen.naming;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.UnderscoresInNameBehavior;
import software.amazon.awssdk.codegen.model.config.customization.ShareModelConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;

@RunWith(MockitoJUnitRunner.class)
public class DefaultNamingStrategyTest {
    private CustomizationConfig customizationConfig = CustomizationConfig.create();

    private ServiceModel serviceModel = mock(ServiceModel.class);

    @Mock
    private Map<String, Shape> mockShapeMap;

    @Mock
    private Shape mockParentShape;

    @Mock
    private Shape mockShape;

    @Mock
    private Shape mockStringShape;

    @Mock
    private Member member;

    @Mock
    private ServiceMetadata serviceMetadata;

    private DefaultNamingStrategy strat = new DefaultNamingStrategy(serviceModel, customizationConfig);

    @Before
    public void setUp() {
    }

    @Test
    public void enumNamesConvertCorrectly() {
        validateConversion("Twilio-Sms", "TWILIO_SMS");
        validateConversion("t2.micro", "T2_MICRO");
        validateConversion("GreaterThanThreshold", "GREATER_THAN_THRESHOLD");
        validateConversion("INITIALIZED", "INITIALIZED");
        validateConversion("GENERIC_EVENT", "GENERIC_EVENT");
        validateConversion("WINDOWS_2012", "WINDOWS_2012");
        validateConversion("ec2:spot-fleet-request:TargetCapacity", "EC2_SPOT_FLEET_REQUEST_TARGET_CAPACITY");
        validateConversion("elasticmapreduce:instancegroup:InstanceCount", "ELASTICMAPREDUCE_INSTANCEGROUP_INSTANCE_COUNT");
        validateConversion("application/vnd.amazonaws.card.generic", "APPLICATION_VND_AMAZONAWS_CARD_GENERIC");
        validateConversion("IPV4", "IPV4");
        validateConversion("ipv4", "IPV4");
        validateConversion("IPv4", "IP_V4");
        validateConversion("ipV4", "IP_V4");
        validateConversion("IPMatch", "IP_MATCH");
        validateConversion("S3", "S3");
        validateConversion("EC2Instance", "EC2_INSTANCE");
        validateConversion("aws.config", "AWS_CONFIG");
        validateConversion("AWS::EC2::CustomerGateway", "AWS_EC2_CUSTOMER_GATEWAY");
        validateConversion("application/pdf", "APPLICATION_PDF");
        validateConversion("ADConnector", "AD_CONNECTOR");
        validateConversion("MS-CHAPv1", "MS_CHAP_V1");
        validateConversion("One-Way: Outgoing", "ONE_WAY_OUTGOING");
        validateConversion("scram_sha_1", "SCRAM_SHA_1");
        validateConversion("EC_prime256v1", "EC_PRIME256_V1");
        validateConversion("EC_PRIME256V1", "EC_PRIME256_V1");
        validateConversion("EC2v11.4", "EC2_V11_4");
        validateConversion("nodejs4.3-edge", "NODEJS4_3_EDGE");
        validateConversion("BUILD_GENERAL1_SMALL", "BUILD_GENERAL1_SMALL");
        validateConversion("SSE_S3", "SSE_S3");
        validateConversion("http1.1", "HTTP1_1");
        validateConversion("T100", "T100");
        validateConversion("s3:ObjectCreated:*", "S3_OBJECT_CREATED");
        validateConversion("s3:ObjectCreated:Put", "S3_OBJECT_CREATED_PUT");
        validateConversion("TLSv1", "TLS_V1");
        validateConversion("TLSv1.2", "TLS_V1_2");
        validateConversion("us-east-1", "US_EAST_1");
        validateConversion("io1", "IO1");
        validateConversion("testNESTEDAcronym", "TEST_NESTED_ACRONYM");
        validateConversion("IoT", "IOT");
        validateConversion("textORcsv", "TEXT_OR_CSV");
        validateConversion("__foo___", "FOO");
        validateConversion("TEST__FOO", "TEST_FOO");
        validateConversion("IFrame", "I_FRAME");
        validateConversion("TPain", "T_PAIN");
        validateConversion("S3EC2", "S3_EC2");
        validateConversion("S3Ec2", "S3_EC2");
        validateConversion("s3Ec2", "S3_EC2");
        validateConversion("s3ec2", "S3_EC2");
    }

    @Test
    public void test_GetFluentSetterMethodName_NoEnum() {
        when(serviceModel.getShapes()).thenReturn(mockShapeMap);
        when(mockShape.getEnumValues()).thenReturn(null);
        when(mockShape.getType()).thenReturn("foo");

        assertThat(strat.getFluentSetterMethodName("AwesomeMethod", mockParentShape, mockShape)).isEqualTo("awesomeMethod");
    }

    @Test
    public void test_GetFluentSetterMethodName_NoEnum_WithList() {
        when(serviceModel.getShapes()).thenReturn(mockShapeMap);
        when(mockShapeMap.get(eq("MockStringShape"))).thenReturn(mockStringShape);

        when(mockShape.getEnumValues()).thenReturn(null);
        when(mockShape.getType()).thenReturn("list");
        when(mockShape.getListMember()).thenReturn(member);

        when(mockStringShape.getEnumValues()).thenReturn(null);
        when(mockStringShape.getType()).thenReturn("string");

        when(member.getShape()).thenReturn("MockStringShape");

        assertThat(strat.getFluentSetterMethodName("AwesomeMethod", mockParentShape, mockShape)).isEqualTo("awesomeMethod");
    }

    @Test
    public void test_GetFluentSetterMethodName_WithEnumShape_NoListOrMap() {
        when(serviceModel.getShapes()).thenReturn(mockShapeMap);
        when(mockShape.getEnumValues()).thenReturn(new ArrayList<>());
        when(mockShape.getType()).thenReturn("foo");

        assertThat(strat.getFluentSetterMethodName("AwesomeMethod", mockParentShape, mockShape)).isEqualTo("awesomeMethod");
    }

    @Test
    public void test_GetFluentSetterMethodName_WithEnumShape_WithList() {
        when(serviceModel.getShapes()).thenReturn(mockShapeMap);
        when(mockShapeMap.get(eq("MockStringShape"))).thenReturn(mockStringShape);

        when(mockShape.getEnumValues()).thenReturn(null);
        when(mockShape.getType()).thenReturn("list");
        when(mockShape.getListMember()).thenReturn(member);

        when(mockStringShape.getEnumValues()).thenReturn(Arrays.asList("Enum1", "Enum2"));
        when(mockStringShape.getType()).thenReturn("string");

        when(member.getShape()).thenReturn("MockStringShape");

        assertThat(strat.getFluentSetterMethodName("AwesomeMethod", mockParentShape, mockShape)).isEqualTo("awesomeMethodWithStrings");
    }

    @Test
    public void nonSharedModel_packageName() {
        String serviceName = "foo";
        DefaultNamingStrategy strategy = new DefaultNamingStrategy(serviceModel, CustomizationConfig.create());
        assertThat(strategy.getClientPackageName(serviceName)).isEqualTo("foo");
        assertThat(strategy.getPaginatorsPackageName(serviceName)).isEqualTo("foo.paginators");
        assertThat(strategy.getSmokeTestPackageName(serviceName)).isEqualTo("foo.smoketests");
        assertThat(strategy.getModelPackageName(serviceName)).isEqualTo("foo.model");
        assertThat(strategy.getRequestTransformPackageName(serviceName)).isEqualTo("foo.transform");
        assertThat(strategy.getTransformPackageName(serviceName)).isEqualTo("foo.transform");
    }

    @Test
    public void sharedModel_notProvidingPackageName_shouldUseServiceName() {
        CustomizationConfig config = CustomizationConfig.create();
        ShareModelConfig shareModelConfig = new ShareModelConfig();
        shareModelConfig.setShareModelWith("foo");
        config.setShareModelConfig(shareModelConfig);
        String serviceName = "bar";

        DefaultNamingStrategy customizedModel = new DefaultNamingStrategy(serviceModel, config);

        assertThat(customizedModel.getClientPackageName(serviceName)).isEqualTo("foo.bar");
        assertThat(customizedModel.getPaginatorsPackageName(serviceName)).isEqualTo("foo.bar.paginators");
        assertThat(customizedModel.getSmokeTestPackageName(serviceName)).isEqualTo("foo.bar.smoketests");
        assertThat(customizedModel.getRequestTransformPackageName(serviceName)).isEqualTo("foo.bar.transform");

        // should share the same model and non-request transform packages
        assertThat(customizedModel.getModelPackageName(serviceName)).isEqualTo("foo.model");
        assertThat(customizedModel.getTransformPackageName(serviceName)).isEqualTo("foo.transform");
    }


    @Test
    public void sharedModel_providingPackageName_shouldUseProvidedpackageName() {
        CustomizationConfig config = CustomizationConfig.create();
        ShareModelConfig shareModelConfig = new ShareModelConfig();
        shareModelConfig.setShareModelWith("foo");
        shareModelConfig.setPackageName("b");
        config.setShareModelConfig(shareModelConfig);
        String serviceName = "bar";

        DefaultNamingStrategy customizedModel = new DefaultNamingStrategy(serviceModel, config);

        assertThat(customizedModel.getClientPackageName(serviceName)).isEqualTo("foo.b");
        assertThat(customizedModel.getPaginatorsPackageName(serviceName)).isEqualTo("foo.b.paginators");
        assertThat(customizedModel.getSmokeTestPackageName(serviceName)).isEqualTo("foo.b.smoketests");
        assertThat(customizedModel.getRequestTransformPackageName(serviceName)).isEqualTo("foo.b.transform");

        // should share the same model and non-request transform packages
        assertThat(customizedModel.getModelPackageName(serviceName)).isEqualTo("foo.model");
        assertThat(customizedModel.getTransformPackageName(serviceName)).isEqualTo("foo.transform");
    }

    @Test
    public void modelNameShouldHavePascalCase() {
        when(serviceModel.getMetadata()).thenReturn(serviceMetadata);
        when(serviceMetadata.getServiceId()).thenReturn("UnitTestService");
        assertThat(strat.getRequestClassName("CAPSTest")).isEqualTo("CapsTestRequest");
        assertThat(strat.getExceptionName("CAPSTest")).isEqualTo("CapsTestException");
        assertThat(strat.getResponseClassName("CAPSTest")).isEqualTo("CapsTestResponse");
        assertThat(strat.getResponseClassName("CAPSByIndex")).isEqualTo("CapsByIndexResponse");

        assertThat(strat.getRequestClassName("FollowedByS3")).isEqualTo("FollowedByS3Request");
    }

    @Test
    public void getServiceName_Uses_ServiceId() {
        when(serviceModel.getMetadata()).thenReturn(serviceMetadata);
        when(serviceMetadata.getServiceId()).thenReturn("Foo");

        assertThat(strat.getServiceName()).isEqualTo("Foo");
    }

    @Test (expected = IllegalStateException.class)
    public void getServiceName_ThrowsException_WhenServiceIdIsNull() {
        when(serviceModel.getMetadata()).thenReturn(serviceMetadata);
        when(serviceMetadata.getServiceId()).thenReturn(null);

        strat.getServiceName();
    }

    @Test (expected = IllegalStateException.class)
    public void getServiceName_ThrowsException_WhenServiceIdIsEmpty() {
        when(serviceModel.getMetadata()).thenReturn(serviceMetadata);
        when(serviceMetadata.getServiceId()).thenReturn("");

        strat.getServiceName();
    }

    @Test (expected = IllegalStateException.class)
    public void getServiceName_ThrowsException_WhenServiceIdHasWhiteSpace() {
        when(serviceModel.getMetadata()).thenReturn(serviceMetadata);
        when(serviceMetadata.getServiceId()).thenReturn("  ");

        strat.getServiceName();
    }

    @Test
    public void getSdkFieldFieldName_SingleWord() {
        assertThat(strat.getSdkFieldFieldName(new MemberModel().withName("foo")))
            .isEqualTo("FOO_FIELD");
    }

    @Test
    public void getSdkFieldFieldName_CamalCaseConvertedToScreamCase() {
        assertThat(strat.getSdkFieldFieldName(new MemberModel().withName("fooBar")))
            .isEqualTo("FOO_BAR_FIELD");
    }

    @Test
    public void getSdkFieldFieldName_PascalCaseConvertedToScreamCase() {
        assertThat(strat.getSdkFieldFieldName(new MemberModel().withName("FooBar")))
            .isEqualTo("FOO_BAR_FIELD");
    }

    private void validateConversion(String input, String expectedOutput) {
        assertThat(strat.getEnumValueName(input)).isEqualTo(expectedOutput);
    }

    @Test
    public void validateDisallowsUnderscoresWithCustomization() {
        String invalidName = "foo_bar";
        verifyFailure(i -> i.getMetadata().setAsyncBuilderInterface(invalidName));
        verifyFailure(i -> i.getMetadata().setSyncBuilderInterface(invalidName));
        verifyFailure(i -> i.getMetadata().setAsyncInterface(invalidName));
        verifyFailure(i -> i.getMetadata().setSyncInterface(invalidName));
        verifyFailure(i -> i.getMetadata().setBaseBuilderInterface(invalidName));
        verifyFailure(i -> i.getMetadata().setBaseExceptionName(invalidName));
        verifyFailure(i -> i.getOperations().put(invalidName, opModel(o -> o.setOperationName(invalidName))));
        verifyFailure(i -> i.getWaiters().put(invalidName, null));
        verifyFailure(i -> i.getShapes().put(invalidName, shapeModel(s -> s.setShapeName(invalidName))));
        verifyFailure(i -> i.getShapes().put(invalidName, shapeWithMember(m -> m.setBeanStyleGetterMethodName(invalidName))));
        verifyFailure(i -> i.getShapes().put(invalidName, shapeWithMember(m -> m.setBeanStyleSetterMethodName(invalidName))));
        verifyFailure(i -> i.getShapes().put(invalidName, shapeWithMember(m -> m.setFluentEnumGetterMethodName(invalidName))));
        verifyFailure(i -> i.getShapes().put(invalidName, shapeWithMember(m -> m.setFluentEnumSetterMethodName(invalidName))));
        verifyFailure(i -> i.getShapes().put(invalidName, shapeWithMember(m -> m.setFluentGetterMethodName(invalidName))));
        verifyFailure(i -> i.getShapes().put(invalidName, shapeWithMember(m -> m.setFluentSetterMethodName(invalidName))));
        verifyFailure(i -> i.getShapes().put(invalidName, shapeWithMember(m -> m.setEnumType(invalidName))));
    }

    @Test
    public void validateAllowsUnderscoresWithCustomization() {
        CustomizationConfig customization =
            CustomizationConfig.create()
                               .withUnderscoresInShapeNameBehavior(UnderscoresInNameBehavior.ALLOW);
        NamingStrategy strategy = new DefaultNamingStrategy(serviceModel, customization);

        Metadata metadata = new Metadata();
        metadata.setAsyncBuilderInterface("foo_bar");

        IntermediateModel model = new IntermediateModel();
        model.setMetadata(metadata);

        strategy.validateCustomerVisibleNaming(model);
    }

    @Test
    public void validateServiceIdentifiersForEnvVarsAndProfileProperty() {
        when(serviceModel.getMetadata()).thenReturn(serviceMetadata);

        validateServiceIdSetting("AccessAnalyzer", "accessanalyzer", "ACCESSANALYZER", "AccessAnalyzer");
        validateServiceIdSetting("Account", "account", "ACCOUNT", "Account");
        validateServiceIdSetting("ACM", "acm", "ACM", "Acm");
        validateServiceIdSetting("ACM PCA", "acm_pca", "ACM_PCA", "AcmPca");
        validateServiceIdSetting("Alexa For Business", "alexa_for_business", "ALEXA_FOR_BUSINESS", "AlexaForBusiness");
        validateServiceIdSetting("amp", "amp", "AMP", "Amp");
        validateServiceIdSetting("Amplify", "amplify", "AMPLIFY", "Amplify");
        validateServiceIdSetting("AmplifyBackend", "amplifybackend", "AMPLIFYBACKEND", "AmplifyBackend");
        validateServiceIdSetting("AmplifyUIBuilder", "amplifyuibuilder", "AMPLIFYUIBUILDER", "AmplifyUiBuilder");
        validateServiceIdSetting("API Gateway", "api_gateway", "API_GATEWAY", "ApiGateway");
        // Kotlin uses ApiGatewayManagement
        validateServiceIdSetting("ApiGatewayManagementApi", "apigatewaymanagementapi", "APIGATEWAYMANAGEMENTAPI",
                                 "ApiGatewayManagementApi");
        validateServiceIdSetting("ApiGatewayV2", "apigatewayv2", "APIGATEWAYV2", "ApiGatewayV2");
        validateServiceIdSetting("AppConfig", "appconfig", "APPCONFIG", "AppConfig");
        validateServiceIdSetting("AppConfigData", "appconfigdata", "APPCONFIGDATA", "AppConfigData");
        validateServiceIdSetting("AppFabric", "appfabric", "APPFABRIC", "AppFabric");
        validateServiceIdSetting("Appflow", "appflow", "APPFLOW", "Appflow");
        validateServiceIdSetting("AppIntegrations", "appintegrations", "APPINTEGRATIONS", "AppIntegrations");
        validateServiceIdSetting("Application Auto Scaling", "application_auto_scaling", "APPLICATION_AUTO_SCALING", "ApplicationAutoScaling");
        validateServiceIdSetting("Application Insights", "application_insights", "APPLICATION_INSIGHTS", "ApplicationInsights");
        validateServiceIdSetting("ApplicationCostProfiler", "applicationcostprofiler", "APPLICATIONCOSTPROFILER", "ApplicationCostProfiler");
        validateServiceIdSetting("App Mesh", "app_mesh", "APP_MESH", "AppMesh");
        validateServiceIdSetting("AppRunner", "apprunner", "APPRUNNER", "AppRunner");
        validateServiceIdSetting("AppStream", "appstream", "APPSTREAM", "AppStream");
        validateServiceIdSetting("AppSync", "appsync", "APPSYNC", "AppSync");
        validateServiceIdSetting("ARC Zonal Shift", "arc_zonal_shift", "ARC_ZONAL_SHIFT", "ArcZonalShift");
        validateServiceIdSetting("Artifact", "artifact", "ARTIFACT", "Artifact");
        validateServiceIdSetting("Athena", "athena", "ATHENA", "Athena");
        validateServiceIdSetting("AuditManager", "auditmanager", "AUDITMANAGER", "AuditManager");
        validateServiceIdSetting("Auto Scaling", "auto_scaling", "AUTO_SCALING", "AutoScaling");
        validateServiceIdSetting("Auto Scaling Plans", "auto_scaling_plans", "AUTO_SCALING_PLANS", "AutoScalingPlans");
        validateServiceIdSetting("b2bi", "b2bi", "B2BI", "B2Bi");
        validateServiceIdSetting("Backup", "backup", "BACKUP", "Backup");
        validateServiceIdSetting("Backup Gateway", "backup_gateway", "BACKUP_GATEWAY", "BackupGateway");
        validateServiceIdSetting("BackupStorage", "backupstorage", "BACKUPSTORAGE", "BackupStorage");
        validateServiceIdSetting("Batch", "batch", "BATCH", "Batch");
        validateServiceIdSetting("BCM Data Exports", "bcm_data_exports", "BCM_DATA_EXPORTS", "BcmDataExports");
        validateServiceIdSetting("Bedrock", "bedrock", "BEDROCK", "Bedrock");
        validateServiceIdSetting("Bedrock Agent", "bedrock_agent", "BEDROCK_AGENT", "BedrockAgent");
        validateServiceIdSetting("Bedrock Agent Runtime", "bedrock_agent_runtime", "BEDROCK_AGENT_RUNTIME", "BedrockAgentRuntime");
        validateServiceIdSetting("Bedrock Runtime", "bedrock_runtime", "BEDROCK_RUNTIME", "BedrockRuntime");
        validateServiceIdSetting("billingconductor", "billingconductor", "BILLINGCONDUCTOR", "Billingconductor");
        validateServiceIdSetting("Braket", "braket", "BRAKET", "Braket");
        validateServiceIdSetting("Budgets", "budgets", "BUDGETS", "Budgets");
        validateServiceIdSetting("Cost Explorer", "cost_explorer", "COST_EXPLORER", "CostExplorer");
        validateServiceIdSetting("chatbot", "chatbot", "CHATBOT", "Chatbot");
        validateServiceIdSetting("Chime", "chime", "CHIME", "Chime");
        validateServiceIdSetting("Chime SDK Identity", "chime_sdk_identity", "CHIME_SDK_IDENTITY", "ChimeSdkIdentity");
        validateServiceIdSetting("Chime SDK Media Pipelines", "chime_sdk_media_pipelines", "CHIME_SDK_MEDIA_PIPELINES", "ChimeSdkMediaPipelines");
        validateServiceIdSetting("Chime SDK Meetings", "chime_sdk_meetings", "CHIME_SDK_MEETINGS", "ChimeSdkMeetings");
        validateServiceIdSetting("Chime SDK Messaging", "chime_sdk_messaging", "CHIME_SDK_MESSAGING", "ChimeSdkMessaging");
        validateServiceIdSetting("Chime SDK Voice", "chime_sdk_voice", "CHIME_SDK_VOICE", "ChimeSdkVoice");
        validateServiceIdSetting("CleanRooms", "cleanrooms", "CLEANROOMS", "CleanRooms");
        validateServiceIdSetting("CleanRoomsML", "cleanroomsml", "CLEANROOMSML", "CleanRoomsMl");
        validateServiceIdSetting("Cloud9", "cloud9", "CLOUD9", "Cloud9");
        validateServiceIdSetting("CloudControl", "cloudcontrol", "CLOUDCONTROL", "CloudControl");
        validateServiceIdSetting("CloudDirectory", "clouddirectory", "CLOUDDIRECTORY", "CloudDirectory");
        validateServiceIdSetting("CloudFormation", "cloudformation", "CLOUDFORMATION", "CloudFormation");
        validateServiceIdSetting("CloudFront", "cloudfront", "CLOUDFRONT", "CloudFront");
        validateServiceIdSetting("CloudFront KeyValueStore", "cloudfront_keyvaluestore", "CLOUDFRONT_KEYVALUESTORE", "CloudFrontKeyValueStore");
        validateServiceIdSetting("CloudHSM", "cloudhsm", "CLOUDHSM", "CloudHsm");
        validateServiceIdSetting("CloudHSM V2", "cloudhsm_v2", "CLOUDHSM_V2", "CloudHsmV2");
        validateServiceIdSetting("CloudSearch", "cloudsearch", "CLOUDSEARCH", "CloudSearch");
        validateServiceIdSetting("CloudSearch Domain", "cloudsearch_domain", "CLOUDSEARCH_DOMAIN", "CloudSearchDomain");
        validateServiceIdSetting("CloudTrail", "cloudtrail", "CLOUDTRAIL", "CloudTrail");
        validateServiceIdSetting("CloudTrail Data", "cloudtrail_data", "CLOUDTRAIL_DATA", "CloudTrailData");
        validateServiceIdSetting("CloudWatch", "cloudwatch", "CLOUDWATCH", "CloudWatch");
        validateServiceIdSetting("codeartifact", "codeartifact", "CODEARTIFACT", "Codeartifact");
        validateServiceIdSetting("CodeBuild", "codebuild", "CODEBUILD", "CodeBuild");
        validateServiceIdSetting("CodeCatalyst", "codecatalyst", "CODECATALYST", "CodeCatalyst");
        validateServiceIdSetting("CodeCommit", "codecommit", "CODECOMMIT", "CodeCommit");
        validateServiceIdSetting("CodeDeploy", "codedeploy", "CODEDEPLOY", "CodeDeploy");
        validateServiceIdSetting("CodeGuru Reviewer", "codeguru_reviewer", "CODEGURU_REVIEWER", "CodeGuruReviewer");
        validateServiceIdSetting("CodeGuru Security", "codeguru_security", "CODEGURU_SECURITY", "CodeGuruSecurity");
        validateServiceIdSetting("CodeGuruProfiler", "codeguruprofiler", "CODEGURUPROFILER", "CodeGuruProfiler");
        validateServiceIdSetting("CodePipeline", "codepipeline", "CODEPIPELINE", "CodePipeline");
        validateServiceIdSetting("CodeStar", "codestar", "CODESTAR", "CodeStar");
        validateServiceIdSetting("CodeStar connections", "codestar_connections", "CODESTAR_CONNECTIONS", "CodeStarConnections");
        validateServiceIdSetting("codestar notifications", "codestar_notifications", "CODESTAR_NOTIFICATIONS", "CodestarNotifications");
        validateServiceIdSetting("Cognito Identity", "cognito_identity", "COGNITO_IDENTITY", "CognitoIdentity");
        validateServiceIdSetting("Cognito Identity Provider", "cognito_identity_provider", "COGNITO_IDENTITY_PROVIDER", "CognitoIdentityProvider");
        validateServiceIdSetting("Cognito Sync", "cognito_sync", "COGNITO_SYNC", "CognitoSync");
        validateServiceIdSetting("Comprehend", "comprehend", "COMPREHEND", "Comprehend");
        validateServiceIdSetting("ComprehendMedical", "comprehendmedical", "COMPREHENDMEDICAL", "ComprehendMedical");
        validateServiceIdSetting("Compute Optimizer", "compute_optimizer", "COMPUTE_OPTIMIZER", "ComputeOptimizer");
        validateServiceIdSetting("Config Service", "config_service", "CONFIG_SERVICE", "Config");
        validateServiceIdSetting("Connect", "connect", "CONNECT", "Connect");
        validateServiceIdSetting("Connect Contact Lens", "connect_contact_lens", "CONNECT_CONTACT_LENS", "ConnectContactLens");
        validateServiceIdSetting("ConnectCampaigns", "connectcampaigns", "CONNECTCAMPAIGNS", "ConnectCampaigns");
        validateServiceIdSetting("ConnectCases", "connectcases", "CONNECTCASES", "ConnectCases");
        validateServiceIdSetting("ConnectParticipant", "connectparticipant", "CONNECTPARTICIPANT", "ConnectParticipant");
        validateServiceIdSetting("ControlTower", "controltower", "CONTROLTOWER", "ControlTower");
        validateServiceIdSetting("Cost Optimization Hub", "cost_optimization_hub", "COST_OPTIMIZATION_HUB", "CostOptimizationHub");
        validateServiceIdSetting("Cost and Usage Report Service", "cost_and_usage_report_service", "COST_AND_USAGE_REPORT_SERVICE", "CostAndUsageReport");
        validateServiceIdSetting("Customer Profiles", "customer_profiles", "CUSTOMER_PROFILES", "CustomerProfiles");
        validateServiceIdSetting("DataBrew", "databrew", "DATABREW", "DataBrew");
        validateServiceIdSetting("DataExchange", "dataexchange", "DATAEXCHANGE", "DataExchange");
        validateServiceIdSetting("Data Pipeline", "data_pipeline", "DATA_PIPELINE", "DataPipeline");
        validateServiceIdSetting("DataSync", "datasync", "DATASYNC", "DataSync");
        validateServiceIdSetting("DataZone", "datazone", "DATAZONE", "DataZone");
        validateServiceIdSetting("DAX", "dax", "DAX", "Dax");
        validateServiceIdSetting("Detective", "detective", "DETECTIVE", "Detective");
        validateServiceIdSetting("Device Farm", "device_farm", "DEVICE_FARM", "DeviceFarm");
        validateServiceIdSetting("DevOps Guru", "devops_guru", "DEVOPS_GURU", "DevOpsGuru");
        validateServiceIdSetting("Direct Connect", "direct_connect", "DIRECT_CONNECT", "DirectConnect");
        validateServiceIdSetting("Application Discovery Service", "application_discovery_service", "APPLICATION_DISCOVERY_SERVICE", "ApplicationDiscovery");
        validateServiceIdSetting("DLM", "dlm", "DLM", "Dlm");
        validateServiceIdSetting("Database Migration Service", "database_migration_service", "DATABASE_MIGRATION_SERVICE", "DatabaseMigration");
        validateServiceIdSetting("DocDB", "docdb", "DOCDB", "DocDb");
        validateServiceIdSetting("DocDB Elastic", "docdb_elastic", "DOCDB_ELASTIC", "DocDbElastic");
        validateServiceIdSetting("drs", "drs", "DRS", "Drs");
        validateServiceIdSetting("Directory Service", "directory_service", "DIRECTORY_SERVICE", "Directory");
        validateServiceIdSetting("DynamoDB", "dynamodb", "DYNAMODB", "DynamoDb");
        validateServiceIdSetting("DynamoDB Streams", "dynamodb_streams", "DYNAMODB_STREAMS", "DynamoDbStreams");
        validateServiceIdSetting("EBS", "ebs", "EBS", "Ebs");
        validateServiceIdSetting("EC2", "ec2", "EC2", "Ec2");
        validateServiceIdSetting("EC2 Instance Connect", "ec2_instance_connect", "EC2_INSTANCE_CONNECT", "Ec2InstanceConnect");
        validateServiceIdSetting("ECR", "ecr", "ECR", "Ecr");
        validateServiceIdSetting("ECR PUBLIC", "ecr_public", "ECR_PUBLIC", "EcrPublic");
        validateServiceIdSetting("ECS", "ecs", "ECS", "Ecs");
        validateServiceIdSetting("EFS", "efs", "EFS", "Efs");
        validateServiceIdSetting("EKS", "eks", "EKS", "Eks");
        validateServiceIdSetting("EKS Auth", "eks_auth", "EKS_AUTH", "EksAuth");
        validateServiceIdSetting("Elastic Inference", "elastic_inference", "ELASTIC_INFERENCE", "ElasticInference");
        validateServiceIdSetting("ElastiCache", "elasticache", "ELASTICACHE", "ElastiCache");
        validateServiceIdSetting("Elastic Beanstalk", "elastic_beanstalk", "ELASTIC_BEANSTALK", "ElasticBeanstalk");
        validateServiceIdSetting("Elastic Transcoder", "elastic_transcoder", "ELASTIC_TRANSCODER", "ElasticTranscoder");
        validateServiceIdSetting("Elastic Load Balancing", "elastic_load_balancing", "ELASTIC_LOAD_BALANCING", "ElasticLoadBalancing");
        validateServiceIdSetting("Elastic Load Balancing v2", "elastic_load_balancing_v2", "ELASTIC_LOAD_BALANCING_V2", "ElasticLoadBalancingV2");
        validateServiceIdSetting("EMR", "emr", "EMR", "Emr");
        validateServiceIdSetting("EMR containers", "emr_containers", "EMR_CONTAINERS", "EmrContainers");
        validateServiceIdSetting("EMR Serverless", "emr_serverless", "EMR_SERVERLESS", "EmrServerless");
        validateServiceIdSetting("EntityResolution", "entityresolution", "ENTITYRESOLUTION", "EntityResolution");
        validateServiceIdSetting("Elasticsearch Service", "elasticsearch_service", "ELASTICSEARCH_SERVICE", "Elasticsearch");
        validateServiceIdSetting("EventBridge", "eventbridge", "EVENTBRIDGE", "EventBridge");
        validateServiceIdSetting("Evidently", "evidently", "EVIDENTLY", "Evidently");
        validateServiceIdSetting("finspace", "finspace", "FINSPACE", "Finspace");
        validateServiceIdSetting("finspace data", "finspace_data", "FINSPACE_DATA", "FinspaceData");
        validateServiceIdSetting("Firehose", "firehose", "FIREHOSE", "Firehose");
        validateServiceIdSetting("fis", "fis", "FIS", "Fis");
        validateServiceIdSetting("FMS", "fms", "FMS", "Fms");
        validateServiceIdSetting("forecast", "forecast", "FORECAST", "Forecast");
        validateServiceIdSetting("forecastquery", "forecastquery", "FORECASTQUERY", "Forecastquery");
        validateServiceIdSetting("FraudDetector", "frauddetector", "FRAUDDETECTOR", "FraudDetector");
        validateServiceIdSetting("FreeTier", "freetier", "FREETIER", "FreeTier");
        validateServiceIdSetting("FSx", "fsx", "FSX", "FSx");
        validateServiceIdSetting("GameLift", "gamelift", "GAMELIFT", "GameLift");
        validateServiceIdSetting("Glacier", "glacier", "GLACIER", "Glacier");
        validateServiceIdSetting("Global Accelerator", "global_accelerator", "GLOBAL_ACCELERATOR", "GlobalAccelerator");
        validateServiceIdSetting("Glue", "glue", "GLUE", "Glue");
        validateServiceIdSetting("grafana", "grafana", "GRAFANA", "Grafana");
        validateServiceIdSetting("Greengrass", "greengrass", "GREENGRASS", "Greengrass");
        validateServiceIdSetting("GreengrassV2", "greengrassv2", "GREENGRASSV2", "GreengrassV2");
        validateServiceIdSetting("GroundStation", "groundstation", "GROUNDSTATION", "GroundStation");
        validateServiceIdSetting("GuardDuty", "guardduty", "GUARDDUTY", "GuardDuty");
        validateServiceIdSetting("Health", "health", "HEALTH", "Health");
        validateServiceIdSetting("HealthLake", "healthlake", "HEALTHLAKE", "HealthLake");
        validateServiceIdSetting("Honeycode", "honeycode", "HONEYCODE", "Honeycode");
        validateServiceIdSetting("IAM", "iam", "IAM", "Iam");
        validateServiceIdSetting("identitystore", "identitystore", "IDENTITYSTORE", "Identitystore");
        validateServiceIdSetting("imagebuilder", "imagebuilder", "IMAGEBUILDER", "Imagebuilder");
        validateServiceIdSetting("ImportExport", "importexport", "IMPORTEXPORT", "ImportExport");
        validateServiceIdSetting("Inspector", "inspector", "INSPECTOR", "Inspector");
        validateServiceIdSetting("Inspector Scan", "inspector_scan", "INSPECTOR_SCAN", "InspectorScan");
        validateServiceIdSetting("Inspector2", "inspector2", "INSPECTOR2", "Inspector2");
        validateServiceIdSetting("InternetMonitor", "internetmonitor", "INTERNETMONITOR", "InternetMonitor");
        validateServiceIdSetting("IoT", "iot", "IOT", "Iot");
        validateServiceIdSetting("IoT Data Plane", "iot_data_plane", "IOT_DATA_PLANE", "IotDataPlane");
        validateServiceIdSetting("IoT Jobs Data Plane", "iot_jobs_data_plane", "IOT_JOBS_DATA_PLANE", "IotJobsDataPlane");
        validateServiceIdSetting("IoT 1Click Devices Service", "iot_1click_devices_service", "IOT_1CLICK_DEVICES_SERVICE", "Iot1ClickDevices");
        validateServiceIdSetting("IoT 1Click Projects", "iot_1click_projects", "IOT_1CLICK_PROJECTS", "Iot1ClickProjects");
        // Kotlin uses Iot instead of IoT, wherever that appears
        validateServiceIdSetting("IoTAnalytics", "iotanalytics", "IOTANALYTICS", "IoTAnalytics");
        validateServiceIdSetting("IotDeviceAdvisor", "iotdeviceadvisor", "IOTDEVICEADVISOR", "IotDeviceAdvisor");
        validateServiceIdSetting("IoT Events", "iot_events", "IOT_EVENTS", "IotEvents");
        validateServiceIdSetting("IoT Events Data", "iot_events_data", "IOT_EVENTS_DATA", "IotEventsData");
        validateServiceIdSetting("IoTFleetHub", "iotfleethub", "IOTFLEETHUB", "IoTFleetHub");
        validateServiceIdSetting("IoTFleetWise", "iotfleetwise", "IOTFLEETWISE", "IoTFleetWise");
        validateServiceIdSetting("IoTSecureTunneling", "iotsecuretunneling", "IOTSECURETUNNELING", "IoTSecureTunneling");
        validateServiceIdSetting("IoTSiteWise", "iotsitewise", "IOTSITEWISE", "IoTSiteWise");
        validateServiceIdSetting("IoTThingsGraph", "iotthingsgraph", "IOTTHINGSGRAPH", "IoTThingsGraph");
        validateServiceIdSetting("IoTTwinMaker", "iottwinmaker", "IOTTWINMAKER", "IoTTwinMaker");
        validateServiceIdSetting("IoT Wireless", "iot_wireless", "IOT_WIRELESS", "IotWireless");
        validateServiceIdSetting("ivs", "ivs", "IVS", "Ivs");
        validateServiceIdSetting("IVS RealTime", "ivs_realtime", "IVS_REALTIME", "IvsRealTime");
        validateServiceIdSetting("ivschat", "ivschat", "IVSCHAT", "Ivschat");
        validateServiceIdSetting("Kafka", "kafka", "KAFKA", "Kafka");
        validateServiceIdSetting("KafkaConnect", "kafkaconnect", "KAFKACONNECT", "KafkaConnect");
        validateServiceIdSetting("kendra", "kendra", "KENDRA", "Kendra");
        validateServiceIdSetting("Kendra Ranking", "kendra_ranking", "KENDRA_RANKING", "KendraRanking");
        validateServiceIdSetting("Keyspaces", "keyspaces", "KEYSPACES", "Keyspaces");
        validateServiceIdSetting("Kinesis", "kinesis", "KINESIS", "Kinesis");
        validateServiceIdSetting("Kinesis Video Archived Media", "kinesis_video_archived_media", "KINESIS_VIDEO_ARCHIVED_MEDIA", "KinesisVideoArchivedMedia");
        validateServiceIdSetting("Kinesis Video Media", "kinesis_video_media", "KINESIS_VIDEO_MEDIA", "KinesisVideoMedia");
        validateServiceIdSetting("Kinesis Video Signaling", "kinesis_video_signaling", "KINESIS_VIDEO_SIGNALING", "KinesisVideoSignaling");
        validateServiceIdSetting("Kinesis Video WebRTC Storage", "kinesis_video_webrtc_storage", "KINESIS_VIDEO_WEBRTC_STORAGE", "KinesisVideoWebRtcStorage");
        validateServiceIdSetting("Kinesis Analytics", "kinesis_analytics", "KINESIS_ANALYTICS", "KinesisAnalytics");
        validateServiceIdSetting("Kinesis Analytics V2", "kinesis_analytics_v2", "KINESIS_ANALYTICS_V2", "KinesisAnalyticsV2");
        validateServiceIdSetting("Kinesis Video", "kinesis_video", "KINESIS_VIDEO", "KinesisVideo");
        validateServiceIdSetting("KMS", "kms", "KMS", "Kms");
        validateServiceIdSetting("LakeFormation", "lakeformation", "LAKEFORMATION", "LakeFormation");
        validateServiceIdSetting("Lambda", "lambda", "LAMBDA", "Lambda");
        validateServiceIdSetting("Launch Wizard", "launch_wizard", "LAUNCH_WIZARD", "LaunchWizard");
        validateServiceIdSetting("Lex Model Building Service", "lex_model_building_service", "LEX_MODEL_BUILDING_SERVICE", "LexModelBuilding");
        validateServiceIdSetting("Lex Runtime Service", "lex_runtime_service", "LEX_RUNTIME_SERVICE", "LexRuntime");
        validateServiceIdSetting("Lex Models V2", "lex_models_v2", "LEX_MODELS_V2", "LexModelsV2");
        validateServiceIdSetting("Lex Runtime V2", "lex_runtime_v2", "LEX_RUNTIME_V2", "LexRuntimeV2");
        validateServiceIdSetting("License Manager", "license_manager", "LICENSE_MANAGER", "LicenseManager");
        validateServiceIdSetting("License Manager Linux Subscriptions", "license_manager_linux_subscriptions", "LICENSE_MANAGER_LINUX_SUBSCRIPTIONS", "LicenseManagerLinuxSubscriptions");
        validateServiceIdSetting("License Manager User Subscriptions", "license_manager_user_subscriptions", "LICENSE_MANAGER_USER_SUBSCRIPTIONS", "LicenseManagerUserSubscriptions");
        validateServiceIdSetting("Lightsail", "lightsail", "LIGHTSAIL", "Lightsail");
        validateServiceIdSetting("Location", "location", "LOCATION", "Location");
        validateServiceIdSetting("CloudWatch Logs", "cloudwatch_logs", "CLOUDWATCH_LOGS", "CloudWatchLogs");
        validateServiceIdSetting("CloudWatch Logs", "cloudwatch_logs", "CLOUDWATCH_LOGS", "CloudWatchLogs");
        validateServiceIdSetting("LookoutEquipment", "lookoutequipment", "LOOKOUTEQUIPMENT", "LookoutEquipment");
        validateServiceIdSetting("LookoutMetrics", "lookoutmetrics", "LOOKOUTMETRICS", "LookoutMetrics");
        validateServiceIdSetting("LookoutVision", "lookoutvision", "LOOKOUTVISION", "LookoutVision");
        validateServiceIdSetting("m2", "m2", "M2", "M2");
        validateServiceIdSetting("Machine Learning", "machine_learning", "MACHINE_LEARNING", "MachineLearning");
        validateServiceIdSetting("Macie2", "macie2", "MACIE2", "Macie2");
        validateServiceIdSetting("ManagedBlockchain", "managedblockchain", "MANAGEDBLOCKCHAIN", "ManagedBlockchain");
        validateServiceIdSetting("ManagedBlockchain Query", "managedblockchain_query", "MANAGEDBLOCKCHAIN_QUERY", "ManagedBlockchainQuery");
        validateServiceIdSetting("Marketplace Agreement", "marketplace_agreement", "MARKETPLACE_AGREEMENT", "MarketplaceAgreement");
        validateServiceIdSetting("Marketplace Catalog", "marketplace_catalog", "MARKETPLACE_CATALOG", "MarketplaceCatalog");
        validateServiceIdSetting("Marketplace Deployment", "marketplace_deployment", "MARKETPLACE_DEPLOYMENT", "MarketplaceDeployment");
        validateServiceIdSetting("Marketplace Entitlement Service", "marketplace_entitlement_service", "MARKETPLACE_ENTITLEMENT_SERVICE", "MarketplaceEntitlement");
        validateServiceIdSetting("Marketplace Commerce Analytics", "marketplace_commerce_analytics", "MARKETPLACE_COMMERCE_ANALYTICS", "MarketplaceCommerceAnalytics");
        validateServiceIdSetting("MediaConnect", "mediaconnect", "MEDIACONNECT", "MediaConnect");
        validateServiceIdSetting("MediaConvert", "mediaconvert", "MEDIACONVERT", "MediaConvert");
        validateServiceIdSetting("MediaLive", "medialive", "MEDIALIVE", "MediaLive");
        validateServiceIdSetting("MediaPackage", "mediapackage", "MEDIAPACKAGE", "MediaPackage");
        validateServiceIdSetting("MediaPackage Vod", "mediapackage_vod", "MEDIAPACKAGE_VOD", "MediaPackageVod");
        validateServiceIdSetting("MediaPackageV2", "mediapackagev2", "MEDIAPACKAGEV2", "MediaPackageV2");
        validateServiceIdSetting("MediaStore", "mediastore", "MEDIASTORE", "MediaStore");
        validateServiceIdSetting("MediaStore Data", "mediastore_data", "MEDIASTORE_DATA", "MediaStoreData");
        validateServiceIdSetting("MediaTailor", "mediatailor", "MEDIATAILOR", "MediaTailor");
        validateServiceIdSetting("Medical Imaging", "medical_imaging", "MEDICAL_IMAGING", "MedicalImaging");
        validateServiceIdSetting("MemoryDB", "memorydb", "MEMORYDB", "MemoryDb");
        validateServiceIdSetting("Marketplace Metering", "marketplace_metering", "MARKETPLACE_METERING", "MarketplaceMetering");
        validateServiceIdSetting("Migration Hub", "migration_hub", "MIGRATION_HUB", "MigrationHub");
        validateServiceIdSetting("mgn", "mgn", "MGN", "Mgn");
        validateServiceIdSetting("Migration Hub Refactor Spaces", "migration_hub_refactor_spaces", "MIGRATION_HUB_REFACTOR_SPACES", "MigrationHubRefactorSpaces");
        validateServiceIdSetting("MigrationHub Config", "migrationhub_config", "MIGRATIONHUB_CONFIG", "MigrationHubConfig");
        validateServiceIdSetting("MigrationHubOrchestrator", "migrationhuborchestrator", "MIGRATIONHUBORCHESTRATOR", "MigrationHubOrchestrator");
        validateServiceIdSetting("MigrationHubStrategy", "migrationhubstrategy", "MIGRATIONHUBSTRATEGY", "MigrationHubStrategy");
        validateServiceIdSetting("Mobile", "mobile", "MOBILE", "Mobile");
        validateServiceIdSetting("mq", "mq", "MQ", "Mq");
        validateServiceIdSetting("MTurk", "mturk", "MTURK", "MTurk");
        validateServiceIdSetting("MWAA", "mwaa", "MWAA", "Mwaa");
        validateServiceIdSetting("Neptune", "neptune", "NEPTUNE", "Neptune");
        validateServiceIdSetting("Neptune Graph", "neptune_graph", "NEPTUNE_GRAPH", "NeptuneGraph");
        validateServiceIdSetting("neptunedata", "neptunedata", "NEPTUNEDATA", "Neptunedata");
        validateServiceIdSetting("Network Firewall", "network_firewall", "NETWORK_FIREWALL", "NetworkFirewall");
        validateServiceIdSetting("NetworkManager", "networkmanager", "NETWORKMANAGER", "NetworkManager");
        validateServiceIdSetting("NetworkMonitor", "networkmonitor", "NETWORKMONITOR", "NetworkMonitor");
        validateServiceIdSetting("nimble", "nimble", "NIMBLE", "Nimble");
        validateServiceIdSetting("OAM", "oam", "OAM", "Oam");
        validateServiceIdSetting("Omics", "omics", "OMICS", "Omics");
        validateServiceIdSetting("OpenSearch", "opensearch", "OPENSEARCH", "OpenSearch");
        validateServiceIdSetting("OpenSearchServerless", "opensearchserverless", "OPENSEARCHSERVERLESS", "OpenSearchServerless");
        validateServiceIdSetting("OpsWorks", "opsworks", "OPSWORKS", "OpsWorks");
        validateServiceIdSetting("OpsWorksCM", "opsworkscm", "OPSWORKSCM", "OpsWorksCm");
        validateServiceIdSetting("Organizations", "organizations", "ORGANIZATIONS", "Organizations");
        validateServiceIdSetting("OSIS", "osis", "OSIS", "Osis");
        validateServiceIdSetting("Outposts", "outposts", "OUTPOSTS", "Outposts");
        validateServiceIdSetting("p8data", "p8data", "P8DATA", "P8Data");
        validateServiceIdSetting("Panorama", "panorama", "PANORAMA", "Panorama");
        validateServiceIdSetting("Payment Cryptography", "payment_cryptography", "PAYMENT_CRYPTOGRAPHY", "PaymentCryptography");
        validateServiceIdSetting("Payment Cryptography Data", "payment_cryptography_data", "PAYMENT_CRYPTOGRAPHY_DATA", "PaymentCryptographyData");
        validateServiceIdSetting("Pca Connector Ad", "pca_connector_ad", "PCA_CONNECTOR_AD", "PcaConnectorAd");
        validateServiceIdSetting("Personalize", "personalize", "PERSONALIZE", "Personalize");
        validateServiceIdSetting("Personalize Events", "personalize_events", "PERSONALIZE_EVENTS", "PersonalizeEvents");
        validateServiceIdSetting("Personalize Runtime", "personalize_runtime", "PERSONALIZE_RUNTIME", "PersonalizeRuntime");
        validateServiceIdSetting("PI", "pi", "PI", "Pi");
        validateServiceIdSetting("Pinpoint", "pinpoint", "PINPOINT", "Pinpoint");
        validateServiceIdSetting("Pinpoint Email", "pinpoint_email", "PINPOINT_EMAIL", "PinpointEmail");
        validateServiceIdSetting("Pinpoint SMS Voice", "pinpoint_sms_voice", "PINPOINT_SMS_VOICE", "PinpointSmsVoice");
        validateServiceIdSetting("Pinpoint SMS Voice V2", "pinpoint_sms_voice_v2", "PINPOINT_SMS_VOICE_V2", "PinpointSmsVoiceV2");
        validateServiceIdSetting("Pipes", "pipes", "PIPES", "Pipes");
        validateServiceIdSetting("Polly", "polly", "POLLY", "Polly");
        validateServiceIdSetting("Pricing", "pricing", "PRICING", "Pricing");
        validateServiceIdSetting("PrivateNetworks", "privatenetworks", "PRIVATENETWORKS", "PrivateNetworks");
        validateServiceIdSetting("Proton", "proton", "PROTON", "Proton");
        validateServiceIdSetting("QBusiness", "qbusiness", "QBUSINESS", "QBusiness");
        validateServiceIdSetting("QConnect", "qconnect", "QCONNECT", "QConnect");
        validateServiceIdSetting("QLDB", "qldb", "QLDB", "Qldb");
        validateServiceIdSetting("QLDB Session", "qldb_session", "QLDB_SESSION", "QldbSession");
        validateServiceIdSetting("QuickSight", "quicksight", "QUICKSIGHT", "QuickSight");
        validateServiceIdSetting("RAM", "ram", "RAM", "Ram");
        validateServiceIdSetting("rbin", "rbin", "RBIN", "Rbin");
        validateServiceIdSetting("RDS", "rds", "RDS", "Rds");
        validateServiceIdSetting("RDS Data", "rds_data", "RDS_DATA", "RdsData");
        validateServiceIdSetting("Redshift", "redshift", "REDSHIFT", "Redshift");
        validateServiceIdSetting("Redshift Data", "redshift_data", "REDSHIFT_DATA", "RedshiftData");
        validateServiceIdSetting("Redshift Serverless", "redshift_serverless", "REDSHIFT_SERVERLESS", "RedshiftServerless");
        validateServiceIdSetting("Rekognition", "rekognition", "REKOGNITION", "Rekognition");
        validateServiceIdSetting("repostspace", "repostspace", "REPOSTSPACE", "Repostspace");
        validateServiceIdSetting("resiliencehub", "resiliencehub", "RESILIENCEHUB", "Resiliencehub");
        validateServiceIdSetting("Resource Explorer 2", "resource_explorer_2", "RESOURCE_EXPLORER_2", "ResourceExplorer2");
        validateServiceIdSetting("Resource Groups", "resource_groups", "RESOURCE_GROUPS", "ResourceGroups");
        // Kotlin uses ResourceGroupsTagging
        validateServiceIdSetting("Resource Groups Tagging API", "resource_groups_tagging_api", "RESOURCE_GROUPS_TAGGING_API", "ResourceGroupsTaggingApi");
        validateServiceIdSetting("RoboMaker", "robomaker", "ROBOMAKER", "RoboMaker");
        validateServiceIdSetting("RolesAnywhere", "rolesanywhere", "ROLESANYWHERE", "RolesAnywhere");
        validateServiceIdSetting("Route 53", "route_53", "ROUTE_53", "Route53");
        validateServiceIdSetting("Route53 Recovery Cluster", "route53_recovery_cluster", "ROUTE53_RECOVERY_CLUSTER", "Route53RecoveryCluster");
        validateServiceIdSetting("Route53 Recovery Control Config", "route53_recovery_control_config", "ROUTE53_RECOVERY_CONTROL_CONFIG", "Route53RecoveryControlConfig");
        validateServiceIdSetting("Route53 Recovery Readiness", "route53_recovery_readiness", "ROUTE53_RECOVERY_READINESS", "Route53RecoveryReadiness");
        validateServiceIdSetting("Route 53 Domains", "route_53_domains", "ROUTE_53_DOMAINS", "Route53Domains");
        validateServiceIdSetting("Route53Resolver", "route53resolver", "ROUTE53RESOLVER", "Route53Resolver");
        validateServiceIdSetting("RUM", "rum", "RUM", "Rum");
        validateServiceIdSetting("S3", "s3", "S3", "S3");
        validateServiceIdSetting("S3 Control", "s3_control", "S3_CONTROL", "S3Control");
        validateServiceIdSetting("S3Outposts", "s3outposts", "S3OUTPOSTS", "S3Outposts");
        validateServiceIdSetting("SageMaker", "sagemaker", "SAGEMAKER", "SageMaker");
        validateServiceIdSetting("SageMaker A2I Runtime", "sagemaker_a2i_runtime", "SAGEMAKER_A2I_RUNTIME", "SageMakerA2IRuntime");
        validateServiceIdSetting("Sagemaker Edge", "sagemaker_edge", "SAGEMAKER_EDGE", "SagemakerEdge");
        validateServiceIdSetting("SageMaker FeatureStore Runtime", "sagemaker_featurestore_runtime", "SAGEMAKER_FEATURESTORE_RUNTIME", "SageMakerFeatureStoreRuntime");
        validateServiceIdSetting("SageMaker Geospatial", "sagemaker_geospatial", "SAGEMAKER_GEOSPATIAL", "SageMakerGeospatial");
        validateServiceIdSetting("SageMaker Metrics", "sagemaker_metrics", "SAGEMAKER_METRICS", "SageMakerMetrics");
        validateServiceIdSetting("SageMaker Runtime", "sagemaker_runtime", "SAGEMAKER_RUNTIME", "SageMakerRuntime");
        validateServiceIdSetting("savingsplans", "savingsplans", "SAVINGSPLANS", "Savingsplans");
        validateServiceIdSetting("Scheduler", "scheduler", "SCHEDULER", "Scheduler");
        validateServiceIdSetting("schemas", "schemas", "SCHEMAS", "Schemas");
        validateServiceIdSetting("SimpleDB", "simpledb", "SIMPLEDB", "SimpleDb");
        validateServiceIdSetting("Secrets Manager", "secrets_manager", "SECRETS_MANAGER", "SecretsManager");
        validateServiceIdSetting("SecurityHub", "securityhub", "SECURITYHUB", "SecurityHub");
        validateServiceIdSetting("SecurityLake", "securitylake", "SECURITYLAKE", "SecurityLake");
        validateServiceIdSetting("ServerlessApplicationRepository", "serverlessapplicationrepository", "SERVERLESSAPPLICATIONREPOSITORY", "ServerlessApplicationRepository");
        validateServiceIdSetting("Service Quotas", "service_quotas", "SERVICE_QUOTAS", "ServiceQuotas");
        validateServiceIdSetting("Service Catalog", "service_catalog", "SERVICE_CATALOG", "ServiceCatalog");
        validateServiceIdSetting("Service Catalog AppRegistry", "service_catalog_appregistry", "SERVICE_CATALOG_APPREGISTRY", "ServiceCatalogAppRegistry");
        validateServiceIdSetting("ServiceDiscovery", "servicediscovery", "SERVICEDISCOVERY", "ServiceDiscovery");
        validateServiceIdSetting("SES", "ses", "SES", "Ses");
        validateServiceIdSetting("SESv2", "sesv2", "SESV2", "SesV2");
        validateServiceIdSetting("Shield", "shield", "SHIELD", "Shield");
        validateServiceIdSetting("signer", "signer", "SIGNER", "Signer");
        validateServiceIdSetting("SimSpaceWeaver", "simspaceweaver", "SIMSPACEWEAVER", "SimSpaceWeaver");
        validateServiceIdSetting("SMS", "sms", "SMS", "Sms");
        validateServiceIdSetting("Snow Device Management", "snow_device_management", "SNOW_DEVICE_MANAGEMENT", "SnowDeviceManagement");
        validateServiceIdSetting("Snowball", "snowball", "SNOWBALL", "Snowball");
        validateServiceIdSetting("SNS", "sns", "SNS", "Sns");
        validateServiceIdSetting("SQS", "sqs", "SQS", "Sqs");
        validateServiceIdSetting("SSM", "ssm", "SSM", "Ssm");
        validateServiceIdSetting("SSM Contacts", "ssm_contacts", "SSM_CONTACTS", "SsmContacts");
        validateServiceIdSetting("SSM Incidents", "ssm_incidents", "SSM_INCIDENTS", "SsmIncidents");
        validateServiceIdSetting("Ssm Sap", "ssm_sap", "SSM_SAP", "SsmSap");
        validateServiceIdSetting("SSO", "sso", "SSO", "Sso");
        validateServiceIdSetting("SSO Admin", "sso_admin", "SSO_ADMIN", "SsoAdmin");
        validateServiceIdSetting("SSO OIDC", "sso_oidc", "SSO_OIDC", "SsoOidc");
        validateServiceIdSetting("SFN", "sfn", "SFN", "Sfn");
        validateServiceIdSetting("Storage Gateway", "storage_gateway", "STORAGE_GATEWAY", "StorageGateway");
        validateServiceIdSetting("STS", "sts", "STS", "Sts");
        validateServiceIdSetting("SupplyChain", "supplychain", "SUPPLYCHAIN", "SupplyChain");
        validateServiceIdSetting("Support", "support", "SUPPORT", "Support");
        validateServiceIdSetting("Support App", "support_app", "SUPPORT_APP", "SupportApp");
        validateServiceIdSetting("SWF", "swf", "SWF", "Swf");
        validateServiceIdSetting("synthetics", "synthetics", "SYNTHETICS", "Synthetics");
        validateServiceIdSetting("Textract", "textract", "TEXTRACT", "Textract");
        validateServiceIdSetting("Timestream InfluxDB", "timestream_influxdb", "TIMESTREAM_INFLUXDB", "TimestreamInfluxDb");
        validateServiceIdSetting("Timestream Query", "timestream_query", "TIMESTREAM_QUERY", "TimestreamQuery");
        validateServiceIdSetting("Timestream Write", "timestream_write", "TIMESTREAM_WRITE", "TimestreamWrite");
        validateServiceIdSetting("tnb", "tnb", "TNB", "Tnb");
        validateServiceIdSetting("Transcribe", "transcribe", "TRANSCRIBE", "Transcribe");
        validateServiceIdSetting("Transfer", "transfer", "TRANSFER", "Transfer");
        validateServiceIdSetting("Translate", "translate", "TRANSLATE", "Translate");
        validateServiceIdSetting("TrustedAdvisor", "trustedadvisor", "TRUSTEDADVISOR", "TrustedAdvisor");
        validateServiceIdSetting("VerifiedPermissions", "verifiedpermissions", "VERIFIEDPERMISSIONS", "VerifiedPermissions");
        validateServiceIdSetting("Voice ID", "voice_id", "VOICE_ID", "VoiceId");
        validateServiceIdSetting("VPC Lattice", "vpc_lattice", "VPC_LATTICE", "VpcLattice");
        validateServiceIdSetting("WAF", "waf", "WAF", "Waf");
        validateServiceIdSetting("WAF Regional", "waf_regional", "WAF_REGIONAL", "WafRegional");
        validateServiceIdSetting("WAFV2", "wafv2", "WAFV2", "Wafv2");
        validateServiceIdSetting("WellArchitected", "wellarchitected", "WELLARCHITECTED", "WellArchitected");
        validateServiceIdSetting("Wisdom", "wisdom", "WISDOM", "Wisdom");
        validateServiceIdSetting("WorkDocs", "workdocs", "WORKDOCS", "WorkDocs");
        validateServiceIdSetting("WorkLink", "worklink", "WORKLINK", "WorkLink");
        validateServiceIdSetting("WorkMail", "workmail", "WORKMAIL", "WorkMail");
        validateServiceIdSetting("WorkMailMessageFlow", "workmailmessageflow", "WORKMAILMESSAGEFLOW", "WorkMailMessageFlow");
        validateServiceIdSetting("WorkSpaces", "workspaces", "WORKSPACES", "WorkSpaces");
        validateServiceIdSetting("WorkSpaces Thin Client", "workspaces_thin_client", "WORKSPACES_THIN_CLIENT", "WorkSpacesThinClient");
        validateServiceIdSetting("WorkSpaces Web", "workspaces_web", "WORKSPACES_WEB", "WorkSpacesWeb");
    }

    private void validateServiceIdSetting(String serviceId,
                                          String profileProperty,
                                          String environmentVariable,
                                          String systemProperty) {
        when(serviceMetadata.getServiceId()).thenReturn(serviceId);
        assertThat(strat.getServiceNameForProfileFile())
            .as(() -> serviceId + " uses profile property " + profileProperty)
            .isEqualTo(profileProperty);
        assertThat(strat.getServiceNameForEnvironmentVariables())
            .as(() -> serviceId + " uses environment variable service name " + environmentVariable)
            .isEqualTo(environmentVariable);
        assertThat(strat.getServiceNameForSystemProperties())
            .as(() -> serviceId + " uses system property service name " + systemProperty)
            .isEqualTo(systemProperty);
        
    }

    private void verifyFailure(Consumer<IntermediateModel> modelModifier) {
        IntermediateModel model = new IntermediateModel();
        model.setMetadata(new Metadata());
        modelModifier.accept(model);
        assertThatThrownBy(() -> strat.validateCustomerVisibleNaming(model)).isInstanceOf(RuntimeException.class);
    }

    private OperationModel opModel(Consumer<OperationModel> operationModifier) {
        OperationModel model = new OperationModel();
        operationModifier.accept(model);
        return model;
    }

    private ShapeModel shapeModel(Consumer<ShapeModel> shapeModifier) {
        ShapeModel model = new ShapeModel();
        shapeModifier.accept(model);
        return model;
    }

    private ShapeModel shapeWithMember(Consumer<MemberModel> memberModifier) {
        MemberModel model = new MemberModel();
        memberModifier.accept(model);
        return shapeModel(s -> s.setMembers(singletonList(model)));
    }
}
