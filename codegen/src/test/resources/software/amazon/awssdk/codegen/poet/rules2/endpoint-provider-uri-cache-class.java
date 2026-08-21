package software.amazon.awssdk.services.query.endpoints.internal;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryEndpointProvider implements QueryEndpointProvider {
  @Override
  public CompletableFuture<Endpoint> resolveEndpoint(QueryEndpointParams params) {
    Validate.notNull(params.region(), "Parameter 'region' must not be null");
    try {
      Region region = params.region();
      String regionId = region == null ? null : region.id();
      Endpoint result = endpointRule0(params, regionId);
      if (result == null) {
        throw SdkClientException.create("Rule engine did not reach an error or endpoint result");
      }
      return CompletableFuture.completedFuture(result);
    } catch (SdkClientException e) {
      String errorMsg = e.getMessage();
      if (errorMsg != null && errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
        return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg + ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest."));
      }
      return CompletableFutureUtils.failedFuture(e);
    } catch (Exception error) {
      return CompletableFutureUtils.failedFuture(error);
    }
  }

  private static Endpoint endpointRule0(QueryEndpointParams params, String region) {
    return endpointRule1(params, region);
  }

  private static Endpoint endpointRule1(QueryEndpointParams params, String region) {
    RulePartition partitionResult = RulesFunctions.awsPartition(region);
    if (partitionResult != null) {
      Endpoint result = endpointRule2(params, partitionResult);
      if (result != null) {
        return result;
      }
      result = endpointRule6(params, region, partitionResult);
      if (result != null) {
        return result;
      }
      throw SdkClientException.create(region + " is not a valid HTTP host-label");
      if (params.useFipsEndpoint() == null && params.useDualStackEndpoint() != null && params.useDualStackEndpoint() && params.arnList() != null) {
        String firstArn = RulesFunctions.listAccess(params.arnList(), 0);
        if (firstArn != null) {
          RuleArn parsedArn = RulesFunctions.awsParseArn(firstArn);
          if (parsedArn != null) {
            return Endpoint.ofAttribute(EndpointUrl.fromComponents("https", params.endpointId() + ".query." + partitionResult.dualStackDnsSuffix(), -1, ""), AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*")).build()));
          }
        }
      }
    }
    return null;
  }

  private static Endpoint endpointRule2(QueryEndpointParams params, RulePartition partitionResult) {
    if (params.endpointId() != null) {
      if (params.useFipsEndpoint() != null && params.useFipsEndpoint()) {
        throw SdkClientException.create("FIPS endpoints not supported with multi-region endpoints");
      }
      if (params.useFipsEndpoint() == null && params.useDualStackEndpoint() != null && params.useDualStackEndpoint()) {
        return Endpoint.ofAttribute(EndpointUrl.fromComponents("https", params.endpointId() + ".query." + partitionResult.dualStackDnsSuffix(), -1, ""), AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*")).build()));
      }
      return Endpoint.ofAttribute(EndpointUrl.fromComponents("https", params.endpointId() + ".query." + partitionResult.dnsSuffix(), -1, ""), AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*")).build()));
    }
    return null;
  }

  private static Endpoint endpointRule6(QueryEndpointParams params, String region,
      RulePartition partitionResult) {
    if (RulesFunctions.isValidHostLabelSingle(region)) {
      if (params.useFipsEndpoint() != null && params.useFipsEndpoint() && params.useDualStackEndpoint() == null) {
        return Endpoint.ofAttribute(EndpointUrl.fromComponents("https", "query-fips." + region + "." + partitionResult.dnsSuffix(), -1, ""), AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*")).build()));
      }
      if (params.useDualStackEndpoint() != null && params.useDualStackEndpoint() && params.useFipsEndpoint() == null) {
        return Endpoint.ofAttribute(EndpointUrl.fromComponents("https", "query." + region + "." + partitionResult.dualStackDnsSuffix(), -1, ""), AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder().signingName("query").signingRegion(region).build()));
      }
      if (params.useDualStackEndpoint() != null && params.useFipsEndpoint() != null && params.useDualStackEndpoint() && params.useFipsEndpoint()) {
        return Endpoint.ofAttribute(EndpointUrl.fromComponents("https", "query-fips." + region + "." + partitionResult.dualStackDnsSuffix(), -1, ""), AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*")).build()));
      }
      return Endpoint.of(EndpointUrl.fromComponents("https", "query." + region + "." + partitionResult.dnsSuffix(), -1, ""));
    }
    return null;
  }

  @Override
  public boolean equals(Object rhs) {
    return rhs != null && getClass().equals(rhs.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
