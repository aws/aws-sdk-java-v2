package software.amazon.awssdk.services.database.endpoints.internal;

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.awscore.internal.endpoints.AwsEndpointProviderUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.services.database.endpoints.DatabaseEndpointParams;
import software.amazon.awssdk.utils.CollectionUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DatabaseEndpointResolverUtils {
  private DatabaseEndpointResolverUtils() {
  }

  public static DatabaseEndpointParams ruleParams(SdkRequest request,
      ExecutionAttributes executionAttributes) {
    DatabaseEndpointParams.Builder builder = DatabaseEndpointParams.builder();
    builder.region(AwsEndpointProviderUtils.regionBuiltIn(executionAttributes));
    builder.endpoint(AwsEndpointProviderUtils.endpointBuiltIn(executionAttributes));
    setContextParams(builder, executionAttributes.getAttribute(AwsExecutionAttribute.OPERATION_NAME), request);
    setStaticContextParams(builder, executionAttributes.getAttribute(AwsExecutionAttribute.OPERATION_NAME));
    setOperationContextParams(builder, executionAttributes.getAttribute(AwsExecutionAttribute.OPERATION_NAME), request);
    return builder.build();
  }

  private static void setContextParams(DatabaseEndpointParams.Builder params, String operationName,
      SdkRequest request) {
  }

  private static void setStaticContextParams(DatabaseEndpointParams.Builder params,
      String operationName) {
  }

  public static <T extends Identity> SelectedAuthScheme<T> authSchemeWithEndpointSignerProperties(
      List<EndpointAuthScheme> endpointAuthSchemes, SelectedAuthScheme<T> selectedAuthScheme) {
    for (EndpointAuthScheme endpointAuthScheme : endpointAuthSchemes) {
      if (!endpointAuthScheme.schemeId().equals(selectedAuthScheme.authSchemeOption().schemeId())) {
        continue;
      }
      AuthSchemeOption.Builder option = selectedAuthScheme.authSchemeOption().toBuilder();
      if (endpointAuthScheme instanceof SigV4AuthScheme) {
        SigV4AuthScheme v4AuthScheme = (SigV4AuthScheme) endpointAuthScheme;
        if (v4AuthScheme.isDisableDoubleEncodingSet()) {
          option.putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, !v4AuthScheme.disableDoubleEncoding());
        }
        if (v4AuthScheme.signingRegion() != null) {
          option.putSignerProperty(AwsV4HttpSigner.REGION_NAME, v4AuthScheme.signingRegion());
        }
        if (v4AuthScheme.signingName() != null) {
          option.putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, v4AuthScheme.signingName());
        }
        return new SelectedAuthScheme<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(), option.build());
      }
      if (endpointAuthScheme instanceof SigV4aAuthScheme) {
        SigV4aAuthScheme v4aAuthScheme = (SigV4aAuthScheme) endpointAuthScheme;
        if (v4aAuthScheme.isDisableDoubleEncodingSet()) {
          option.putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, !v4aAuthScheme.disableDoubleEncoding());
        }
        if (!(selectedAuthScheme.authSchemeOption().schemeId().equals(AwsV4aAuthScheme.SCHEME_ID) && selectedAuthScheme.authSchemeOption().signerProperty(AwsV4aHttpSigner.REGION_SET) != null) && !CollectionUtils.isNullOrEmpty(v4aAuthScheme.signingRegionSet())) {
          RegionSet regionSet = RegionSet.create(v4aAuthScheme.signingRegionSet());
          option.putSignerProperty(AwsV4aHttpSigner.REGION_SET, regionSet);
        }
        if (v4aAuthScheme.signingName() != null) {
          option.putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, v4aAuthScheme.signingName());
        }
        return new SelectedAuthScheme<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(), option.build());
      }
      throw new IllegalArgumentException("Endpoint auth scheme '" + endpointAuthScheme.name() + "' cannot be mapped to the SDK auth scheme. Was it declared in the service's model?");
    }
    return selectedAuthScheme;
  }

  private static void setOperationContextParams(DatabaseEndpointParams.Builder params,
      String operationName, SdkRequest request) {
  }

  public static Optional<String> hostPrefix(String operationName, SdkRequest request) {
    return Optional.empty();
  }

  public static void setMetricValues(Endpoint endpoint, ExecutionAttributes executionAttributes) {
    if (endpoint.attribute(AwsEndpointAttribute.METRIC_VALUES) != null) {
      executionAttributes.getOptionalAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS).ifPresent(metrics -> endpoint.attribute(AwsEndpointAttribute.METRIC_VALUES).forEach(v -> metrics.addMetric(v)));
    }
  }
}
