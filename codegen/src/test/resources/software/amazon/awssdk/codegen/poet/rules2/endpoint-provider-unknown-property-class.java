package software.amazon.awssdk.services.query.endpoints.internal;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryEndpointProvider implements QueryEndpointProvider {
  @Override
  public CompletableFuture<Endpoint> resolveEndpoint(QueryEndpointParams params) {
    try {
      Endpoint result = endpointRule0(params);
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

  private static Endpoint endpointRule0(QueryEndpointParams params) {
    Endpoint result = endpointRule1(params);
    if (result != null) {
      return result;
    }
    throw SdkClientException.create("Invalid Configuration: Missing Endpoint");
  }

  private static Endpoint endpointRule1(QueryEndpointParams params) {
    if (params.endpoint() != null) {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(params.endpoint())).build();
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
