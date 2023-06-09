package software.amazon.awssdk.core;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for operations with the RequestCompression trait to disable request configuration and set the minimum
 * compression threshold in bytes.
 */
@SdkPublicApi
public class RequestCompressionConfiguration implements ToCopyableBuilder<RequestCompressionConfiguration.Builder,
    RequestCompressionConfiguration> {

    private final Boolean requestCompressionEnabled;
    private final Integer minimumCompressionThresholdInBytes;

    private RequestCompressionConfiguration(DefaultBuilder builder) {
        this.requestCompressionEnabled = builder.requestCompressionEnabled;
        this.minimumCompressionThresholdInBytes = builder.minimumCompressionThresholdInBytes;
    }

    /**
     * If set, returns true if request compression is enabled, else false if request compression is disabled.
     */
    public Boolean requestCompressionEnabled() {
        return requestCompressionEnabled;
    }

    /**
     * If set, returns the minimum compression threshold in bytes, inclusive, in order to trigger request compression.
     */
    public Integer getMinimumCompressionThresholdInBytes() {
        return minimumCompressionThresholdInBytes;
    }

    /**
     * Create a {@link RequestCompressionConfiguration.Builder}, used to create a {@link RequestCompressionConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RequestCompressionConfiguration that = (RequestCompressionConfiguration) o;

        if (requestCompressionEnabled != that.requestCompressionEnabled) {
            return false;
        }
        return Objects.equals(minimumCompressionThresholdInBytes, that.minimumCompressionThresholdInBytes);
    }

    @Override
    public int hashCode() {
        int result = requestCompressionEnabled != null ? requestCompressionEnabled.hashCode() : 0;
        result = 31 * result + (minimumCompressionThresholdInBytes != null ? minimumCompressionThresholdInBytes.hashCode() : 0);
        return result;
    }


    public interface Builder extends CopyableBuilder<Builder, RequestCompressionConfiguration> {

        /**
         * Configures whether request compression is enabled or not.
         *
         * @param requestCompressionEnabled
         * @return This object for method chaining.
         */
        Builder requestCompressionEnabled(Boolean requestCompressionEnabled);

        /**
         * Configures the minimum compression threshold in bytes.
         *
         * @param minimumCompressionThresholdInBytes
         * @return This object for method chaining.
         */
        Builder minimumCompressionThresholdInBytes(Integer minimumCompressionThresholdInBytes);
    }

    private static final class DefaultBuilder implements Builder {
        private Boolean requestCompressionEnabled;
        private Integer minimumCompressionThresholdInBytes;

        private DefaultBuilder() {
        }

        private DefaultBuilder(RequestCompressionConfiguration requestCompressionConfiguration) {
            this.requestCompressionEnabled = requestCompressionConfiguration.requestCompressionEnabled;
            this.minimumCompressionThresholdInBytes = requestCompressionConfiguration.minimumCompressionThresholdInBytes;
        }

        @Override
        public Builder requestCompressionEnabled(Boolean requestCompressionEnabled) {
            this.requestCompressionEnabled = requestCompressionEnabled;
            return this;
        }

        @Override
        public Builder minimumCompressionThresholdInBytes(Integer minimumCompressionThresholdInBytes) {
            this.minimumCompressionThresholdInBytes = minimumCompressionThresholdInBytes;
            return this;
        }

        @Override
        public RequestCompressionConfiguration build() {
            return new RequestCompressionConfiguration(this);
        }
    }
}
