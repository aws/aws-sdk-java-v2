package software.amazon.awssdk.regions;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.servicemetadata.A4bServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AcmPcaServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AcmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiMediatailorServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiPricingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApigatewayServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApplicationAutoscalingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Appstream2ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AthenaServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AutoscalingPlansServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.AutoscalingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.BatchServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.BudgetsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Cloud9ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ClouddirectoryServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudformationServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudfrontServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudhsmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Cloudhsmv2ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudsearchServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CloudtrailServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodebuildServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodecommitServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodedeployServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodepipelineServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CodestarServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CognitoIdentityServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CognitoIdpServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CognitoSyncServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ComprehendServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ConfigServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.CurServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DataIotServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DatapipelineServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DaxServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DevicefarmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DirectconnectServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DiscoveryServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DlmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DmsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DynamodbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Ec2ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EcrServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EcsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticacheServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticbeanstalkServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticfilesystemServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticloadbalancingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticmapreduceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElastictranscoderServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EmailServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EnhancedS3ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EntitlementMarketplaceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EventsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.FirehoseServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.FmsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GameliftServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GlacierServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GlueServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GreengrassServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.GuarddutyServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.HealthServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IamServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ImportexportServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.InspectorServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IotServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IotanalyticsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KinesisServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KinesisanalyticsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KinesisvideoServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.KmsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LambdaServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LightsailServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LogsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MachinelearningServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MacieServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MarketplacecommerceanalyticsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MediaconvertServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MedialiveServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MediapackageServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MediastoreServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MeteringMarketplaceServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MghServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MobileanalyticsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ModelsLexServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MonitoringServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MqServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.MturkRequesterServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.NeptuneServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.OpsworksCmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.OpsworksServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.OrganizationsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.PinpointServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.PollyServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RdsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RedshiftServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RekognitionServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ResourceGroupsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Route53ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Route53domainsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RuntimeLexServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RuntimeSagemakerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SagemakerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SdbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SecretsmanagerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ServerlessrepoServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ServicecatalogServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ServicediscoveryServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ShieldServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SmsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SnowballServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SnsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SqsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SsmServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.StatesServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.StoragegatewayServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.StreamsDynamodbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.StsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SupportServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.SwfServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TaggingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TranscribeServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.TranslateServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WafRegionalServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WafServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WorkdocsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WorkmailServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.WorkspacesServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.XrayServiceMetadata;
import software.amazon.awssdk.utils.ImmutableMap;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class GeneratedServiceMetadataProvider implements ServiceMetadataProvider {
    private static final Map<String, ServiceMetadata> SERVICE_METADATA = ImmutableMap.<String, ServiceMetadata> builder()
            .put("a4b", new A4bServiceMetadata()).put("acm", new AcmServiceMetadata())
            .put("acm-pca", new AcmPcaServiceMetadata()).put("api.mediatailor", new ApiMediatailorServiceMetadata())
            .put("api.pricing", new ApiPricingServiceMetadata()).put("apigateway", new ApigatewayServiceMetadata())
            .put("application-autoscaling", new ApplicationAutoscalingServiceMetadata())
            .put("appstream2", new Appstream2ServiceMetadata()).put("athena", new AthenaServiceMetadata())
            .put("autoscaling", new AutoscalingServiceMetadata()).put("autoscaling-plans", new AutoscalingPlansServiceMetadata())
            .put("batch", new BatchServiceMetadata()).put("budgets", new BudgetsServiceMetadata())
            .put("ce", new CeServiceMetadata()).put("cloud9", new Cloud9ServiceMetadata())
            .put("clouddirectory", new ClouddirectoryServiceMetadata())
            .put("cloudformation", new CloudformationServiceMetadata()).put("cloudfront", new CloudfrontServiceMetadata())
            .put("cloudhsm", new CloudhsmServiceMetadata()).put("cloudhsmv2", new Cloudhsmv2ServiceMetadata())
            .put("cloudsearch", new CloudsearchServiceMetadata()).put("cloudtrail", new CloudtrailServiceMetadata())
            .put("codebuild", new CodebuildServiceMetadata()).put("codecommit", new CodecommitServiceMetadata())
            .put("codedeploy", new CodedeployServiceMetadata()).put("codepipeline", new CodepipelineServiceMetadata())
            .put("codestar", new CodestarServiceMetadata()).put("cognito-identity", new CognitoIdentityServiceMetadata())
            .put("cognito-idp", new CognitoIdpServiceMetadata()).put("cognito-sync", new CognitoSyncServiceMetadata())
            .put("comprehend", new ComprehendServiceMetadata()).put("config", new ConfigServiceMetadata())
            .put("cur", new CurServiceMetadata()).put("data.iot", new DataIotServiceMetadata())
            .put("datapipeline", new DatapipelineServiceMetadata()).put("dax", new DaxServiceMetadata())
            .put("devicefarm", new DevicefarmServiceMetadata()).put("directconnect", new DirectconnectServiceMetadata())
            .put("discovery", new DiscoveryServiceMetadata()).put("dlm", new DlmServiceMetadata())
            .put("dms", new DmsServiceMetadata()).put("ds", new DsServiceMetadata())
            .put("dynamodb", new DynamodbServiceMetadata()).put("ec2", new Ec2ServiceMetadata())
            .put("ecr", new EcrServiceMetadata()).put("ecs", new EcsServiceMetadata())
            .put("elasticache", new ElasticacheServiceMetadata()).put("elasticbeanstalk", new ElasticbeanstalkServiceMetadata())
            .put("elasticfilesystem", new ElasticfilesystemServiceMetadata())
            .put("elasticloadbalancing", new ElasticloadbalancingServiceMetadata())
            .put("elasticmapreduce", new ElasticmapreduceServiceMetadata())
            .put("elastictranscoder", new ElastictranscoderServiceMetadata()).put("email", new EmailServiceMetadata())
            .put("entitlement.marketplace", new EntitlementMarketplaceServiceMetadata()).put("es", new EsServiceMetadata())
            .put("events", new EventsServiceMetadata()).put("firehose", new FirehoseServiceMetadata())
            .put("fms", new FmsServiceMetadata()).put("gamelift", new GameliftServiceMetadata())
            .put("glacier", new GlacierServiceMetadata()).put("glue", new GlueServiceMetadata())
            .put("greengrass", new GreengrassServiceMetadata()).put("guardduty", new GuarddutyServiceMetadata())
            .put("health", new HealthServiceMetadata()).put("iam", new IamServiceMetadata())
            .put("importexport", new ImportexportServiceMetadata()).put("inspector", new InspectorServiceMetadata())
            .put("iot", new IotServiceMetadata()).put("iotanalytics", new IotanalyticsServiceMetadata())
            .put("kinesis", new KinesisServiceMetadata()).put("kinesisanalytics", new KinesisanalyticsServiceMetadata())
            .put("kinesisvideo", new KinesisvideoServiceMetadata()).put("kms", new KmsServiceMetadata())
            .put("lambda", new LambdaServiceMetadata()).put("lightsail", new LightsailServiceMetadata())
            .put("logs", new LogsServiceMetadata()).put("machinelearning", new MachinelearningServiceMetadata())
            .put("macie", new MacieServiceMetadata())
            .put("marketplacecommerceanalytics", new MarketplacecommerceanalyticsServiceMetadata())
            .put("mediaconvert", new MediaconvertServiceMetadata()).put("medialive", new MedialiveServiceMetadata())
            .put("mediapackage", new MediapackageServiceMetadata()).put("mediastore", new MediastoreServiceMetadata())
            .put("metering.marketplace", new MeteringMarketplaceServiceMetadata()).put("mgh", new MghServiceMetadata())
            .put("mobileanalytics", new MobileanalyticsServiceMetadata()).put("models.lex", new ModelsLexServiceMetadata())
            .put("monitoring", new MonitoringServiceMetadata()).put("mq", new MqServiceMetadata())
            .put("mturk-requester", new MturkRequesterServiceMetadata()).put("neptune", new NeptuneServiceMetadata())
            .put("opsworks", new OpsworksServiceMetadata()).put("opsworks-cm", new OpsworksCmServiceMetadata())
            .put("organizations", new OrganizationsServiceMetadata()).put("pinpoint", new PinpointServiceMetadata())
            .put("polly", new PollyServiceMetadata()).put("rds", new RdsServiceMetadata())
            .put("redshift", new RedshiftServiceMetadata()).put("rekognition", new RekognitionServiceMetadata())
            .put("resource-groups", new ResourceGroupsServiceMetadata()).put("route53", new Route53ServiceMetadata())
            .put("route53domains", new Route53domainsServiceMetadata()).put("runtime.lex", new RuntimeLexServiceMetadata())
            .put("runtime.sagemaker", new RuntimeSagemakerServiceMetadata()).put("s3", new EnhancedS3ServiceMetadata())
            .put("sagemaker", new SagemakerServiceMetadata()).put("sdb", new SdbServiceMetadata())
            .put("secretsmanager", new SecretsmanagerServiceMetadata())
            .put("serverlessrepo", new ServerlessrepoServiceMetadata())
            .put("servicecatalog", new ServicecatalogServiceMetadata())
            .put("servicediscovery", new ServicediscoveryServiceMetadata()).put("shield", new ShieldServiceMetadata())
            .put("sms", new SmsServiceMetadata()).put("snowball", new SnowballServiceMetadata())
            .put("sns", new SnsServiceMetadata()).put("sqs", new SqsServiceMetadata()).put("ssm", new SsmServiceMetadata())
            .put("states", new StatesServiceMetadata()).put("storagegateway", new StoragegatewayServiceMetadata())
            .put("streams.dynamodb", new StreamsDynamodbServiceMetadata()).put("sts", new StsServiceMetadata())
            .put("support", new SupportServiceMetadata()).put("swf", new SwfServiceMetadata())
            .put("tagging", new TaggingServiceMetadata()).put("transcribe", new TranscribeServiceMetadata())
            .put("translate", new TranslateServiceMetadata()).put("waf", new WafServiceMetadata())
            .put("waf-regional", new WafRegionalServiceMetadata()).put("workdocs", new WorkdocsServiceMetadata())
            .put("workmail", new WorkmailServiceMetadata()).put("workspaces", new WorkspacesServiceMetadata())
            .put("xray", new XrayServiceMetadata()).build();

    public ServiceMetadata serviceMetadata(String endpointPrefix) {
        return SERVICE_METADATA.get(endpointPrefix);
    }
}
