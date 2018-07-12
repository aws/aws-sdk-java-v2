${fileHeader}
package ${metadata.fullModelPackageName};

import javax.annotation.Generated;

/**
 * Base exception for all service exceptions thrown by ${metadata.serviceFullName}
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${className} extends ${baseExceptionFqcn} {

    /**
     * Constructs a new ${className} with the specified error
     * message.
     *
     * @param message Describes the error encountered.
     */
    public ${className}(Builder builder) {
        super(builder);
    }



}
