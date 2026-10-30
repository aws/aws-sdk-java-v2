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

package software.amazon.awssdk.s3.wiretap.testarchive;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opentest4j.TestAbortedException;
import software.amazon.awssdk.s3.wiretap.client.ClientBuildFailed;
import software.amazon.awssdk.s3.wiretap.client.ClientConfig;
import software.amazon.awssdk.s3.wiretap.client.ClientKind;
import software.amazon.awssdk.s3.wiretap.client.ClientSuccessOrFailure;
import software.amazon.awssdk.s3.wiretap.client.ConfiguredClient;
import software.amazon.awssdk.s3.wiretap.client.HttpClient;
import software.amazon.awssdk.s3.wiretap.client.Payload;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;
import software.amazon.awssdk.s3.wiretap.client.feature.CategoryGroup;
import software.amazon.awssdk.s3.wiretap.client.feature.advanced.DisableHostPrefixInjectionFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.AccelerateFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.ChecksumCalculationFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.CrossRegionAccessFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.DefaultsModeFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.DualstackFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.FipsFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.MultiRegionAccessPointsFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.PathStyleFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.ResponseChecksumValidationFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.S3ExpressSessionAuthFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.UseArnRegionFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.crt.MinimumPartSizeFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.crt.TargetThroughputFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.deprecated.DeprecatedAccelerateFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.deprecated.DeprecatedChecksumValidationFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.deprecated.DeprecatedDualstackFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.deprecated.DeprecatedMultiRegionFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.deprecated.DeprecatedPathStyleFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.deprecated.DeprecatedUseArnRegionFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.override.ApiCallAttemptTimeoutFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.override.ApiCallTimeoutFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.override.AppIdFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.override.CompressionFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.override.RetryModeFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.serviceconfig.ChunkedEncodingFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.serviceconfig.ExpectContinueFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.serviceconfig.ExpectContinueThresholdFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.MultipartFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.RetryFeature;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

public class ClientTestExtension implements TestTemplateInvocationContextProvider {

    private static final List<software.amazon.awssdk.s3.wiretap.client.feature.Feature<?>> KNOWN_FEATURES = List.of(
            new HttpClientFeature(),
            new MultipartFeature(),
            new RetryFeature(),
            new ChecksumCalculationFeature(),
            new TargetThroughputFeature(),
            new MinimumPartSizeFeature(),
            new AccelerateFeature(),
            new PathStyleFeature(),
            new UseArnRegionFeature(),
            new MultiRegionAccessPointsFeature(),
            new CrossRegionAccessFeature(),
            new S3ExpressSessionAuthFeature(),
            new DualstackFeature(),
            new FipsFeature(),
            new DefaultsModeFeature(),
            new ResponseChecksumValidationFeature(),
            new ChunkedEncodingFeature(),
            new ExpectContinueFeature(),
            new ExpectContinueThresholdFeature(),
            new RetryModeFeature(),
            new ApiCallTimeoutFeature(),
            new ApiCallAttemptTimeoutFeature(),
            new CompressionFeature(),
            new AppIdFeature(),
            new DisableHostPrefixInjectionFeature(),
            new DeprecatedAccelerateFeature(),
            new DeprecatedPathStyleFeature(),
            new DeprecatedDualstackFeature(),
            new DeprecatedUseArnRegionFeature(),
            new DeprecatedMultiRegionFeature(),
            new DeprecatedChecksumValidationFeature());

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return context.getTestMethod()
                .map(m -> m.isAnnotationPresent(ClientTest.class))
                .orElse(false);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        Class<?> paramType = clientParamType(method);
        int[] sizes = payloadSizes(method);
        Map<Class<?>, List<Category<?>>> selected = selectedCategories(method);

        List<PairwiseCombiner.Axis> axes = new ArrayList<>();
        for (software.amazon.awssdk.s3.wiretap.client.feature.Feature<?> feature : KNOWN_FEATURES) {
            List<Category<?>> categories = selected.get(feature.getClass());
            if (categories == null) {
                categories = List.of(feature.defaultCategory(paramType));
            }
            if (feature instanceof HttpClientFeature) {
                validateHttpTypes(method, paramType, categories);
            }
            axes.add(new PairwiseCombiner.Axis(feature, categories, feature.defaultCategory(paramType)));
        }

