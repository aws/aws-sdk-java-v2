package software.amazon.awssdk.services.query.endpoints.internal;

import java.util.List;
import java.util.Objects;
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
  private volatile CacheEntry cache;

  @Override
  public CompletableFuture<Endpoint> resolveEndpoint(QueryEndpointParams endpointParams) {
    // Single-entry result cache: reuse the last endpoint when the params still match.
    CacheEntry cached = this.cache;
    if (cached != null && cacheParamsMatch(endpointParams, cached.params)) {
      return CompletableFuture.completedFuture(cached.endpoint);
    }
    try {
      Evaluator evaluator = new Evaluator();
      evaluator.params = endpointParams;
      evaluator.region = endpointParams.region() == null ? null : endpointParams.region().id();
      Endpoint result = evaluator.nodeP14();
      if (result == null) {
        return CompletableFutureUtils.failedFuture(SdkClientException.create("Rule engine did not reach an error or endpoint result"));
      }
      this.cache = new CacheEntry(endpointParams, result);
      return CompletableFuture.completedFuture(result);
    } catch (SdkClientException e) {
      String errorMsg = e.getMessage();
      if (errorMsg != null && errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
        return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg + ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.", e));
      }
      return CompletableFutureUtils.failedFuture(e);
    } catch (Exception error) {
      return CompletableFutureUtils.failedFuture(error);
    }
  }

  private static boolean cacheParamsMatch(QueryEndpointParams a, QueryEndpointParams b) {
    return Objects.equals(a.useDualStack(), b.useDualStack())
            && Objects.equals(a.useFips(), b.useFips())
            && Objects.equals(a.region(), b.region())
            && Objects.equals(a.stringContextParam(), b.stringContextParam())
            && Objects.equals(a.endpoint(), b.endpoint())
            && Objects.equals(a.staticStringParam(), b.staticStringParam())
            && Objects.equals(a.operationContextParam(), b.operationContextParam())
            && cacheListsMatch(a.arnList(), b.arnList());
  }

  private static boolean cacheListsMatch(List<String> a, List<String> b) {
    if (a == b) return true;
    if (a == null || b == null) return false;
    int size = a.size();
    if (size != b.size()) return false;
    // Bounded so that a long list cannot make the cache check cost more than resolving.
    if (size > 8) return false;
    for (int i = 0; i < size; i++) {
      if (!Objects.equals(a.get(i), b.get(i))) return false;
    }
    return true;
  }

  private static final class Evaluator {
    QueryEndpointParams params;

    String region;

    RulePartition partitionResult;

    private Endpoint nodeP0() {
      return null;
    }

    private Endpoint nodeP1() {
      return Boolean.TRUE.equals(params.useDualStack())
              ? result1()
              : result2();
    }

    private Endpoint nodeP2() {
      return Boolean.TRUE.equals(params.useFips())
              ? result0()
              : nodeP1();
    }

    private Endpoint nodeP3() {
      return cond6()
              ? result3()
              : result4();
    }

    private Endpoint nodeP4() {
      return cond5()
              ? nodeP3()
              : result4();
    }

    private Endpoint nodeP5() {
      return cond7()
              ? result5()
              : result6();
    }

    private Endpoint nodeP6() {
      return cond5()
              ? nodeP5()
              : result7();
    }

    private Endpoint nodeP7() {
      return Boolean.TRUE.equals(params.useDualStack())
              ? nodeP4()
              : nodeP6();
    }

    private Endpoint nodeP8() {
      return cond3()
              ? nodeP7()
              : result11();
    }

    private Endpoint nodeP9() {
      return cond6()
              ? result8()
              : result9();
    }

    private Endpoint nodeP10() {
      return Boolean.TRUE.equals(params.useDualStack())
              ? nodeP9()
              : result10();
    }

    private Endpoint nodeP11() {
      return cond3()
              ? nodeP10()
              : result11();
    }

    private Endpoint nodeP12() {
      return Boolean.TRUE.equals(params.useFips())
              ? nodeP8()
              : nodeP11();
    }

    private Endpoint nodeP13() {
      return region != null
              ? nodeP12()
              : result11();
    }

    private Endpoint nodeP14() {
      return params.endpoint() != null
              ? nodeP2()
              : nodeP13();
    }

    private boolean cond3() {
      partitionResult = RulesFunctions.awsPartition(region);
      return partitionResult != null;
    }

    private boolean cond5() {
      return (partitionResult.supportsFIPS());
    }

    private boolean cond6() {
      return (partitionResult.supportsDualStack());
    }

    private boolean cond7() {
      return ("aws-us-gov".equals(partitionResult.name()));
    }

    private Endpoint result0() {
      throw SdkClientException.create("Invalid Configuration: FIPS and custom endpoint are not supported");
    }

    private Endpoint result1() {
      throw SdkClientException.create("Invalid Configuration: Dualstack and custom endpoint are not supported");
    }

    private Endpoint result2() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromString(params.endpoint())).build();
    }

    private Endpoint result3() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "query-fips." + region + "." + partitionResult.dualStackDnsSuffix(), -1, "")).build();
    }

    private Endpoint result4() {
      throw SdkClientException.create("FIPS and DualStack are enabled, but this partition does not support one or both");
    }

    private Endpoint result5() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "query." + region + ".amazonaws.com", -1, "")).build();
    }

    private Endpoint result6() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "query-fips." + region + "." + partitionResult.dnsSuffix(), -1, "")).build();
    }

    private Endpoint result7() {
      throw SdkClientException.create("FIPS is enabled but this partition does not support FIPS");
    }

    private Endpoint result8() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "query." + region + "." + partitionResult.dualStackDnsSuffix(), -1, "")).build();
    }

    private Endpoint result9() {
      throw SdkClientException.create("DualStack is enabled but this partition does not support DualStack");
    }

    private Endpoint result10() {
      return Endpoint.builder().endpointUrl(EndpointUrl.fromComponents("https", "query." + region + "." + partitionResult.dnsSuffix(), -1, "")).build();
    }

    private Endpoint result11() {
      throw SdkClientException.create("Invalid Configuration: Missing Region");
    }
  }

  private static final class CacheEntry {
    final QueryEndpointParams params;

    final Endpoint endpoint;

    CacheEntry(QueryEndpointParams params, Endpoint endpoint) {
      this.params = params;
      this.endpoint = endpoint;
    }
  }
}
