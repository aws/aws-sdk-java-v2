${fileHeader}
<#assign inputType = operation.input.variableType>
<#assign outputType = operation.returnType.returnType>
<#assign input = operation.input.variableName>
<#assign operationName = operation.operationName>

package ${metadata.fullWaitersPackageName};

import javax.annotation.Generated;

import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.waiters.SdkFunction;
import ${metadata.fullModelPackageName}.${inputType};
import ${metadata.fullModelPackageName}.${outputType};
import ${metadata.fullClientPackageName}.${metadata.syncInterface};

@SdkInternalApi
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${operationName}Function implements SdkFunction<${inputType}, ${outputType}>{

    /**
     * Represents the service client
     */
    private final ${metadata.syncInterface} client;

    /**
      * Constructs a new ${operationName}Function with the
      * given client
      * @param client
      *         Service client
      */
    public ${operationName}Function(${metadata.syncInterface} client){
        this.client = client;
    }

    /**
      * Makes a call to the operation specified by the
      * waiter by taking the corresponding request and
      * returns the corresponding result
      * @param ${input}
      *          Corresponding request for the operation
      * @return Corresponding result of the operation
      */
    @Override
    public ${outputType} apply(${inputType} ${input}){
        return client.${waiter.operationMethodName}(${input});
    }
}