        boolean collapseKindMismatches = paramType != ClientSuccessOrFailure.class;
        List<ClientConfig> filtered = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ClientConfig config : combine(method, axes)) {
            ClientConfig resolved = collapseKindMismatches ? filterToKind(config) : config;
            if (seen.add(resolved.describe())) {
                filtered.add(resolved);
            }
        }
        boolean labelSize = sizes.length > 1;
        return filtered.stream()
                .flatMap(config -> Arrays.stream(sizes).boxed()
                        .map(size -> invocationContext(config, paramType, size, labelSize)));
    }

    private static int[] payloadSizes(Method method) {
        boolean hasPayloadParam = Arrays.asList(method.getParameterTypes()).contains(Payload.class);
        int[] sizes = AnnotationSupport.findAnnotation(method, PayloadSizes.class)
                .map(PayloadSizes::value)
                .orElse(null);
        if (sizes == null) {
            return new int[] { PayloadSizes.DEFAULT };
        }
        if (!hasPayloadParam) {
            throw new IllegalStateException("@ClientTest method " + method.getName()
                    + " has @PayloadSizes but no Payload parameter to consume the size");
        }
        if (sizes.length == 0) {
            throw new IllegalStateException("@ClientTest method " + method.getName()
                    + " has an empty @PayloadSizes; list at least one size");
        }
        for (int size : sizes) {
            if (size < 0) {
                throw new IllegalStateException("@ClientTest method " + method.getName()
                        + " has a negative @PayloadSizes value: " + size);
            }
        }
        return sizes;
    }

    private static ClientConfig filterToKind(ClientConfig config) {
        ClientKind kind = kindOf(config);
        Map<software.amazon.awssdk.s3.wiretap.client.feature.Feature<?>, Category<?>> kept = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<software.amazon.awssdk.s3.wiretap.client.feature.Feature<?>, Category<?>> entry
                : config.entries().entrySet()) {
            software.amazon.awssdk.s3.wiretap.client.feature.Feature<?> feature = entry.getKey();
            if (feature.supportedKinds().contains(kind)) {
                kept.put(feature, entry.getValue());
            } else {
                kept.put(feature, feature.defaultCategory(clientClassFor(kind)));
                changed = true;
            }
        }
        return changed ? new ClientConfig(kept) : config;
    }

    private static ClientKind kindOf(ClientConfig config) {
        HttpClient.Type type = config.value(HttpClientFeature.class);
        return type.kind();
    }

    private static Class<?> clientClassFor(ClientKind kind) {
        return kind == ClientKind.SYNC ? S3Client.class : S3AsyncClient.class;
    }

    private static List<ClientConfig> combine(Method method, List<PairwiseCombiner.Axis> axes) {
        CombinationMode mode = AnnotationSupport.findAnnotation(method, ClientTest.class)
                .map(ClientTest::mode)
                .orElse(CombinationMode.CARTESIAN);
        if (mode == CombinationMode.CARTESIAN) {
            return PairwiseCombiner.cartesian(axes);
        }
        List<PairwiseCombiner.Axis> crossed = axes.stream()
                .filter(a -> a.feature() instanceof HttpClientFeature).toList();
        List<PairwiseCombiner.Axis> pairwiseAxes = axes.stream()
                .filter(a -> !(a.feature() instanceof HttpClientFeature)).toList();
        return PairwiseCombiner.combine(crossed, pairwiseAxes);
    }

    private static Class<?> clientParamType(Method method) {
        for (Class<?> p : method.getParameterTypes()) {
            if (p == S3Client.class || p == S3AsyncClient.class
                    || p == ConfiguredClient.class || p == ClientBuildFailed.class
                    || p == ClientSuccessOrFailure.class) {
                return p;
            }
        }
        throw new IllegalStateException("@ClientTest method " + method.getName()
                + " must declare an S3Client, S3AsyncClient, ConfiguredClient, ClientBuildFailed"
                + " or ClientSuccessOrFailure parameter");
    }

    private static Map<Class<?>, List<Category<?>>> selectedCategories(Method method) {
        Map<Class<?>, List<Category<?>>> selected = new LinkedHashMap<>();
        for (Feature annotation : AnnotationSupport.findRepeatableAnnotations(method, Feature.class)) {
            selected.put(annotation.feature(), categoriesOf(method, annotation));
        }
        return selected;
    }

    private static List<Category<?>> categoriesOf(Method method, Feature annotation) {
        boolean hasValues = annotation.values().length > 0;
        boolean hasGroup = annotation.group() != Feature.NoGroup.class;
        if (hasValues && hasGroup) {
            throw new IllegalStateException("@Feature on " + method.getName() + " for "
                    + annotation.feature().getSimpleName()
                    + " must set at most one of values or group");
        }
        if (hasGroup) {
            CategoryGroup<?> group = instantiate(annotation.group());
            return new ArrayList<>(group.categories());
        }
        if (hasValues) {
            List<Category<?>> categories = new ArrayList<>();
            for (Class<? extends Category<?>> categoryClass : annotation.values()) {
                categories.add(instantiate(categoryClass));
            }
            return categories;
        }
        return new ArrayList<>(instantiate(annotation.feature()).allCategories());
    }

    private static void validateHttpTypes(Method method, Class<?> paramType, List<Category<?>> categories) {
        if (paramType != S3AsyncClient.class) {
            return;
        }
        for (Category<?> category : categories) {
            HttpClient.Type type = (HttpClient.Type) category.value();
            if (!type.isAsync()) {
                throw new IllegalStateException("@ClientTest method " + method.getName()
                        + " takes S3AsyncClient but selects a sync HTTP client type: " + type);
            }
        }
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate " + type.getName()
                    + " (needs a public no-arg constructor)", e);
        }
    }

    private static TestTemplateInvocationContext invocationContext(ClientConfig config, Class<?> paramType,
            int payloadSize, boolean labelSize) {
        OutcomeHolder holder = new OutcomeHolder();
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return labelSize ? config.describe() + ", size=" + payloadSize : config.describe();
            }

            @Override
            public List<org.junit.jupiter.api.extension.Extension> getAdditionalExtensions() {
                return List.of(
                        new ClientParameterResolver(config, paramType, payloadSize, holder),
                        new BuildInterceptor(holder));
            }
        };
    }

    private static final class OutcomeHolder {
        private S3TestBase.BuildOutcome outcome;
    }

    private record ClientParameterResolver(ClientConfig config, Class<?> paramType, int payloadSize,
            OutcomeHolder holder) implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            Class<?> type = parameterContext.getParameter().getType();
            return type == S3Client.class || type == S3AsyncClient.class
                    || type == ConfiguredClient.class || type == ClientBuildFailed.class
                    || type == ClientSuccessOrFailure.class
                    || type == Payload.class;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            if (parameterContext.getParameter().getType() == Payload.class) {
                return Payload.ofSize(payloadSize);
            }
            S3TestBase test = (S3TestBase) extensionContext.getRequiredTestInstance();
            S3TestBase.BuildOutcome outcome = test.buildConfigured(config, paramType);
            holder.outcome = outcome;
            return outcome.skip() ? null : outcome.value();
        }
    }

    private record BuildInterceptor(OutcomeHolder holder) implements InvocationInterceptor {

        @Override
        public void interceptTestTemplateMethod(Invocation<Void> invocation,
                ReflectiveInvocationContext<Method> invocationContext,
                ExtensionContext extensionContext) throws Throwable {
            if (holder.outcome != null && holder.outcome.skip()) {
                invocation.skip();
                throw new TestAbortedException(holder.outcome.reason());
            }
            invocation.proceed();
        }
    }
}
