package software.amazon.awssdk.regions;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.servicemetadata.A4bServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AccessAnalyzerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AccountServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AcmPcaServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AcmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AirflowServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AmplifyServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AmplifybackendServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiDetectiveServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiEcrPublicServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiEcrServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiElasticInferenceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiFleethubIotServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiMediatailorServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiPricingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiSagemakerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApigatewayServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AppIntegrationsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AppflowServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApplicationAutoscalingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApplicationinsightsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AppmeshServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApprunnerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Appstream2ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AppsyncServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AthenaServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AuditmanagerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AutoscalingPlansServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AutoscalingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.BackupServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.BatchServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.BraketServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.BudgetsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ChimeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Cloud9ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudcontrolapiServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ClouddirectoryServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudformationServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudfrontServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudhsmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Cloudhsmv2ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudsearchServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudtrailServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodeartifactServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodebuildServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodecommitServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodedeployServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodeguruProfilerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodeguruReviewerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodepipelineServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodestarConnectionsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodestarNotificationsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodestarServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CognitoIdentityServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CognitoIdpServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CognitoSyncServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ComprehendServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ComprehendmedicalServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ComputeOptimizerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ConfigServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ConnectServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ConnectparticipantServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ContactLensServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CurServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DataIotServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DataJobsIotServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DataMediastoreServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DatabrewServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DataexchangeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DatapipelineServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DatasyncServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DaxServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DeeplensServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DevicefarmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DevicesIot1clickServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DirectconnectServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DiscoveryServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DlmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DmsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DocdbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DynamodbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EbsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Ec2ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EcsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EdgeSagemakerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EksServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticacheServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticbeanstalkServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticfilesystemServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticloadbalancingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticmapreduceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElastictranscoderServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EmailServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EmrContainersServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EnhancedS3ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EntitlementMarketplaceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EventsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ExecuteApiServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.FinspaceApiServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.FinspaceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.FirehoseServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.FmsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ForecastServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ForecastqueryServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.FrauddetectorServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.FsxServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GameliftServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GlacierServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GlueServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GrafanaServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GreengrassServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GroundstationServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GuarddutyServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.HealthServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.HealthlakeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.HoneycodeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IamServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IdentityChimeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IdentitystoreServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ImportexportServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.InspectorServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IotServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IotanalyticsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IotdeviceadvisorServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IoteventsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IoteventsdataServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IotsecuredtunnelingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IotsitewiseServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IotthingsgraphServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IotwirelessServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IvsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KafkaServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KafkaconnectServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KendraServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KinesisServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KinesisanalyticsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KinesisvideoServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KmsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LakeformationServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LambdaServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LicenseManagerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LightsailServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LogsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LookoutequipmentServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LookoutmetricsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LookoutvisionServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MachinelearningServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Macie2ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MacieServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ManagedblockchainServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MarketplacecommerceanalyticsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MediaconnectServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MediaconvertServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MedialiveServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MediapackageServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MediapackageVodServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MediastoreServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MemorydbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MessagingChimeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MeteringMarketplaceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MghServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MgnServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MigrationhubStrategyServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MobileanalyticsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ModelsLexServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ModelsV2LexServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MonitoringServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MqServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MturkRequesterServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.NeptuneServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.NetworkFirewallServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.NetworkmanagerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.NimbleServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.OidcServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.OperatorServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.OpsworksCmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.OpsworksServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.OrganizationsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.OutpostsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.PersonalizeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.PiServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.PinpointServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.PinpointSmsVoiceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.PollyServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.PortalSsoServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ProfileServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ProjectsIot1clickServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.QldbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.QuicksightServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RamServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RdsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RdsdataserviceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RedshiftServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RekognitionServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ResourceGroupsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RobomakerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Route53RecoveryControlConfigServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Route53ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Route53domainsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Route53resolverServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RuntimeLexServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RuntimeSagemakerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RuntimeV2LexServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.S3ControlServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.S3OutpostsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SavingsplansServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SchemasServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SdbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SecretsmanagerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SecurityhubServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ServerlessrepoServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ServicecatalogAppregistryServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ServicecatalogServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ServicediscoveryServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ServicequotasServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SessionQldbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ShieldServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SignerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SmsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SmsVoicePinpointServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SnowDeviceManagementServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SnowballServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SnsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SqsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SsmIncidentsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SsmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.StatesServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.StoragegatewayServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.StreamsDynamodbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.StsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SupportServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SwfServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SyntheticsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TaggingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TextractServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TimestreamQueryServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TimestreamWriteServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TranscribeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TranscribestreamingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TransferServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TranslateServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ValkyrieServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.VoiceidServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WafRegionalServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WafServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WisdomServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WorkdocsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WorkmailServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WorkspacesServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.XrayServiceMetadata;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class GeneratedServiceMetadataProvider implements ServiceMetadataProvider {
    private static final Map<String, ServiceMetadata> SERVICE_METADATA = new HashMap<>();

    private static ServiceMetadata createServiceMetadata(String endpointPrefix) {
        switch (endpointPrefix) {
            case "a4b":
                return new A4bServiceMetadata();
            case "access-analyzer":
                return new AccessAnalyzerServiceMetadata();
            case "account":
                return new AccountServiceMetadata();
            case "acm":
                return new AcmServiceMetadata();
            case "acm-pca":
                return new AcmPcaServiceMetadata();
            case "airflow":
                return new AirflowServiceMetadata();
            case "amplify":
                return new AmplifyServiceMetadata();
            case "amplifybackend":
                return new AmplifybackendServiceMetadata();
            case "api.detective":
                return new ApiDetectiveServiceMetadata();
            case "api.ecr":
                return new ApiEcrServiceMetadata();
            case "api.ecr-public":
                return new ApiEcrPublicServiceMetadata();
            case "api.elastic-inference":
                return new ApiElasticInferenceServiceMetadata();
            case "api.fleethub.iot":
                return new ApiFleethubIotServiceMetadata();
            case "api.mediatailor":
                return new ApiMediatailorServiceMetadata();
            case "api.pricing":
                return new ApiPricingServiceMetadata();
            case "api.sagemaker":
                return new ApiSagemakerServiceMetadata();
            case "apigateway":
                return new ApigatewayServiceMetadata();
            case "app-integrations":
                return new AppIntegrationsServiceMetadata();
            case "appflow":
                return new AppflowServiceMetadata();
            case "application-autoscaling":
                return new ApplicationAutoscalingServiceMetadata();
            case "applicationinsights":
                return new ApplicationinsightsServiceMetadata();
            case "appmesh":
                return new AppmeshServiceMetadata();
            case "apprunner":
                return new ApprunnerServiceMetadata();
            case "appstream2":
                return new Appstream2ServiceMetadata();
            case "appsync":
                return new AppsyncServiceMetadata();
            case "aps":
                return new ApsServiceMetadata();
            case "athena":
                return new AthenaServiceMetadata();
            case "auditmanager":
                return new AuditmanagerServiceMetadata();
            case "autoscaling":
                return new AutoscalingServiceMetadata();
            case "autoscaling-plans":
                return new AutoscalingPlansServiceMetadata();
            case "backup":
                return new BackupServiceMetadata();
            case "batch":
                return new BatchServiceMetadata();
            case "braket":
                return new BraketServiceMetadata();
            case "budgets":
                return new BudgetsServiceMetadata();
            case "ce":
                return new CeServiceMetadata();
            case "chime":
                return new ChimeServiceMetadata();
            case "cloud9":
                return new Cloud9ServiceMetadata();
            case "cloudcontrolapi":
                return new CloudcontrolapiServiceMetadata();
            case "clouddirectory":
                return new ClouddirectoryServiceMetadata();
            case "cloudformation":
                return new CloudformationServiceMetadata();
            case "cloudfront":
                return new CloudfrontServiceMetadata();
            case "cloudhsm":
                return new CloudhsmServiceMetadata();
            case "cloudhsmv2":
                return new Cloudhsmv2ServiceMetadata();
            case "cloudsearch":
                return new CloudsearchServiceMetadata();
            case "cloudtrail":
                return new CloudtrailServiceMetadata();
            case "codeartifact":
                return new CodeartifactServiceMetadata();
            case "codebuild":
                return new CodebuildServiceMetadata();
            case "codecommit":
                return new CodecommitServiceMetadata();
            case "codedeploy":
                return new CodedeployServiceMetadata();
            case "codeguru-profiler":
                return new CodeguruProfilerServiceMetadata();
            case "codeguru-reviewer":
                return new CodeguruReviewerServiceMetadata();
            case "codepipeline":
                return new CodepipelineServiceMetadata();
            case "codestar":
                return new CodestarServiceMetadata();
            case "codestar-connections":
                return new CodestarConnectionsServiceMetadata();
            case "codestar-notifications":
                return new CodestarNotificationsServiceMetadata();
            case "cognito-identity":
                return new CognitoIdentityServiceMetadata();
            case "cognito-idp":
                return new CognitoIdpServiceMetadata();
            case "cognito-sync":
                return new CognitoSyncServiceMetadata();
            case "comprehend":
                return new ComprehendServiceMetadata();
            case "comprehendmedical":
                return new ComprehendmedicalServiceMetadata();
            case "compute-optimizer":
                return new ComputeOptimizerServiceMetadata();
            case "config":
                return new ConfigServiceMetadata();
            case "connect":
                return new ConnectServiceMetadata();
            case "connectparticipant":
                return new ConnectparticipantServiceMetadata();
            case "contact-lens":
                return new ContactLensServiceMetadata();
            case "cur":
                return new CurServiceMetadata();
            case "data.iot":
                return new DataIotServiceMetadata();
            case "data.jobs.iot":
                return new DataJobsIotServiceMetadata();
            case "data.mediastore":
                return new DataMediastoreServiceMetadata();
            case "databrew":
                return new DatabrewServiceMetadata();
            case "dataexchange":
                return new DataexchangeServiceMetadata();
            case "datapipeline":
                return new DatapipelineServiceMetadata();
            case "datasync":
                return new DatasyncServiceMetadata();
            case "dax":
                return new DaxServiceMetadata();
            case "deeplens":
                return new DeeplensServiceMetadata();
            case "devicefarm":
                return new DevicefarmServiceMetadata();
            case "devices.iot1click":
                return new DevicesIot1clickServiceMetadata();
            case "directconnect":
                return new DirectconnectServiceMetadata();
            case "discovery":
                return new DiscoveryServiceMetadata();
            case "dlm":
                return new DlmServiceMetadata();
            case "dms":
                return new DmsServiceMetadata();
            case "docdb":
                return new DocdbServiceMetadata();
            case "ds":
                return new DsServiceMetadata();
            case "dynamodb":
                return new DynamodbServiceMetadata();
            case "ebs":
                return new EbsServiceMetadata();
            case "ec2":
                return new Ec2ServiceMetadata();
            case "ecs":
                return new EcsServiceMetadata();
            case "edge.sagemaker":
                return new EdgeSagemakerServiceMetadata();
            case "eks":
                return new EksServiceMetadata();
            case "elasticache":
                return new ElasticacheServiceMetadata();
            case "elasticbeanstalk":
                return new ElasticbeanstalkServiceMetadata();
            case "elasticfilesystem":
                return new ElasticfilesystemServiceMetadata();
            case "elasticloadbalancing":
                return new ElasticloadbalancingServiceMetadata();
            case "elasticmapreduce":
                return new ElasticmapreduceServiceMetadata();
            case "elastictranscoder":
                return new ElastictranscoderServiceMetadata();
            case "email":
                return new EmailServiceMetadata();
            case "emr-containers":
                return new EmrContainersServiceMetadata();
            case "entitlement.marketplace":
                return new EntitlementMarketplaceServiceMetadata();
            case "es":
                return new EsServiceMetadata();
            case "events":
                return new EventsServiceMetadata();
            case "execute-api":
                return new ExecuteApiServiceMetadata();
            case "finspace":
                return new FinspaceServiceMetadata();
            case "finspace-api":
                return new FinspaceApiServiceMetadata();
            case "firehose":
                return new FirehoseServiceMetadata();
            case "fms":
                return new FmsServiceMetadata();
            case "forecast":
                return new ForecastServiceMetadata();
            case "forecastquery":
                return new ForecastqueryServiceMetadata();
            case "frauddetector":
                return new FrauddetectorServiceMetadata();
            case "fsx":
                return new FsxServiceMetadata();
            case "gamelift":
                return new GameliftServiceMetadata();
            case "glacier":
                return new GlacierServiceMetadata();
            case "glue":
                return new GlueServiceMetadata();
            case "grafana":
                return new GrafanaServiceMetadata();
            case "greengrass":
                return new GreengrassServiceMetadata();
            case "groundstation":
                return new GroundstationServiceMetadata();
            case "guardduty":
                return new GuarddutyServiceMetadata();
            case "health":
                return new HealthServiceMetadata();
            case "healthlake":
                return new HealthlakeServiceMetadata();
            case "honeycode":
                return new HoneycodeServiceMetadata();
            case "iam":
                return new IamServiceMetadata();
            case "identity-chime":
                return new IdentityChimeServiceMetadata();
            case "identitystore":
                return new IdentitystoreServiceMetadata();
            case "importexport":
                return new ImportexportServiceMetadata();
            case "inspector":
                return new InspectorServiceMetadata();
            case "iot":
                return new IotServiceMetadata();
            case "iotanalytics":
                return new IotanalyticsServiceMetadata();
            case "iotdeviceadvisor":
                return new IotdeviceadvisorServiceMetadata();
            case "iotevents":
                return new IoteventsServiceMetadata();
            case "ioteventsdata":
                return new IoteventsdataServiceMetadata();
            case "iotsecuredtunneling":
                return new IotsecuredtunnelingServiceMetadata();
            case "iotsitewise":
                return new IotsitewiseServiceMetadata();
            case "iotthingsgraph":
                return new IotthingsgraphServiceMetadata();
            case "iotwireless":
                return new IotwirelessServiceMetadata();
            case "ivs":
                return new IvsServiceMetadata();
            case "kafka":
                return new KafkaServiceMetadata();
            case "kafkaconnect":
                return new KafkaconnectServiceMetadata();
            case "kendra":
                return new KendraServiceMetadata();
            case "kinesis":
                return new KinesisServiceMetadata();
            case "kinesisanalytics":
                return new KinesisanalyticsServiceMetadata();
            case "kinesisvideo":
                return new KinesisvideoServiceMetadata();
            case "kms":
                return new KmsServiceMetadata();
            case "lakeformation":
                return new LakeformationServiceMetadata();
            case "lambda":
                return new LambdaServiceMetadata();
            case "license-manager":
                return new LicenseManagerServiceMetadata();
            case "lightsail":
                return new LightsailServiceMetadata();
            case "logs":
                return new LogsServiceMetadata();
            case "lookoutequipment":
                return new LookoutequipmentServiceMetadata();
            case "lookoutmetrics":
                return new LookoutmetricsServiceMetadata();
            case "lookoutvision":
                return new LookoutvisionServiceMetadata();
            case "machinelearning":
                return new MachinelearningServiceMetadata();
            case "macie":
                return new MacieServiceMetadata();
            case "macie2":
                return new Macie2ServiceMetadata();
            case "managedblockchain":
                return new ManagedblockchainServiceMetadata();
            case "marketplacecommerceanalytics":
                return new MarketplacecommerceanalyticsServiceMetadata();
            case "mediaconnect":
                return new MediaconnectServiceMetadata();
            case "mediaconvert":
                return new MediaconvertServiceMetadata();
            case "medialive":
                return new MedialiveServiceMetadata();
            case "mediapackage":
                return new MediapackageServiceMetadata();
            case "mediapackage-vod":
                return new MediapackageVodServiceMetadata();
            case "mediastore":
                return new MediastoreServiceMetadata();
            case "memorydb":
                return new MemorydbServiceMetadata();
            case "messaging-chime":
                return new MessagingChimeServiceMetadata();
            case "metering.marketplace":
                return new MeteringMarketplaceServiceMetadata();
            case "mgh":
                return new MghServiceMetadata();
            case "mgn":
                return new MgnServiceMetadata();
            case "migrationhub-strategy":
                return new MigrationhubStrategyServiceMetadata();
            case "mobileanalytics":
                return new MobileanalyticsServiceMetadata();
            case "models-v2-lex":
                return new ModelsV2LexServiceMetadata();
            case "models.lex":
                return new ModelsLexServiceMetadata();
            case "monitoring":
                return new MonitoringServiceMetadata();
            case "mq":
                return new MqServiceMetadata();
            case "mturk-requester":
                return new MturkRequesterServiceMetadata();
            case "neptune":
                return new NeptuneServiceMetadata();
            case "network-firewall":
                return new NetworkFirewallServiceMetadata();
            case "networkmanager":
                return new NetworkmanagerServiceMetadata();
            case "nimble":
                return new NimbleServiceMetadata();
            case "oidc":
                return new OidcServiceMetadata();
            case "opsworks":
                return new OpsworksServiceMetadata();
            case "opsworks-cm":
                return new OpsworksCmServiceMetadata();
            case "organizations":
                return new OrganizationsServiceMetadata();
            case "outposts":
                return new OutpostsServiceMetadata();
            case "personalize":
                return new PersonalizeServiceMetadata();
            case "pi":
                return new PiServiceMetadata();
            case "pinpoint":
                return new PinpointServiceMetadata();
            case "pinpoint-sms-voice":
                return new PinpointSmsVoiceServiceMetadata();
            case "polly":
                return new PollyServiceMetadata();
            case "portal.sso":
                return new PortalSsoServiceMetadata();
            case "profile":
                return new ProfileServiceMetadata();
            case "projects.iot1click":
                return new ProjectsIot1clickServiceMetadata();
            case "qldb":
                return new QldbServiceMetadata();
            case "quicksight":
                return new QuicksightServiceMetadata();
            case "ram":
                return new RamServiceMetadata();
            case "rds":
                return new RdsServiceMetadata();
            case "rdsdataservice":
                return new RdsdataserviceServiceMetadata();
            case "redshift":
                return new RedshiftServiceMetadata();
            case "rekognition":
                return new RekognitionServiceMetadata();
            case "resource-groups":
                return new ResourceGroupsServiceMetadata();
            case "robomaker":
                return new RobomakerServiceMetadata();
            case "route53":
                return new Route53ServiceMetadata();
            case "route53-recovery-control-config":
                return new Route53RecoveryControlConfigServiceMetadata();
            case "route53domains":
                return new Route53domainsServiceMetadata();
            case "route53resolver":
                return new Route53resolverServiceMetadata();
            case "runtime-v2-lex":
                return new RuntimeV2LexServiceMetadata();
            case "runtime.lex":
                return new RuntimeLexServiceMetadata();
            case "runtime.sagemaker":
                return new RuntimeSagemakerServiceMetadata();
            case "s3":
                return new EnhancedS3ServiceMetadata();
            case "s3-control":
                return new S3ControlServiceMetadata();
            case "s3-outposts":
                return new S3OutpostsServiceMetadata();
            case "savingsplans":
                return new SavingsplansServiceMetadata();
            case "schemas":
                return new SchemasServiceMetadata();
            case "sdb":
                return new SdbServiceMetadata();
            case "secretsmanager":
                return new SecretsmanagerServiceMetadata();
            case "securityhub":
                return new SecurityhubServiceMetadata();
            case "serverlessrepo":
                return new ServerlessrepoServiceMetadata();
            case "servicecatalog":
                return new ServicecatalogServiceMetadata();
            case "servicecatalog-appregistry":
                return new ServicecatalogAppregistryServiceMetadata();
            case "servicediscovery":
                return new ServicediscoveryServiceMetadata();
            case "servicequotas":
                return new ServicequotasServiceMetadata();
            case "session.qldb":
                return new SessionQldbServiceMetadata();
            case "shield":
                return new ShieldServiceMetadata();
            case "signer":
                return new SignerServiceMetadata();
            case "sms":
                return new SmsServiceMetadata();
            case "sms-voice.pinpoint":
                return new SmsVoicePinpointServiceMetadata();
            case "snow-device-management":
                return new SnowDeviceManagementServiceMetadata();
            case "snowball":
                return new SnowballServiceMetadata();
            case "sns":
                return new SnsServiceMetadata();
            case "sqs":
                return new SqsServiceMetadata();
            case "ssm":
                return new SsmServiceMetadata();
            case "ssm-incidents":
                return new SsmIncidentsServiceMetadata();
            case "states":
                return new StatesServiceMetadata();
            case "storagegateway":
                return new StoragegatewayServiceMetadata();
            case "streams.dynamodb":
                return new StreamsDynamodbServiceMetadata();
            case "sts":
                return new StsServiceMetadata();
            case "support":
                return new SupportServiceMetadata();
            case "swf":
                return new SwfServiceMetadata();
            case "synthetics":
                return new SyntheticsServiceMetadata();
            case "tagging":
                return new TaggingServiceMetadata();
            case "textract":
                return new TextractServiceMetadata();
            case "timestream.query":
                return new TimestreamQueryServiceMetadata();
            case "timestream.write":
                return new TimestreamWriteServiceMetadata();
            case "transcribe":
                return new TranscribeServiceMetadata();
            case "transcribestreaming":
                return new TranscribestreamingServiceMetadata();
            case "transfer":
                return new TransferServiceMetadata();
            case "translate":
                return new TranslateServiceMetadata();
            case "valkyrie":
                return new ValkyrieServiceMetadata();
            case "voiceid":
                return new VoiceidServiceMetadata();
            case "waf":
                return new WafServiceMetadata();
            case "waf-regional":
                return new WafRegionalServiceMetadata();
            case "wisdom":
                return new WisdomServiceMetadata();
            case "workdocs":
                return new WorkdocsServiceMetadata();
            case "workmail":
                return new WorkmailServiceMetadata();
            case "workspaces":
                return new WorkspacesServiceMetadata();
            case "xray":
                return new XrayServiceMetadata();
            case "operator":
                return new OperatorServiceMetadata();
            default:
                throw new IllegalStateException("Unexpected value: " + endpointPrefix);
        }
    }

    private static ServiceMetadata getServiceMetadata(String endpointPrefix) {
        if (!SERVICE_METADATA.containsKey(endpointPrefix)) {
            SERVICE_METADATA.put(endpointPrefix, createServiceMetadata(endpointPrefix));
        }
        return SERVICE_METADATA.get(endpointPrefix);
    }

    @Override
    public ServiceMetadata serviceMetadata(String endpointPrefix) {
        return getServiceMetadata(endpointPrefix);
    }
}