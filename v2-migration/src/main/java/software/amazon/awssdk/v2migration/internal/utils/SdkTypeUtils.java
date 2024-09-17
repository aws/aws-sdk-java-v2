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

package software.amazon.awssdk.v2migration.internal.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProcessCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider;
import software.amazon.awssdk.utils.ImmutableMap;

/**
 * Type creation and checking utilities.
 */
@SdkInternalApi
public final class SdkTypeUtils {
    /**
     * V2 core classes with a static factory method
     */
    public static final Map<String, Integer> V2_CORE_CLASSES_WITH_STATIC_FACTORY =
        ImmutableMap.<String, Integer>builder()
                    .put(EnvironmentVariableCredentialsProvider.class.getCanonicalName(), 0)
                    .put(InstanceProfileCredentialsProvider.class.getCanonicalName(), 0)
                    .put(AwsBasicCredentials.class.getCanonicalName(), 2)
                    .put(AwsSessionCredentials.class.getCanonicalName(), 3)
                    .put(StaticCredentialsProvider.class.getCanonicalName(), 1)
                    .build();

    private static final Pattern V1_SERVICE_CLASS_PATTERN =
        Pattern.compile("com\\.amazonaws\\.services\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+");
    private static final Pattern V1_SERVICE_MODEL_CLASS_PATTERN =
        Pattern.compile("com\\.amazonaws\\.services\\.[a-zA-Z0-9]+\\.model\\.[a-zA-Z0-9]+");

    private static final Pattern V1_SERVICE_CLIENT_CLASS_PATTERN =
        Pattern.compile("com\\.amazonaws\\.services\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+");
    private static final Pattern V1_SERVICE_CLIENT_BUILDER_CLASS_PATTERN =
        Pattern.compile("com\\.amazonaws\\.services\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+Builder");

    private static final Pattern V2_MODEL_BUILDER_PATTERN =
        Pattern.compile("software\\.amazon\\.awssdk\\.services\\.[a-zA-Z0-9]+\\.model\\.[a-zA-Z0-9]+\\.Builder");
    private static final Pattern V2_MODEL_CLASS_PATTERN = Pattern.compile(
        "software\\.amazon\\.awssdk\\.services\\.[a-zA-Z0-9]+\\.model\\..[a-zA-Z0-9]+");
    private static final Pattern V2_CLIENT_CLASS_PATTERN = Pattern.compile(
        "software\\.amazon\\.awssdk\\.services\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+");

    private static final Pattern V2_ASYNC_CLIENT_CLASS_PATTERN = Pattern.compile(
        "software\\.amazon\\.awssdk\\.services\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+AsyncClient");

    /**
     * V2 core classes with a builder
     */
    private static final Set<String> V2_CORE_CLASSES_WITH_BUILDER =
        new HashSet<>(Arrays.asList(ClientOverrideConfiguration.class.getCanonicalName(),
                                    DefaultCredentialsProvider.class.getCanonicalName(),
                                    ProfileCredentialsProvider.class.getCanonicalName(),
                                    ContainerCredentialsProvider.class.getCanonicalName(),
                                    InstanceProfileCredentialsProvider.class.getCanonicalName(),
                                    StsAssumeRoleCredentialsProvider.class.getCanonicalName(),
                                    StsGetSessionTokenCredentialsProvider.class.getCanonicalName(),
                                    StsAssumeRoleWithWebIdentityCredentialsProvider.class.getCanonicalName(),
                                    ProcessCredentialsProvider.class.getCanonicalName()));

    private static final Pattern V2_CLIENT_BUILDER_PATTERN = Pattern.compile(
        "software\\.amazon\\.awssdk\\.services\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+Builder");

    private static final Set<String> V1_SERVICES_PACKAGE_NAMES = new HashSet<>();

