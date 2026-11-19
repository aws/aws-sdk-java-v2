package software.amazon.awssdk.regions;

import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.servicemetadata.AccessAnalyzerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiEcrPublicServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ApiEcrServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.DynamodbServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.ElasticloadbalancingServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.EnhancedS3ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.IamServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.LambdaServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.Route53ServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.RuntimeSagemakerServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.S3OutpostsServiceMetadata;
import software.amazon.awssdk.regions.servicemetadata.StsServiceMetadata;
import software.amazon.awssdk.utils.ImmutableMap;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class GeneratedServiceMetadataProvider implements ServiceMetadataProvider {
    private static final Map<String, ServiceMetadata> SERVICE_METADATA = ImmutableMap.<String, ServiceMetadata>builder()
            .put("access-analyzer", new AccessAnalyzerServiceMetadata()).put("api.ecr", new ApiEcrServiceMetadata())
            .put("api.ecr-public", new ApiEcrPublicServiceMetadata()).put("dynamodb", new DynamodbServiceMetadata())
            .put("elasticloadbalancing", new ElasticloadbalancingServiceMetadata()).put("iam", new IamServiceMetadata())
            .put("lambda", new LambdaServiceMetadata()).put("route53", new Route53ServiceMetadata())
            .put("runtime.sagemaker", new RuntimeSagemakerServiceMetadata()).put("s3", new EnhancedS3ServiceMetadata())
            .put("s3-outposts", new S3OutpostsServiceMetadata()).put("sts", new StsServiceMetadata()).build();

    public ServiceMetadata serviceMetadata(String endpointPrefix) {
        return SERVICE_METADATA.get(endpointPrefix);
    }
}
