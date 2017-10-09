package software.amazon.awssdk;

import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base per-request override configuration for all SDK requests.
 */
public abstract class SdkRequestOverrideConfig<B extends SdkRequestOverrideConfig.Builder<B, C>,
        C extends SdkRequestOverrideConfig<B, C>> implements ToCopyableBuilder<B, SdkRequestOverrideConfig<B, C>> {

    private final ProgressListener progressListener;

    private final Map<String, String> customHeaders;

    private final Map<String, List<String>> customQueryParameters;

    protected SdkRequestOverrideConfig(B builder) {
        this.progressListener = builder.getProgressListener();
        this.customHeaders = builder.getCustomHeaders();
        this.customQueryParameters = builder.getCustomQueryParameters();
    }

    public Optional<ProgressListener> progressListener() {
        return Optional.ofNullable(progressListener);
    }

    public Optional<Map<String, String>> customHeaders() {
        return Optional.ofNullable(customHeaders);
    }

    public Optional<Map<String, List<String>>> customQueryParameters() {
        return Optional.ofNullable(customQueryParameters);
    }

    public interface Builder<B extends SdkRequestOverrideConfig.Builder<B, C>,
            C extends SdkRequestOverrideConfig<B, C>> extends CopyableBuilder<B, SdkRequestOverrideConfig<B, C>> {
        ProgressListener getProgressListener();

        B progressListener(ProgressListener progressListener);

        Map<String, String> getCustomHeaders();

        B customHeaders(Map<String, String> customHeaders);

        Map<String, List<String>> getCustomQueryParameters();

        B customQueryParameters(Map<String, List<String>> customQueryParameters);
    }

    protected static abstract class BuilderImpl<B extends SdkRequestOverrideConfig.Builder<B, C>,
            C extends SdkRequestOverrideConfig<B, C>> implements Builder<B, C> {
        private final Class<B> concrete;

        private ProgressListener progressListener;

        private Map<String, String> customHeaders;

        private Map<String, List<String>> customQueryParameters;

        protected BuilderImpl(Class<B> concrete) {
            Validate.isInstanceOf(concrete, this, "This class is not an instance of %s!", concrete);
            this.concrete = concrete;
        }

        protected BuilderImpl(Class<B> concrete, C sdkRequestOverrideConfig) {
            this(concrete);
            sdkRequestOverrideConfig.progressListener().map(this::progressListener);
            sdkRequestOverrideConfig.customHeaders().map(this::customHeaders);
            sdkRequestOverrideConfig.customQueryParameters().map(this::customQueryParameters);
        }

        @Override
        public ProgressListener getProgressListener() {
            return progressListener;
        }

        @Override
        public B progressListener(ProgressListener progressListener) {
            this.progressListener = progressListener;
            return concrete.cast(this);
        }

        @Override
        public Map<String, String> getCustomHeaders() {
            return customHeaders;
        }

        public void setCustomHeaders(Map<String, String> customHeaders) {
            customHeaders(customHeaders);
        }

        @Override
        public B customHeaders(Map<String, String> customHeaders) {
            if (customHeaders == null) {
                this.customHeaders = null;
            } else {
                this.customHeaders = new HashMap<>(customHeaders);
            }
            return concrete.cast(this);
        }

        @Override
        public Map<String, List<String>> getCustomQueryParameters() {
            return customQueryParameters;
        }

        public void setCustomQueryParameters(Map<String, List<String>> customQueryParameters) {
            customQueryParameters(customQueryParameters);
        }

        @Override
        public B customQueryParameters(Map<String, List<String>> customQueryParameters) {
            if (customQueryParameters == null) {
                this.customQueryParameters = null;
            } else {
                this.customQueryParameters = new HashMap<>(customQueryParameters.size());
                customQueryParameters.forEach((key, value) -> this.customQueryParameters.put(key, new ArrayList<>(value)));
            }
            return concrete.cast(this);
        }
    }
}
