${fileHeader}
package ${metadata.fullModelPackageName};

import software.amazon.awssdk.opensdk.SdkErrorHttpMetadata;
import software.amazon.awssdk.opensdk.internal.BaseException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import javax.annotation.Generated;

/**
 * Base exception for all service exceptions thrown by ${metadata.serviceFullName}
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${className} extends ${baseExceptionFqcn} implements BaseException {

    private static final long serialVersionUID = 1L;

    private SdkErrorHttpMetadata sdkHttpMetadata;

    private String message;

    /**
     * Constructs a new ${className} with the specified error
     * message.
     *
     * @param message Describes the error encountered.
     */
    public ${className}(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public ${className} sdkHttpMetadata(SdkErrorHttpMetadata sdkHttpMetadata) {
        this.sdkHttpMetadata = sdkHttpMetadata;
        return this;
    }

    @Override
    public SdkErrorHttpMetadata sdkHttpMetadata() {
        return sdkHttpMetadata;
    }

    @SdkInternalApi
    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
