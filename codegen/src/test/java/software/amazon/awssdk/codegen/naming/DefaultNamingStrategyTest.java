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
        validateServiceIdSetting("AccessAnalyzer", "accessanalyzer", "ACCESSANALYZER");
        validateServiceIdSetting("Account", "account", "ACCOUNT");
        validateServiceIdSetting("ACM", "acm", "ACM");
        validateServiceIdSetting("ACM PCA", "acm_pca", "ACM_PCA");
        validateServiceIdSetting("Alexa For Business", "alexa_for_business", "ALEXA_FOR_BUSINESS");
        validateServiceIdSetting("amp", "amp", "AMP");
        validateServiceIdSetting("Amplify", "amplify", "AMPLIFY");
        validateServiceIdSetting("AmplifyBackend", "amplifybackend", "AMPLIFYBACKEND");
        validateServiceIdSetting("AmplifyUIBuilder", "amplifyuibuilder", "AMPLIFYUIBUILDER");
        validateServiceIdSetting("API Gateway", "api_gateway", "API_GATEWAY");
        validateServiceIdSetting("ApiGatewayManagementApi", "apigatewaymanagementapi", "APIGATEWAYMANAGEMENTAPI");
        validateServiceIdSetting("ApiGatewayV2", "apigatewayv2", "APIGATEWAYV2");
        validateServiceIdSetting("AppConfig", "appconfig", "APPCONFIG");
        validateServiceIdSetting("AppConfigData", "appconfigdata", "APPCONFIGDATA");
        validateServiceIdSetting("AppFabric", "appfabric", "APPFABRIC");
        validateServiceIdSetting("Appflow", "appflow", "APPFLOW");
        validateServiceIdSetting("AppIntegrations", "appintegrations", "APPINTEGRATIONS");
        validateServiceIdSetting("Application Auto Scaling", "application_auto_scaling", "APPLICATION_AUTO_SCALING");
        validateServiceIdSetting("Application Insights", "application_insights", "APPLICATION_INSIGHTS");
        validateServiceIdSetting("ApplicationCostProfiler", "applicationcostprofiler", "APPLICATIONCOSTPROFILER");
        validateServiceIdSetting("App Mesh", "app_mesh", "APP_MESH");
        validateServiceIdSetting("AppRunner", "apprunner", "APPRUNNER");
        validateServiceIdSetting("AppStream", "appstream", "APPSTREAM");
        validateServiceIdSetting("AppSync", "appsync", "APPSYNC");
        validateServiceIdSetting("ARC Zonal Shift", "arc_zonal_shift", "ARC_ZONAL_SHIFT");
        validateServiceIdSetting("Artifact", "artifact", "ARTIFACT");
        validateServiceIdSetting("Athena", "athena", "ATHENA");
        validateServiceIdSetting("AuditManager", "auditmanager", "AUDITMANAGER");
        validateServiceIdSetting("Auto Scaling", "auto_scaling", "AUTO_SCALING");
        validateServiceIdSetting("Auto Scaling Plans", "auto_scaling_plans", "AUTO_SCALING_PLANS");
        validateServiceIdSetting("b2bi", "b2bi", "B2BI");
        validateServiceIdSetting("Backup", "backup", "BACKUP");
        validateServiceIdSetting("Backup Gateway", "backup_gateway", "BACKUP_GATEWAY");
        validateServiceIdSetting("BackupStorage", "backupstorage", "BACKUPSTORAGE");
        validateServiceIdSetting("Batch", "batch", "BATCH");
        validateServiceIdSetting("BCM Data Exports", "bcm_data_exports", "BCM_DATA_EXPORTS");
        validateServiceIdSetting("Bedrock", "bedrock", "BEDROCK");
        validateServiceIdSetting("Bedrock Agent", "bedrock_agent", "BEDROCK_AGENT");
        validateServiceIdSetting("Bedrock Agent Runtime", "bedrock_agent_runtime", "BEDROCK_AGENT_RUNTIME");
        validateServiceIdSetting("Bedrock Runtime", "bedrock_runtime", "BEDROCK_RUNTIME");
        validateServiceIdSetting("billingconductor", "billingconductor", "BILLINGCONDUCTOR");
        validateServiceIdSetting("Braket", "braket", "BRAKET");
        validateServiceIdSetting("Budgets", "budgets", "BUDGETS");
        validateServiceIdSetting("Cost Explorer", "cost_explorer", "COST_EXPLORER");
        validateServiceIdSetting("chatbot", "chatbot", "CHATBOT");
        validateServiceIdSetting("Chime", "chime", "CHIME");
        validateServiceIdSetting("Chime SDK Identity", "chime_sdk_identity", "CHIME_SDK_IDENTITY");
        validateServiceIdSetting("Chime SDK Media Pipelines", "chime_sdk_media_pipelines", "CHIME_SDK_MEDIA_PIPELINES");
        validateServiceIdSetting("Chime SDK Meetings", "chime_sdk_meetings", "CHIME_SDK_MEETINGS");
        validateServiceIdSetting("Chime SDK Messaging", "chime_sdk_messaging", "CHIME_SDK_MESSAGING");
        validateServiceIdSetting("Chime SDK Voice", "chime_sdk_voice", "CHIME_SDK_VOICE");
        validateServiceIdSetting("CleanRooms", "cleanrooms", "CLEANROOMS");
        validateServiceIdSetting("CleanRoomsML", "cleanroomsml", "CLEANROOMSML");
        validateServiceIdSetting("Cloud9", "cloud9", "CLOUD9");
        validateServiceIdSetting("CloudControl", "cloudcontrol", "CLOUDCONTROL");
        validateServiceIdSetting("CloudDirectory", "clouddirectory", "CLOUDDIRECTORY");
        validateServiceIdSetting("CloudFormation", "cloudformation", "CLOUDFORMATION");
        validateServiceIdSetting("CloudFront", "cloudfront", "CLOUDFRONT");
        validateServiceIdSetting("CloudFront KeyValueStore", "cloudfront_keyvaluestore", "CLOUDFRONT_KEYVALUESTORE");
        validateServiceIdSetting("CloudHSM", "cloudhsm", "CLOUDHSM");
        validateServiceIdSetting("CloudHSM V2", "cloudhsm_v2", "CLOUDHSM_V2");
        validateServiceIdSetting("CloudSearch", "cloudsearch", "CLOUDSEARCH");
        validateServiceIdSetting("CloudSearch Domain", "cloudsearch_domain", "CLOUDSEARCH_DOMAIN");
        validateServiceIdSetting("CloudTrail", "cloudtrail", "CLOUDTRAIL");
        validateServiceIdSetting("CloudTrail Data", "cloudtrail_data", "CLOUDTRAIL_DATA");
        validateServiceIdSetting("CloudWatch", "cloudwatch", "CLOUDWATCH");
        validateServiceIdSetting("codeartifact", "codeartifact", "CODEARTIFACT");
        validateServiceIdSetting("CodeBuild", "codebuild", "CODEBUILD");
        validateServiceIdSetting("CodeCatalyst", "codecatalyst", "CODECATALYST");
        validateServiceIdSetting("CodeCommit", "codecommit", "CODECOMMIT");
        validateServiceIdSetting("CodeDeploy", "codedeploy", "CODEDEPLOY");
        validateServiceIdSetting("CodeGuru Reviewer", "codeguru_reviewer", "CODEGURU_REVIEWER");
        validateServiceIdSetting("CodeGuru Security", "codeguru_security", "CODEGURU_SECURITY");
        validateServiceIdSetting("CodeGuruProfiler", "codeguruprofiler", "CODEGURUPROFILER");
        validateServiceIdSetting("CodePipeline", "codepipeline", "CODEPIPELINE");
        validateServiceIdSetting("CodeStar", "codestar", "CODESTAR");
        validateServiceIdSetting("CodeStar connections", "codestar_connections", "CODESTAR_CONNECTIONS");
        validateServiceIdSetting("codestar notifications", "codestar_notifications", "CODESTAR_NOTIFICATIONS");
        validateServiceIdSetting("Cognito Identity", "cognito_identity", "COGNITO_IDENTITY");
        validateServiceIdSetting("Cognito Identity Provider", "cognito_identity_provider", "COGNITO_IDENTITY_PROVIDER");
        validateServiceIdSetting("Cognito Sync", "cognito_sync", "COGNITO_SYNC");
        validateServiceIdSetting("Comprehend", "comprehend", "COMPREHEND");
        validateServiceIdSetting("ComprehendMedical", "comprehendmedical", "COMPREHENDMEDICAL");
        validateServiceIdSetting("Compute Optimizer", "compute_optimizer", "COMPUTE_OPTIMIZER");
        validateServiceIdSetting("Config Service", "config_service", "CONFIG_SERVICE");
        validateServiceIdSetting("Connect", "connect", "CONNECT");
        validateServiceIdSetting("Connect Contact Lens", "connect_contact_lens", "CONNECT_CONTACT_LENS");
        validateServiceIdSetting("ConnectCampaigns", "connectcampaigns", "CONNECTCAMPAIGNS");
        validateServiceIdSetting("ConnectCases", "connectcases", "CONNECTCASES");
        validateServiceIdSetting("ConnectParticipant", "connectparticipant", "CONNECTPARTICIPANT");
        validateServiceIdSetting("ControlTower", "controltower", "CONTROLTOWER");
        validateServiceIdSetting("Cost Optimization Hub", "cost_optimization_hub", "COST_OPTIMIZATION_HUB");
        validateServiceIdSetting("Cost and Usage Report Service", "cost_and_usage_report_service", "COST_AND_USAGE_REPORT_SERVICE");
        validateServiceIdSetting("Customer Profiles", "customer_profiles", "CUSTOMER_PROFILES");
        validateServiceIdSetting("DataBrew", "databrew", "DATABREW");
        validateServiceIdSetting("DataExchange", "dataexchange", "DATAEXCHANGE");
        validateServiceIdSetting("Data Pipeline", "data_pipeline", "DATA_PIPELINE");
        validateServiceIdSetting("DataSync", "datasync", "DATASYNC");
        validateServiceIdSetting("DataZone", "datazone", "DATAZONE");
        validateServiceIdSetting("DAX", "dax", "DAX");
        validateServiceIdSetting("Detective", "detective", "DETECTIVE");
        validateServiceIdSetting("Device Farm", "device_farm", "DEVICE_FARM");
        validateServiceIdSetting("DevOps Guru", "devops_guru", "DEVOPS_GURU");
        validateServiceIdSetting("Direct Connect", "direct_connect", "DIRECT_CONNECT");
        validateServiceIdSetting("Application Discovery Service", "application_discovery_service", "APPLICATION_DISCOVERY_SERVICE");
        validateServiceIdSetting("DLM", "dlm", "DLM");
        validateServiceIdSetting("Database Migration Service", "database_migration_service", "DATABASE_MIGRATION_SERVICE");
        validateServiceIdSetting("DocDB", "docdb", "DOCDB");
        validateServiceIdSetting("DocDB Elastic", "docdb_elastic", "DOCDB_ELASTIC");
        validateServiceIdSetting("drs", "drs", "DRS");
        validateServiceIdSetting("Directory Service", "directory_service", "DIRECTORY_SERVICE");
        validateServiceIdSetting("DynamoDB", "dynamodb", "DYNAMODB");
        validateServiceIdSetting("DynamoDB Streams", "dynamodb_streams", "DYNAMODB_STREAMS");
        validateServiceIdSetting("EBS", "ebs", "EBS");
        validateServiceIdSetting("EC2", "ec2", "EC2");
        validateServiceIdSetting("EC2 Instance Connect", "ec2_instance_connect", "EC2_INSTANCE_CONNECT");
        validateServiceIdSetting("ECR", "ecr", "ECR");
        validateServiceIdSetting("ECR PUBLIC", "ecr_public", "ECR_PUBLIC");
        validateServiceIdSetting("ECS", "ecs", "ECS");
        validateServiceIdSetting("EFS", "efs", "EFS");
        validateServiceIdSetting("EKS", "eks", "EKS");
        validateServiceIdSetting("EKS Auth", "eks_auth", "EKS_AUTH");
        validateServiceIdSetting("Elastic Inference", "elastic_inference", "ELASTIC_INFERENCE");
        validateServiceIdSetting("ElastiCache", "elasticache", "ELASTICACHE");
        validateServiceIdSetting("Elastic Beanstalk", "elastic_beanstalk", "ELASTIC_BEANSTALK");
        validateServiceIdSetting("Elastic Transcoder", "elastic_transcoder", "ELASTIC_TRANSCODER");
        validateServiceIdSetting("Elastic Load Balancing", "elastic_load_balancing", "ELASTIC_LOAD_BALANCING");
        validateServiceIdSetting("Elastic Load Balancing v2", "elastic_load_balancing_v2", "ELASTIC_LOAD_BALANCING_V2");
        validateServiceIdSetting("EMR", "emr", "EMR");
        validateServiceIdSetting("EMR containers", "emr_containers", "EMR_CONTAINERS");
        validateServiceIdSetting("EMR Serverless", "emr_serverless", "EMR_SERVERLESS");
        validateServiceIdSetting("EntityResolution", "entityresolution", "ENTITYRESOLUTION");
        validateServiceIdSetting("Elasticsearch Service", "elasticsearch_service", "ELASTICSEARCH_SERVICE");
        validateServiceIdSetting("EventBridge", "eventbridge", "EVENTBRIDGE");
        validateServiceIdSetting("Evidently", "evidently", "EVIDENTLY");
        validateServiceIdSetting("finspace", "finspace", "FINSPACE");
        validateServiceIdSetting("finspace data", "finspace_data", "FINSPACE_DATA");
        validateServiceIdSetting("Firehose", "firehose", "FIREHOSE");
        validateServiceIdSetting("fis", "fis", "FIS");
        validateServiceIdSetting("FMS", "fms", "FMS");
        validateServiceIdSetting("forecast", "forecast", "FORECAST");
        validateServiceIdSetting("forecastquery", "forecastquery", "FORECASTQUERY");
        validateServiceIdSetting("FraudDetector", "frauddetector", "FRAUDDETECTOR");
        validateServiceIdSetting("FreeTier", "freetier", "FREETIER");
        validateServiceIdSetting("FSx", "fsx", "FSX");
        validateServiceIdSetting("GameLift", "gamelift", "GAMELIFT");
        validateServiceIdSetting("Glacier", "glacier", "GLACIER");
        validateServiceIdSetting("Global Accelerator", "global_accelerator", "GLOBAL_ACCELERATOR");
        validateServiceIdSetting("Glue", "glue", "GLUE");
        validateServiceIdSetting("grafana", "grafana", "GRAFANA");
        validateServiceIdSetting("Greengrass", "greengrass", "GREENGRASS");
        validateServiceIdSetting("GreengrassV2", "greengrassv2", "GREENGRASSV2");
        validateServiceIdSetting("GroundStation", "groundstation", "GROUNDSTATION");
        validateServiceIdSetting("GuardDuty", "guardduty", "GUARDDUTY");
        validateServiceIdSetting("Health", "health", "HEALTH");
        validateServiceIdSetting("HealthLake", "healthlake", "HEALTHLAKE");
        validateServiceIdSetting("Honeycode", "honeycode", "HONEYCODE");
        validateServiceIdSetting("IAM", "iam", "IAM");
        validateServiceIdSetting("identitystore", "identitystore", "IDENTITYSTORE");
        validateServiceIdSetting("imagebuilder", "imagebuilder", "IMAGEBUILDER");
        validateServiceIdSetting("ImportExport", "importexport", "IMPORTEXPORT");
        validateServiceIdSetting("Inspector", "inspector", "INSPECTOR");
        validateServiceIdSetting("Inspector Scan", "inspector_scan", "INSPECTOR_SCAN");
        validateServiceIdSetting("Inspector2", "inspector2", "INSPECTOR2");
        validateServiceIdSetting("InternetMonitor", "internetmonitor", "INTERNETMONITOR");
        validateServiceIdSetting("IoT", "iot", "IOT");
        validateServiceIdSetting("IoT Data Plane", "iot_data_plane", "IOT_DATA_PLANE");
        validateServiceIdSetting("IoT Jobs Data Plane", "iot_jobs_data_plane", "IOT_JOBS_DATA_PLANE");
        validateServiceIdSetting("IoT 1Click Devices Service", "iot_1click_devices_service", "IOT_1CLICK_DEVICES_SERVICE");
        validateServiceIdSetting("IoT 1Click Projects", "iot_1click_projects", "IOT_1CLICK_PROJECTS");
        validateServiceIdSetting("IoTAnalytics", "iotanalytics", "IOTANALYTICS");
        validateServiceIdSetting("IotDeviceAdvisor", "iotdeviceadvisor", "IOTDEVICEADVISOR");
        validateServiceIdSetting("IoT Events", "iot_events", "IOT_EVENTS");
        validateServiceIdSetting("IoT Events Data", "iot_events_data", "IOT_EVENTS_DATA");
        validateServiceIdSetting("IoTFleetHub", "iotfleethub", "IOTFLEETHUB");
        validateServiceIdSetting("IoTFleetWise", "iotfleetwise", "IOTFLEETWISE");
        validateServiceIdSetting("IoTSecureTunneling", "iotsecuretunneling", "IOTSECURETUNNELING");
        validateServiceIdSetting("IoTSiteWise", "iotsitewise", "IOTSITEWISE");
        validateServiceIdSetting("IoTThingsGraph", "iotthingsgraph", "IOTTHINGSGRAPH");
        validateServiceIdSetting("IoTTwinMaker", "iottwinmaker", "IOTTWINMAKER");
        validateServiceIdSetting("IoT Wireless", "iot_wireless", "IOT_WIRELESS");
        validateServiceIdSetting("ivs", "ivs", "IVS");
        validateServiceIdSetting("IVS RealTime", "ivs_realtime", "IVS_REALTIME");
        validateServiceIdSetting("ivschat", "ivschat", "IVSCHAT");
        validateServiceIdSetting("Kafka", "kafka", "KAFKA");
        validateServiceIdSetting("KafkaConnect", "kafkaconnect", "KAFKACONNECT");
        validateServiceIdSetting("kendra", "kendra", "KENDRA");
        validateServiceIdSetting("Kendra Ranking", "kendra_ranking", "KENDRA_RANKING");
        validateServiceIdSetting("Keyspaces", "keyspaces", "KEYSPACES");
        validateServiceIdSetting("Kinesis", "kinesis", "KINESIS");
        validateServiceIdSetting("Kinesis Video Archived Media", "kinesis_video_archived_media", "KINESIS_VIDEO_ARCHIVED_MEDIA");
        validateServiceIdSetting("Kinesis Video Media", "kinesis_video_media", "KINESIS_VIDEO_MEDIA");
        validateServiceIdSetting("Kinesis Video Signaling", "kinesis_video_signaling", "KINESIS_VIDEO_SIGNALING");
        validateServiceIdSetting("Kinesis Video WebRTC Storage", "kinesis_video_webrtc_storage", "KINESIS_VIDEO_WEBRTC_STORAGE");
        validateServiceIdSetting("Kinesis Analytics", "kinesis_analytics", "KINESIS_ANALYTICS");
        validateServiceIdSetting("Kinesis Analytics V2", "kinesis_analytics_v2", "KINESIS_ANALYTICS_V2");
        validateServiceIdSetting("Kinesis Video", "kinesis_video", "KINESIS_VIDEO");
        validateServiceIdSetting("KMS", "kms", "KMS");
        validateServiceIdSetting("LakeFormation", "lakeformation", "LAKEFORMATION");
        validateServiceIdSetting("Lambda", "lambda", "LAMBDA");
        validateServiceIdSetting("Launch Wizard", "launch_wizard", "LAUNCH_WIZARD");
        validateServiceIdSetting("Lex Model Building Service", "lex_model_building_service", "LEX_MODEL_BUILDING_SERVICE");
        validateServiceIdSetting("Lex Runtime Service", "lex_runtime_service", "LEX_RUNTIME_SERVICE");
        validateServiceIdSetting("Lex Models V2", "lex_models_v2", "LEX_MODELS_V2");
        validateServiceIdSetting("Lex Runtime V2", "lex_runtime_v2", "LEX_RUNTIME_V2");
        validateServiceIdSetting("License Manager", "license_manager", "LICENSE_MANAGER");
        validateServiceIdSetting("License Manager Linux Subscriptions", "license_manager_linux_subscriptions", "LICENSE_MANAGER_LINUX_SUBSCRIPTIONS");
        validateServiceIdSetting("License Manager User Subscriptions", "license_manager_user_subscriptions", "LICENSE_MANAGER_USER_SUBSCRIPTIONS");
        validateServiceIdSetting("Lightsail", "lightsail", "LIGHTSAIL");
        validateServiceIdSetting("Location", "location", "LOCATION");
        validateServiceIdSetting("CloudWatch Logs", "cloudwatch_logs", "CLOUDWATCH_LOGS");
        validateServiceIdSetting("CloudWatch Logs", "cloudwatch_logs", "CLOUDWATCH_LOGS");
        validateServiceIdSetting("LookoutEquipment", "lookoutequipment", "LOOKOUTEQUIPMENT");
        validateServiceIdSetting("LookoutMetrics", "lookoutmetrics", "LOOKOUTMETRICS");
        validateServiceIdSetting("LookoutVision", "lookoutvision", "LOOKOUTVISION");
        validateServiceIdSetting("m2", "m2", "M2");
        validateServiceIdSetting("Machine Learning", "machine_learning", "MACHINE_LEARNING");
        validateServiceIdSetting("Macie2", "macie2", "MACIE2");
        validateServiceIdSetting("ManagedBlockchain", "managedblockchain", "MANAGEDBLOCKCHAIN");
        validateServiceIdSetting("ManagedBlockchain Query", "managedblockchain_query", "MANAGEDBLOCKCHAIN_QUERY");
        validateServiceIdSetting("Marketplace Agreement", "marketplace_agreement", "MARKETPLACE_AGREEMENT");
        validateServiceIdSetting("Marketplace Catalog", "marketplace_catalog", "MARKETPLACE_CATALOG");
        validateServiceIdSetting("Marketplace Deployment", "marketplace_deployment", "MARKETPLACE_DEPLOYMENT");
        validateServiceIdSetting("Marketplace Entitlement Service", "marketplace_entitlement_service", "MARKETPLACE_ENTITLEMENT_SERVICE");
        validateServiceIdSetting("Marketplace Commerce Analytics", "marketplace_commerce_analytics", "MARKETPLACE_COMMERCE_ANALYTICS");
        validateServiceIdSetting("MediaConnect", "mediaconnect", "MEDIACONNECT");
        validateServiceIdSetting("MediaConvert", "mediaconvert", "MEDIACONVERT");
        validateServiceIdSetting("MediaLive", "medialive", "MEDIALIVE");
        validateServiceIdSetting("MediaPackage", "mediapackage", "MEDIAPACKAGE");
        validateServiceIdSetting("MediaPackage Vod", "mediapackage_vod", "MEDIAPACKAGE_VOD");
        validateServiceIdSetting("MediaPackageV2", "mediapackagev2", "MEDIAPACKAGEV2");
        validateServiceIdSetting("MediaStore", "mediastore", "MEDIASTORE");
        validateServiceIdSetting("MediaStore Data", "mediastore_data", "MEDIASTORE_DATA");
        validateServiceIdSetting("MediaTailor", "mediatailor", "MEDIATAILOR");
        validateServiceIdSetting("Medical Imaging", "medical_imaging", "MEDICAL_IMAGING");
        validateServiceIdSetting("MemoryDB", "memorydb", "MEMORYDB");
        validateServiceIdSetting("Marketplace Metering", "marketplace_metering", "MARKETPLACE_METERING");
        validateServiceIdSetting("Migration Hub", "migration_hub", "MIGRATION_HUB");
        validateServiceIdSetting("mgn", "mgn", "MGN");
        validateServiceIdSetting("Migration Hub Refactor Spaces", "migration_hub_refactor_spaces", "MIGRATION_HUB_REFACTOR_SPACES");
        validateServiceIdSetting("MigrationHub Config", "migrationhub_config", "MIGRATIONHUB_CONFIG");
        validateServiceIdSetting("MigrationHubOrchestrator", "migrationhuborchestrator", "MIGRATIONHUBORCHESTRATOR");
        validateServiceIdSetting("MigrationHubStrategy", "migrationhubstrategy", "MIGRATIONHUBSTRATEGY");
        validateServiceIdSetting("Mobile", "mobile", "MOBILE");
        validateServiceIdSetting("mq", "mq", "MQ");
        validateServiceIdSetting("MTurk", "mturk", "MTURK");
        validateServiceIdSetting("MWAA", "mwaa", "MWAA");
        validateServiceIdSetting("Neptune", "neptune", "NEPTUNE");
        validateServiceIdSetting("Neptune Graph", "neptune_graph", "NEPTUNE_GRAPH");
        validateServiceIdSetting("neptunedata", "neptunedata", "NEPTUNEDATA");
        validateServiceIdSetting("Network Firewall", "network_firewall", "NETWORK_FIREWALL");
        validateServiceIdSetting("NetworkManager", "networkmanager", "NETWORKMANAGER");
        validateServiceIdSetting("NetworkMonitor", "networkmonitor", "NETWORKMONITOR");
        validateServiceIdSetting("nimble", "nimble", "NIMBLE");
        validateServiceIdSetting("OAM", "oam", "OAM");
        validateServiceIdSetting("Omics", "omics", "OMICS");
        validateServiceIdSetting("OpenSearch", "opensearch", "OPENSEARCH");
        validateServiceIdSetting("OpenSearchServerless", "opensearchserverless", "OPENSEARCHSERVERLESS");
        validateServiceIdSetting("OpsWorks", "opsworks", "OPSWORKS");
        validateServiceIdSetting("OpsWorksCM", "opsworkscm", "OPSWORKSCM");
        validateServiceIdSetting("Organizations", "organizations", "ORGANIZATIONS");
        validateServiceIdSetting("OSIS", "osis", "OSIS");
        validateServiceIdSetting("Outposts", "outposts", "OUTPOSTS");
        validateServiceIdSetting("p8data", "p8data", "P8DATA");
        validateServiceIdSetting("p8data", "p8data", "P8DATA");
        validateServiceIdSetting("Panorama", "panorama", "PANORAMA");
        validateServiceIdSetting("Payment Cryptography", "payment_cryptography", "PAYMENT_CRYPTOGRAPHY");
        validateServiceIdSetting("Payment Cryptography Data", "payment_cryptography_data", "PAYMENT_CRYPTOGRAPHY_DATA");
        validateServiceIdSetting("Pca Connector Ad", "pca_connector_ad", "PCA_CONNECTOR_AD");
        validateServiceIdSetting("Personalize", "personalize", "PERSONALIZE");
        validateServiceIdSetting("Personalize Events", "personalize_events", "PERSONALIZE_EVENTS");
        validateServiceIdSetting("Personalize Runtime", "personalize_runtime", "PERSONALIZE_RUNTIME");
        validateServiceIdSetting("PI", "pi", "PI");
        validateServiceIdSetting("Pinpoint", "pinpoint", "PINPOINT");
        validateServiceIdSetting("Pinpoint Email", "pinpoint_email", "PINPOINT_EMAIL");
        validateServiceIdSetting("Pinpoint SMS Voice", "pinpoint_sms_voice", "PINPOINT_SMS_VOICE");
        validateServiceIdSetting("Pinpoint SMS Voice V2", "pinpoint_sms_voice_v2", "PINPOINT_SMS_VOICE_V2");
        validateServiceIdSetting("Pipes", "pipes", "PIPES");
        validateServiceIdSetting("Polly", "polly", "POLLY");
        validateServiceIdSetting("Pricing", "pricing", "PRICING");
        validateServiceIdSetting("PrivateNetworks", "privatenetworks", "PRIVATENETWORKS");
        validateServiceIdSetting("Proton", "proton", "PROTON");
        validateServiceIdSetting("QBusiness", "qbusiness", "QBUSINESS");
        validateServiceIdSetting("QConnect", "qconnect", "QCONNECT");
        validateServiceIdSetting("QLDB", "qldb", "QLDB");
        validateServiceIdSetting("QLDB Session", "qldb_session", "QLDB_SESSION");
        validateServiceIdSetting("QuickSight", "quicksight", "QUICKSIGHT");
        validateServiceIdSetting("RAM", "ram", "RAM");
        validateServiceIdSetting("rbin", "rbin", "RBIN");
        validateServiceIdSetting("RDS", "rds", "RDS");
        validateServiceIdSetting("RDS Data", "rds_data", "RDS_DATA");
        validateServiceIdSetting("Redshift", "redshift", "REDSHIFT");
        validateServiceIdSetting("Redshift Data", "redshift_data", "REDSHIFT_DATA");
        validateServiceIdSetting("Redshift Serverless", "redshift_serverless", "REDSHIFT_SERVERLESS");
        validateServiceIdSetting("Rekognition", "rekognition", "REKOGNITION");
        validateServiceIdSetting("repostspace", "repostspace", "REPOSTSPACE");
        validateServiceIdSetting("resiliencehub", "resiliencehub", "RESILIENCEHUB");
        validateServiceIdSetting("Resource Explorer 2", "resource_explorer_2", "RESOURCE_EXPLORER_2");
        validateServiceIdSetting("Resource Groups", "resource_groups", "RESOURCE_GROUPS");
        validateServiceIdSetting("Resource Groups Tagging API", "resource_groups_tagging_api", "RESOURCE_GROUPS_TAGGING_API");
        validateServiceIdSetting("RoboMaker", "robomaker", "ROBOMAKER");
        validateServiceIdSetting("RolesAnywhere", "rolesanywhere", "ROLESANYWHERE");
        validateServiceIdSetting("Route 53", "route_53", "ROUTE_53");
        validateServiceIdSetting("Route53 Recovery Cluster", "route53_recovery_cluster", "ROUTE53_RECOVERY_CLUSTER");
        validateServiceIdSetting("Route53 Recovery Control Config", "route53_recovery_control_config", "ROUTE53_RECOVERY_CONTROL_CONFIG");
        validateServiceIdSetting("Route53 Recovery Readiness", "route53_recovery_readiness", "ROUTE53_RECOVERY_READINESS");
        validateServiceIdSetting("Route 53 Domains", "route_53_domains", "ROUTE_53_DOMAINS");
        validateServiceIdSetting("Route53Resolver", "route53resolver", "ROUTE53RESOLVER");
        validateServiceIdSetting("RUM", "rum", "RUM");
        validateServiceIdSetting("S3", "s3", "S3");
        validateServiceIdSetting("S3 Control", "s3_control", "S3_CONTROL");
        validateServiceIdSetting("S3Outposts", "s3outposts", "S3OUTPOSTS");
        validateServiceIdSetting("SageMaker", "sagemaker", "SAGEMAKER");
        validateServiceIdSetting("SageMaker A2I Runtime", "sagemaker_a2i_runtime", "SAGEMAKER_A2I_RUNTIME");
        validateServiceIdSetting("Sagemaker Edge", "sagemaker_edge", "SAGEMAKER_EDGE");
        validateServiceIdSetting("SageMaker FeatureStore Runtime", "sagemaker_featurestore_runtime", "SAGEMAKER_FEATURESTORE_RUNTIME");
        validateServiceIdSetting("SageMaker Geospatial", "sagemaker_geospatial", "SAGEMAKER_GEOSPATIAL");
        validateServiceIdSetting("SageMaker Metrics", "sagemaker_metrics", "SAGEMAKER_METRICS");
        validateServiceIdSetting("SageMaker Runtime", "sagemaker_runtime", "SAGEMAKER_RUNTIME");
        validateServiceIdSetting("savingsplans", "savingsplans", "SAVINGSPLANS");
        validateServiceIdSetting("Scheduler", "scheduler", "SCHEDULER");
        validateServiceIdSetting("schemas", "schemas", "SCHEMAS");
        validateServiceIdSetting("SimpleDB", "simpledb", "SIMPLEDB");
        validateServiceIdSetting("Secrets Manager", "secrets_manager", "SECRETS_MANAGER");
        validateServiceIdSetting("SecurityHub", "securityhub", "SECURITYHUB");
        validateServiceIdSetting("SecurityLake", "securitylake", "SECURITYLAKE");
        validateServiceIdSetting("ServerlessApplicationRepository", "serverlessapplicationrepository", "SERVERLESSAPPLICATIONREPOSITORY");
        validateServiceIdSetting("Service Quotas", "service_quotas", "SERVICE_QUOTAS");
        validateServiceIdSetting("Service Catalog", "service_catalog", "SERVICE_CATALOG");
        validateServiceIdSetting("Service Catalog AppRegistry", "service_catalog_appregistry", "SERVICE_CATALOG_APPREGISTRY");
        validateServiceIdSetting("ServiceDiscovery", "servicediscovery", "SERVICEDISCOVERY");
        validateServiceIdSetting("SES", "ses", "SES");
        validateServiceIdSetting("SESv2", "sesv2", "SESV2");
        validateServiceIdSetting("Shield", "shield", "SHIELD");
        validateServiceIdSetting("signer", "signer", "SIGNER");
        validateServiceIdSetting("SimSpaceWeaver", "simspaceweaver", "SIMSPACEWEAVER");
        validateServiceIdSetting("SMS", "sms", "SMS");
        validateServiceIdSetting("Snow Device Management", "snow_device_management", "SNOW_DEVICE_MANAGEMENT");
        validateServiceIdSetting("Snowball", "snowball", "SNOWBALL");
        validateServiceIdSetting("SNS", "sns", "SNS");
        validateServiceIdSetting("SQS", "sqs", "SQS");
        validateServiceIdSetting("SSM", "ssm", "SSM");
        validateServiceIdSetting("SSM Contacts", "ssm_contacts", "SSM_CONTACTS");
        validateServiceIdSetting("SSM Incidents", "ssm_incidents", "SSM_INCIDENTS");
        validateServiceIdSetting("Ssm Sap", "ssm_sap", "SSM_SAP");
        validateServiceIdSetting("SSO", "sso", "SSO");
        validateServiceIdSetting("SSO Admin", "sso_admin", "SSO_ADMIN");
        validateServiceIdSetting("SSO OIDC", "sso_oidc", "SSO_OIDC");
        validateServiceIdSetting("SFN", "sfn", "SFN");
        validateServiceIdSetting("Storage Gateway", "storage_gateway", "STORAGE_GATEWAY");
        validateServiceIdSetting("STS", "sts", "STS");
        validateServiceIdSetting("SupplyChain", "supplychain", "SUPPLYCHAIN");
        validateServiceIdSetting("Support", "support", "SUPPORT");
        validateServiceIdSetting("Support App", "support_app", "SUPPORT_APP");
        validateServiceIdSetting("SWF", "swf", "SWF");
        validateServiceIdSetting("synthetics", "synthetics", "SYNTHETICS");
        validateServiceIdSetting("Textract", "textract", "TEXTRACT");
        validateServiceIdSetting("Timestream InfluxDB", "timestream_influxdb", "TIMESTREAM_INFLUXDB");
        validateServiceIdSetting("Timestream Query", "timestream_query", "TIMESTREAM_QUERY");
        validateServiceIdSetting("Timestream Write", "timestream_write", "TIMESTREAM_WRITE");
        validateServiceIdSetting("tnb", "tnb", "TNB");
        validateServiceIdSetting("Transcribe", "transcribe", "TRANSCRIBE");
        validateServiceIdSetting("Transfer", "transfer", "TRANSFER");
        validateServiceIdSetting("Translate", "translate", "TRANSLATE");
        validateServiceIdSetting("TrustedAdvisor", "trustedadvisor", "TRUSTEDADVISOR");
        validateServiceIdSetting("VerifiedPermissions", "verifiedpermissions", "VERIFIEDPERMISSIONS");
        validateServiceIdSetting("Voice ID", "voice_id", "VOICE_ID");
        validateServiceIdSetting("VPC Lattice", "vpc_lattice", "VPC_LATTICE");
        validateServiceIdSetting("WAF", "waf", "WAF");
        validateServiceIdSetting("WAF Regional", "waf_regional", "WAF_REGIONAL");
        validateServiceIdSetting("WAFV2", "wafv2", "WAFV2");
        validateServiceIdSetting("WellArchitected", "wellarchitected", "WELLARCHITECTED");
        validateServiceIdSetting("Wisdom", "wisdom", "WISDOM");
        validateServiceIdSetting("WorkDocs", "workdocs", "WORKDOCS");
        validateServiceIdSetting("WorkLink", "worklink", "WORKLINK");
        validateServiceIdSetting("WorkMail", "workmail", "WORKMAIL");
        validateServiceIdSetting("WorkMailMessageFlow", "workmailmessageflow", "WORKMAILMESSAGEFLOW");
        validateServiceIdSetting("WorkSpaces", "workspaces", "WORKSPACES");
        validateServiceIdSetting("WorkSpaces Thin Client", "workspaces_thin_client", "WORKSPACES_THIN_CLIENT");
        validateServiceIdSetting("WorkSpaces Web", "workspaces_web", "WORKSPACES_WEB");
        validateServiceIdSetting("XRay", "xray", "XRAY");
    }

    private void validateServiceIdSetting(String serviceId,
                                          String profileProperty,
                                          String environmentVariable) {
        when(serviceMetadata.getServiceId()).thenReturn(serviceId);
        assertThat(strat.getServiceNameForProfileFile())
            .as(() -> serviceId + " uses profile property " + profileProperty)
            .isEqualTo(profileProperty);
        assertThat(strat.getServiceNameForEnvironmentVariables())
            .as(() -> serviceId + " uses environment variable " + environmentVariable)
            .isEqualTo(environmentVariable);
        
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