    static {
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sagemakeredgemanager");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.medialive");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudhsm");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.comprehendmedical");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudsearchv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudsearchdomain");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.billingconductor");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.support");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.memorydb");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kinesisvideowebrtcstorage");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kinesisvideosignalingchannels");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.chime");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.inspector2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.taxsettings");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.rds");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.paymentcryptographydata");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.verifiedpermissions");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ecrpublic");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.internetmonitor");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ec2.util");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ec2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.tnb");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.securitytoken");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.translate");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.inspector");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.datasync");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.certificatemanager");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codepipeline");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.braket");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.appconfigdata");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.qldbsession");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.gluedatabrew");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.workdocs");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.amplify");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.bedrock");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.outposts");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ram");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.macie2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.elasticfilesystem");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.simpleemailv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.logs");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.servicediscovery");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.importexport");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.workspacesweb");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.appflow");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.chimesdkvoice");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lookoutmetrics");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.s3.model");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.s3");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.repostspace");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.health");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.amplifyuibuilder");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.databasemigrationservice");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.dax");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.clouddirectory");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.costexplorer");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.elasticloadbalancing");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.omics");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.timestreaminfluxdb");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.workmailmessageflow");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codebuild");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.workmail");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ssooidc");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudwatch");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.servermigration");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.stepfunctions");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ssmincidents");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.trustedadvisor");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.wellarchitected");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.elasticsearch");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.quicksight");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.forecastquery");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.bedrockagent");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.migrationhub");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.imagebuilder");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.medicalimaging");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.appsync");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.paymentcryptography");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.budgets");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.controlcatalog");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotfleetwise");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.forecast");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.chimesdkidentity");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.networkfirewall");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.computeoptimizer");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudformation");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.simpleemail");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.pinpoint");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mturk");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.autoscalingplans");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.elastictranscoder");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.neptunedata");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.qapps");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.dlm");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.workspaces");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mediaconvert");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.connect");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.account");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sagemakerfeaturestoreruntime");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.apigateway");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mediapackage");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.amplifybackend");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.proton");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.config");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.wafv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.qconnect");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.emrserverless");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.scheduler");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mailmanager");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mainframemodernization");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.dynamodbv2.xspec");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.dynamodbv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.personalize");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.managedgrafana");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codegurusecurity");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ivschat");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.s3outposts");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudwatchrum");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ivs");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.directory");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.costandusagereport");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.connectcampaign");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotanalytics");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.identitystore");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.migrationhuborchestrator");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.eksauth");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codeconnections");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.transfer");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.waf");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.greengrassv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.supportapp");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.deadline");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cognitosync");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.route53recoverycontrolconfig");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.connectcontactlens");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.appfabric");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cleanroomsml");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.licensemanagerusersubscriptions");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.appregistry");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.eventbridge");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mediaconnect");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.costoptimizationhub");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ec2instanceconnect");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.migrationhubstrategyrecommendations");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iottwinmaker");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.customerprofiles");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.route53domains");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.route53");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.freetier");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotsecuretunneling");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sagemakergeospatial");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.backupgateway");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.s3control");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kinesisanalyticsv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mediapackagevod");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kinesisvideo");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.route53recoverycluster");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.docdbelastic");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.marketplacemetering");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.applicationautoscaling");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mediapackagev2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ebs");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.qbusiness");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.simpleworkflow");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cognitoidp");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codestarnotifications");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.networkmonitor");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotfleethub");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotwireless");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.eks");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.controltower");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.drs");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.opensearch");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ssmcontacts");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.applicationdiscovery");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.private5g");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudfront");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.location");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.timestreamquery");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.marketplacedeployment");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.route53recoveryreadiness");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.chimesdkmediapipelines");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.shield");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.synthetics");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.xray");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.elasticloadbalancingv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.marketplaceagreement");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.signer");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.directconnect");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codegurureviewer");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.securitylake");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sso");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lexmodelbuilding");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mgn");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lightsail");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotjobsdataplane");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.pricing");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.vpclattice");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.identitymanagement");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.datapipeline");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.marketplacecommerceanalytics");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.globalaccelerator");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mq");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.marketplaceentitlement");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.resourcegroups");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.pipes");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.artifact");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.finspace");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.opensearchserverless");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.athena");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ecr");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.storagegateway");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.panorama");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.managedblockchainquery");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.bedrockagentruntime");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cognitoidentity");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotdata");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iot");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sqs");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.pinpointsmsvoicev2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.rekognition");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.dataexchange");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.fsx");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.snowdevicemanagement");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.finspacedata");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.inspectorscan");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.recyclebin");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.detective");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mwaa");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.applicationcostprofiler");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotevents");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ivsrealtime");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ecs");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.bedrockruntime");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kinesis");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kinesisanalytics");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kinesisfirehose");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudhsmv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.glue");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codedeploy");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.simpledb.util");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.simpledb");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.launchwizard");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.personalizeevents");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.devopsguru");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.opsworks");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kafka");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.voiceid");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudcontrolapi");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sagemakerruntime");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.textract");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.batch");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.appconfig");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudwatchevents");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codeartifact");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.autoscaling");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.resiliencehub");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.securityhub");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iot1clickdevices");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.backup");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotdeviceadvisor");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mediastore");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.serverlessapplicationrepository");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.fms");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.timestreamwrite");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ioteventsdata");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codeguruprofiler");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lexruntimev2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.elasticache");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kendra");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.snowball");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotthingsgraph");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.pcaconnectorscep");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iotsitewise");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lexmodelsv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.redshiftserverless");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.migrationhubconfig");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.pi");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kendraranking");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.osis");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.bcmdataexports");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.accessanalyzer");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.appintegrations");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.b2bi");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.keyspaces");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.elasticinference");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.redshift");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudtrail");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.connectparticipant");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.gamelift");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.acmpca");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.simspaceweaver");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lookoutequipment");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.pinpointsmsvoice");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codestarconnections");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.personalizeruntime");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lambda");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.emrcontainers");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sns.util");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sns");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.chimesdkmeetings");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.entityresolution");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.networkmanager");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.datazone");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.savingsplans");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.resourcegroupstaggingapi");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.organizations");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ssoadmin");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lakeformation");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kafkaconnect");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.glacier");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.docdb");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.appstream");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.applicationsignals");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudwatchevidently");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.healthlake");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sagemaker");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codecommit");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.frauddetector");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.apptest");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.transcribe");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.migrationhubrefactorspaces");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.apprunner");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.machinelearning");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.servicequotas");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.marketplacecatalog");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.route53resolver");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.nimblestudio");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.rdsdata");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.fis");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.chimesdkmessaging");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lookoutforvision");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.chatbot");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.resourceexplorer2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloudtraildata");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.devicefarm");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lexruntime");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.arczonalshift");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.ssmsap");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.comprehend");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.connectwisdom");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.licensemanagerlinuxsubscriptions");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.polly");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.appmesh");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.schemas");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.managedblockchain");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.guardduty");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cloud9");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.greengrass");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.simplesystemsmanagement");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.connectcases");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.licensemanager");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mediastoredata");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.servicecatalog");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.oam");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.secretsmanager");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.qldb");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.redshiftdataapi");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.opsworkscm");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.pinpointemail");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.apigatewayv2");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.cleanrooms");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.elasticmapreduce");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.pcaconnectorad");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iot1clickprojects");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.elasticbeanstalk");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.worklink");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.supplychain");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.sagemakermetrics");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.groundstation");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.prometheus");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.apigatewaymanagementapi");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.neptune");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.workspacesthinclient");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.codestar");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.route53profiles");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.augmentedairuntime");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.auditmanager");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.iamrolesanywhere");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.mediatailor");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.kms");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.robomaker");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.applicationinsights");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.s3.transfer");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.dynamodbv2.datamodeling");
        V1_SERVICES_PACKAGE_NAMES.add("com.amazonaws.services.lambda.invoke");
    }

    private SdkTypeUtils() {
    }

    public static boolean isV1Class(JavaType type) {
        return type != null && type.isAssignableFrom(V1_SERVICE_CLASS_PATTERN);
    }

    public static boolean isCustomSdk(String fullyQualifiedName) {
        int lastIndexOfDot = fullyQualifiedName.lastIndexOf('.');
        // i.e.., com.amazonaws.services.SERVICE.CLASSNAME
        String packagePrefix = fullyQualifiedName.substring(0, lastIndexOfDot);

        int lastIndexOfDotModelClass = packagePrefix.lastIndexOf('.');
        // i.e., com.amazonaws.services.SERVICE.model.CLASSNAME
        String packagePrefixModelClass = packagePrefix.substring(0, lastIndexOfDotModelClass);

        return !V1_SERVICES_PACKAGE_NAMES.contains(packagePrefix) && !V1_SERVICES_PACKAGE_NAMES.contains(packagePrefixModelClass);
    }

    public static boolean isV1ModelClass(JavaType type) {
        return type != null
                && type instanceof JavaType.FullyQualified
                && type.isAssignableFrom(V1_SERVICE_MODEL_CLASS_PATTERN);
    }

    public static boolean isV1ClientClass(JavaType type) {
        return type != null
               && type instanceof JavaType.FullyQualified
               && type.isAssignableFrom(V1_SERVICE_CLIENT_CLASS_PATTERN);
    }

    public static boolean isV2ModelBuilder(JavaType type) {
        return type != null
                && type.isAssignableFrom(V2_MODEL_BUILDER_PATTERN);
    }

    public static boolean isV2ModelClass(JavaType type) {
        return type != null
                && type.isAssignableFrom(V2_MODEL_CLASS_PATTERN);
    }

    public static boolean isV2ClientClass(JavaType type) {
        return type != null
               && type.isAssignableFrom(V2_CLIENT_CLASS_PATTERN);
    }

    public static boolean isV2AsyncClientClass(JavaType type) {
        return type != null
               && type.isAssignableFrom(V2_ASYNC_CLIENT_CLASS_PATTERN);
    }

    public static boolean isV2ClientBuilder(JavaType type) {
        return type != null
               && type.isAssignableFrom(V2_CLIENT_BUILDER_PATTERN);
    }

    public static boolean isEligibleToConvertToBuilder(JavaType.FullyQualified type) {
        if (type == null) {
            return false;
        }
        return isV2ModelClass(type) || isV2ClientClass(type) || isV2CoreClassesWithBuilder(type.getFullyQualifiedName());
    }

    public static boolean isEligibleToConvertToStaticFactory(JavaType.FullyQualified type) {
        return type != null && V2_CORE_CLASSES_WITH_STATIC_FACTORY.containsKey(type.getFullyQualifiedName());
    }

    private static boolean isV2CoreClassesWithBuilder(String fqcn) {
        return V2_CORE_CLASSES_WITH_BUILDER.contains(fqcn);
    }

    public static JavaType.FullyQualified v2Builder(JavaType.FullyQualified type) {
        if (!isEligibleToConvertToBuilder(type)) {
            throw new IllegalArgumentException(String.format("%s cannot be converted to builder", type));
        }
        String fqcn;
        if (isV2ModelClass(type)) {
            fqcn = String.format("%s.%s", type.getFullyQualifiedName(), "Builder");
        } else {
            fqcn = String.format("%s%s", type.getFullyQualifiedName(), "Builder");
        }
        
        return TypeUtils.asFullyQualified(JavaType.buildType(fqcn));
    }

    public static JavaType.FullyQualified v2ClientFromClientBuilder(JavaType.FullyQualified type) {
        if (!isV2ClientBuilder(type)) {
            throw new IllegalArgumentException(String.format("%s is not a client builder", type));
        }

        String builder = type.getFullyQualifiedName().replace("Builder", "");
        return TypeUtils.asFullyQualified(JavaType.buildType(builder));
    }
}
