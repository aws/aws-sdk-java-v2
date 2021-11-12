package software.amazon.awssdk.regions;

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
import software.amazon.awssdk.utils.ImmutableMap;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class GeneratedServiceMetadataProvider implements ServiceMetadataProvider {
    private static final Map<String, ServiceMetadata> SERVICE_METADATA = ImmutableMap.<String, ServiceMetadata> builder()
                                                                                     .put("a4b", new A4bServiceMetadata()).put("access-analyzer", new AccessAnalyzerServiceMetadata())
                                                                                     .put("account", new AccountServiceMetadata()).put("acm", new AcmServiceMetadata())
                                                                                     .put("acm-pca", new AcmPcaServiceMetadata()).put("airflow", new AirflowServiceMetadata())
                                                                                     .put("amplify", new AmplifyServiceMetadata()).put("amplifybackend", new AmplifybackendServiceMetadata())
                                                                                     .put("api.detective", new ApiDetectiveServiceMetadata()).put("api.ecr", new ApiEcrServiceMetadata())
                                                                                     .put("api.ecr-public", new ApiEcrPublicServiceMetadata())
                                                                                     .put("api.elastic-inference", new ApiElasticInferenceServiceMetadata())
                                                                                     .put("api.fleethub.iot", new ApiFleethubIotServiceMetadata())
                                                                                     .put("api.mediatailor", new ApiMediatailorServiceMetadata()).put("api.pricing", new ApiPricingServiceMetadata())
                                                                                     .put("api.sagemaker", new ApiSagemakerServiceMetadata()).put("apigateway", new ApigatewayServiceMetadata())
                                                                                     .put("app-integrations", new AppIntegrationsServiceMetadata()).put("appflow", new AppflowServiceMetadata())
                                                                                     .put("application-autoscaling", new ApplicationAutoscalingServiceMetadata())
                                                                                     .put("applicationinsights", new ApplicationinsightsServiceMetadata()).put("appmesh", new AppmeshServiceMetadata())
                                                                                     .put("apprunner", new ApprunnerServiceMetadata()).put("appstream2", new Appstream2ServiceMetadata())
                                                                                     .put("appsync", new AppsyncServiceMetadata()).put("aps", new ApsServiceMetadata())
                                                                                     .put("athena", new AthenaServiceMetadata()).put("auditmanager", new AuditmanagerServiceMetadata())
                                                                                     .put("autoscaling", new AutoscalingServiceMetadata()).put("autoscaling-plans", new AutoscalingPlansServiceMetadata())
                                                                                     .put("backup", new BackupServiceMetadata()).put("batch", new BatchServiceMetadata())
                                                                                     .put("braket", new BraketServiceMetadata()).put("budgets", new BudgetsServiceMetadata())
                                                                                     .put("ce", new CeServiceMetadata()).put("chime", new ChimeServiceMetadata())
                                                                                     .put("cloud9", new Cloud9ServiceMetadata()).put("cloudcontrolapi", new CloudcontrolapiServiceMetadata())
                                                                                     .put("clouddirectory", new ClouddirectoryServiceMetadata())
                                                                                     .put("cloudformation", new CloudformationServiceMetadata()).put("cloudfront", new CloudfrontServiceMetadata())
                                                                                     .put("cloudhsm", new CloudhsmServiceMetadata()).put("cloudhsmv2", new Cloudhsmv2ServiceMetadata())
                                                                                     .put("cloudsearch", new CloudsearchServiceMetadata()).put("cloudtrail", new CloudtrailServiceMetadata())
                                                                                     .put("codeartifact", new CodeartifactServiceMetadata()).put("codebuild", new CodebuildServiceMetadata())
                                                                                     .put("codecommit", new CodecommitServiceMetadata()).put("codedeploy", new CodedeployServiceMetadata())
                                                                                     .put("codeguru-profiler", new CodeguruProfilerServiceMetadata())
                                                                                     .put("codeguru-reviewer", new CodeguruReviewerServiceMetadata())
                                                                                     .put("codepipeline", new CodepipelineServiceMetadata()).put("codestar", new CodestarServiceMetadata())
                                                                                     .put("codestar-connections", new CodestarConnectionsServiceMetadata())
                                                                                     .put("codestar-notifications", new CodestarNotificationsServiceMetadata())
                                                                                     .put("cognito-identity", new CognitoIdentityServiceMetadata()).put("cognito-idp", new CognitoIdpServiceMetadata())
                                                                                     .put("cognito-sync", new CognitoSyncServiceMetadata()).put("comprehend", new ComprehendServiceMetadata())
                                                                                     .put("comprehendmedical", new ComprehendmedicalServiceMetadata())
                                                                                     .put("compute-optimizer", new ComputeOptimizerServiceMetadata()).put("config", new ConfigServiceMetadata())
                                                                                     .put("connect", new ConnectServiceMetadata()).put("connectparticipant", new ConnectparticipantServiceMetadata())
                                                                                     .put("contact-lens", new ContactLensServiceMetadata()).put("cur", new CurServiceMetadata())
                                                                                     .put("data.iot", new DataIotServiceMetadata()).put("data.jobs.iot", new DataJobsIotServiceMetadata())
                                                                                     .put("data.mediastore", new DataMediastoreServiceMetadata()).put("databrew", new DatabrewServiceMetadata())
                                                                                     .put("dataexchange", new DataexchangeServiceMetadata()).put("datapipeline", new DatapipelineServiceMetadata())
                                                                                     .put("datasync", new DatasyncServiceMetadata()).put("dax", new DaxServiceMetadata())
                                                                                     .put("deeplens", new DeeplensServiceMetadata()).put("devicefarm", new DevicefarmServiceMetadata())
                                                                                     .put("devices.iot1click", new DevicesIot1clickServiceMetadata())
                                                                                     .put("directconnect", new DirectconnectServiceMetadata()).put("discovery", new DiscoveryServiceMetadata())
                                                                                     .put("dlm", new DlmServiceMetadata()).put("dms", new DmsServiceMetadata()).put("docdb", new DocdbServiceMetadata())
                                                                                     .put("ds", new DsServiceMetadata()).put("dynamodb", new DynamodbServiceMetadata())
                                                                                     .put("ebs", new EbsServiceMetadata()).put("ec2", new Ec2ServiceMetadata()).put("ecs", new EcsServiceMetadata())
                                                                                     .put("edge.sagemaker", new EdgeSagemakerServiceMetadata()).put("eks", new EksServiceMetadata())
                                                                                     .put("elasticache", new ElasticacheServiceMetadata()).put("elasticbeanstalk", new ElasticbeanstalkServiceMetadata())
                                                                                     .put("elasticfilesystem", new ElasticfilesystemServiceMetadata())
                                                                                     .put("elasticloadbalancing", new ElasticloadbalancingServiceMetadata())
                                                                                     .put("elasticmapreduce", new ElasticmapreduceServiceMetadata())
                                                                                     .put("elastictranscoder", new ElastictranscoderServiceMetadata()).put("email", new EmailServiceMetadata())
                                                                                     .put("emr-containers", new EmrContainersServiceMetadata())
                                                                                     .put("entitlement.marketplace", new EntitlementMarketplaceServiceMetadata()).put("es", new EsServiceMetadata())
                                                                                     .put("events", new EventsServiceMetadata()).put("execute-api", new ExecuteApiServiceMetadata())
                                                                                     .put("finspace", new FinspaceServiceMetadata()).put("finspace-api", new FinspaceApiServiceMetadata())
                                                                                     .put("firehose", new FirehoseServiceMetadata()).put("fms", new FmsServiceMetadata())
                                                                                     .put("forecast", new ForecastServiceMetadata()).put("forecastquery", new ForecastqueryServiceMetadata())
                                                                                     .put("frauddetector", new FrauddetectorServiceMetadata()).put("fsx", new FsxServiceMetadata())
                                                                                     .put("gamelift", new GameliftServiceMetadata()).put("glacier", new GlacierServiceMetadata())
                                                                                     .put("glue", new GlueServiceMetadata()).put("grafana", new GrafanaServiceMetadata())
                                                                                     .put("greengrass", new GreengrassServiceMetadata()).put("groundstation", new GroundstationServiceMetadata())
                                                                                     .put("guardduty", new GuarddutyServiceMetadata()).put("health", new HealthServiceMetadata())
                                                                                     .put("healthlake", new HealthlakeServiceMetadata()).put("honeycode", new HoneycodeServiceMetadata())
                                                                                     .put("iam", new IamServiceMetadata()).put("identity-chime", new IdentityChimeServiceMetadata())
                                                                                     .put("identitystore", new IdentitystoreServiceMetadata()).put("importexport", new ImportexportServiceMetadata())
                                                                                     .put("inspector", new InspectorServiceMetadata()).put("iot", new IotServiceMetadata())
                                                                                     .put("iotanalytics", new IotanalyticsServiceMetadata())
                                                                                     .put("iotdeviceadvisor", new IotdeviceadvisorServiceMetadata()).put("iotevents", new IoteventsServiceMetadata())
                                                                                     .put("ioteventsdata", new IoteventsdataServiceMetadata())
                                                                                     .put("iotsecuredtunneling", new IotsecuredtunnelingServiceMetadata())
                                                                                     .put("iotsitewise", new IotsitewiseServiceMetadata()).put("iotthingsgraph", new IotthingsgraphServiceMetadata())
                                                                                     .put("iotwireless", new IotwirelessServiceMetadata()).put("ivs", new IvsServiceMetadata())
                                                                                     .put("kafka", new KafkaServiceMetadata()).put("kafkaconnect", new KafkaconnectServiceMetadata())
                                                                                     .put("kendra", new KendraServiceMetadata()).put("kinesis", new KinesisServiceMetadata())
                                                                                     .put("kinesisanalytics", new KinesisanalyticsServiceMetadata())
                                                                                     .put("kinesisvideo", new KinesisvideoServiceMetadata()).put("kms", new KmsServiceMetadata())
                                                                                     .put("lakeformation", new LakeformationServiceMetadata()).put("lambda", new LambdaServiceMetadata())
                                                                                     .put("license-manager", new LicenseManagerServiceMetadata()).put("lightsail", new LightsailServiceMetadata())
                                                                                     .put("logs", new LogsServiceMetadata()).put("lookoutequipment", new LookoutequipmentServiceMetadata())
                                                                                     .put("lookoutmetrics", new LookoutmetricsServiceMetadata()).put("lookoutvision", new LookoutvisionServiceMetadata())
                                                                                     .put("machinelearning", new MachinelearningServiceMetadata()).put("macie", new MacieServiceMetadata())
                                                                                     .put("macie2", new Macie2ServiceMetadata()).put("managedblockchain", new ManagedblockchainServiceMetadata())
                                                                                     .put("marketplacecommerceanalytics", new MarketplacecommerceanalyticsServiceMetadata())
                                                                                     .put("mediaconnect", new MediaconnectServiceMetadata()).put("mediaconvert", new MediaconvertServiceMetadata())
                                                                                     .put("medialive", new MedialiveServiceMetadata()).put("mediapackage", new MediapackageServiceMetadata())
                                                                                     .put("mediapackage-vod", new MediapackageVodServiceMetadata()).put("mediastore", new MediastoreServiceMetadata())
                                                                                     .put("memorydb", new MemorydbServiceMetadata()).put("messaging-chime", new MessagingChimeServiceMetadata())
                                                                                     .put("metering.marketplace", new MeteringMarketplaceServiceMetadata()).put("mgh", new MghServiceMetadata())
                                                                                     .put("mgn", new MgnServiceMetadata()).put("migrationhub-strategy", new MigrationhubStrategyServiceMetadata())
                                                                                     .put("mobileanalytics", new MobileanalyticsServiceMetadata()).put("models-v2-lex", new ModelsV2LexServiceMetadata())
                                                                                     .put("models.lex", new ModelsLexServiceMetadata()).put("monitoring", new MonitoringServiceMetadata())
                                                                                     .put("mq", new MqServiceMetadata()).put("mturk-requester", new MturkRequesterServiceMetadata())
                                                                                     .put("neptune", new NeptuneServiceMetadata()).put("network-firewall", new NetworkFirewallServiceMetadata())
                                                                                     .put("networkmanager", new NetworkmanagerServiceMetadata()).put("nimble", new NimbleServiceMetadata())
                                                                                     .put("oidc", new OidcServiceMetadata()).put("opsworks", new OpsworksServiceMetadata())
                                                                                     .put("opsworks-cm", new OpsworksCmServiceMetadata()).put("organizations", new OrganizationsServiceMetadata())
                                                                                     .put("outposts", new OutpostsServiceMetadata()).put("personalize", new PersonalizeServiceMetadata())
                                                                                     .put("pi", new PiServiceMetadata()).put("pinpoint", new PinpointServiceMetadata())
                                                                                     .put("pinpoint-sms-voice", new PinpointSmsVoiceServiceMetadata()).put("polly", new PollyServiceMetadata())
                                                                                     .put("portal.sso", new PortalSsoServiceMetadata()).put("profile", new ProfileServiceMetadata())
                                                                                     .put("projects.iot1click", new ProjectsIot1clickServiceMetadata()).put("qldb", new QldbServiceMetadata())
                                                                                     .put("quicksight", new QuicksightServiceMetadata()).put("ram", new RamServiceMetadata())
                                                                                     .put("rds", new RdsServiceMetadata()).put("rdsdataservice", new RdsdataserviceServiceMetadata())
                                                                                     .put("redshift", new RedshiftServiceMetadata()).put("rekognition", new RekognitionServiceMetadata())
                                                                                     .put("resource-groups", new ResourceGroupsServiceMetadata()).put("robomaker", new RobomakerServiceMetadata())
                                                                                     .put("route53", new Route53ServiceMetadata())
                                                                                     .put("route53-recovery-control-config", new Route53RecoveryControlConfigServiceMetadata())
                                                                                     .put("route53domains", new Route53domainsServiceMetadata())
                                                                                     .put("route53resolver", new Route53resolverServiceMetadata())
                                                                                     .put("runtime-v2-lex", new RuntimeV2LexServiceMetadata()).put("runtime.lex", new RuntimeLexServiceMetadata())
                                                                                     .put("runtime.sagemaker", new RuntimeSagemakerServiceMetadata()).put("s3", new EnhancedS3ServiceMetadata())
                                                                                     .put("s3-control", new S3ControlServiceMetadata()).put("s3-outposts", new S3OutpostsServiceMetadata())
                                                                                     .put("savingsplans", new SavingsplansServiceMetadata()).put("schemas", new SchemasServiceMetadata())
                                                                                     .put("sdb", new SdbServiceMetadata()).put("secretsmanager", new SecretsmanagerServiceMetadata())
                                                                                     .put("securityhub", new SecurityhubServiceMetadata()).put("serverlessrepo", new ServerlessrepoServiceMetadata())
                                                                                     .put("servicecatalog", new ServicecatalogServiceMetadata())
                                                                                     .put("servicecatalog-appregistry", new ServicecatalogAppregistryServiceMetadata())
                                                                                     .put("servicediscovery", new ServicediscoveryServiceMetadata())
                                                                                     .put("servicequotas", new ServicequotasServiceMetadata()).put("session.qldb", new SessionQldbServiceMetadata())
                                                                                     .put("shield", new ShieldServiceMetadata()).put("signer", new SignerServiceMetadata())
                                                                                     .put("sms", new SmsServiceMetadata()).put("sms-voice.pinpoint", new SmsVoicePinpointServiceMetadata())
                                                                                     .put("snow-device-management", new SnowDeviceManagementServiceMetadata())
                                                                                     .put("snowball", new SnowballServiceMetadata()).put("sns", new SnsServiceMetadata())
                                                                                     .put("sqs", new SqsServiceMetadata()).put("ssm", new SsmServiceMetadata())
                                                                                     .put("ssm-incidents", new SsmIncidentsServiceMetadata()).put("states", new StatesServiceMetadata())
                                                                                     .put("storagegateway", new StoragegatewayServiceMetadata())
                                                                                     .put("streams.dynamodb", new StreamsDynamodbServiceMetadata()).put("sts", new StsServiceMetadata())
                                                                                     .put("support", new SupportServiceMetadata()).put("swf", new SwfServiceMetadata())
                                                                                     .put("synthetics", new SyntheticsServiceMetadata()).put("tagging", new TaggingServiceMetadata())
                                                                                     .put("textract", new TextractServiceMetadata()).put("timestream.query", new TimestreamQueryServiceMetadata())
                                                                                     .put("timestream.write", new TimestreamWriteServiceMetadata()).put("transcribe", new TranscribeServiceMetadata())
                                                                                     .put("transcribestreaming", new TranscribestreamingServiceMetadata()).put("transfer", new TransferServiceMetadata())
                                                                                     .put("translate", new TranslateServiceMetadata()).put("valkyrie", new ValkyrieServiceMetadata())
                                                                                     .put("voiceid", new VoiceidServiceMetadata()).put("waf", new WafServiceMetadata())
                                                                                     .put("waf-regional", new WafRegionalServiceMetadata()).put("wisdom", new WisdomServiceMetadata())
                                                                                     .put("workdocs", new WorkdocsServiceMetadata()).put("workmail", new WorkmailServiceMetadata())
                                                                                     .put("workspaces", new WorkspacesServiceMetadata()).put("xray", new XrayServiceMetadata())
                                                                                     .put("operator", new OperatorServiceMetadata()).build();

    public ServiceMetadata serviceMetadata(String endpointPrefix) {
        return SERVICE_METADATA.get(endpointPrefix);
    }
}
