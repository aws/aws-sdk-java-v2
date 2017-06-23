${fileHeader}
<#assign outputType = operation.returnType.returnType>

package ${metadata.fullWaitersPackageName};

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.waiters.WaiterAcceptor;
import software.amazon.awssdk.waiters.WaiterState;
import software.amazon.awssdk.waiters.AcceptorPathMatcher;
import ${metadata.fullModelPackageName}.*;

import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.awssdk.jmespath.*;

import java.io.IOException;
import javax.annotation.Generated;

@SdkInternalApi
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
class ${waiter.waiterName} {

<#list waiter.acceptors as acceptor>

    <#if !acceptor.isStatusMatcher>
    static class Is${acceptor.expectedAsCamelCase}Matcher extends WaiterAcceptor<${outputType}> {
        <#if acceptor.isErrorMatcher>
            /**
              * Takes the response exception and determines whether this
              * exception matches the expected exception, by comparing the
              * respective error codes.
              * @param e
              *          Response Exception
              * @return True if it matches, False otherwise
              */
            @Override
            public boolean matches(AmazonServiceException e){
                return ${acceptor.expectedAsString}.equals(e.getErrorCode());
            }
        <#else>
            private static final JsonNode EXPECTED_RESULT;

            static{
                 try{
                      EXPECTED_RESULT = ObjectMapperSingleton.getObjectMapper().readTree("${acceptor.expectedAsEscapedJson}");
                 }
                 catch(IOException ioe){
                      throw new RuntimeException(ioe);
                 }
            }

            private static final JmesPathExpression AST = ${acceptor.ast};

            /**
              * Takes the result and determines whether the state of the
              * resource matches the expected state. To determine the current
              * state of the resource, JmesPath expression is evaluated and
              * compared against the expected result.
              * @param result
              *          Corresponding result of the operation
              * @return True if current state of the resource matches the
              *         expected state, False otherwise
              */
            @Override
            public boolean matches(${outputType} result) {
                JsonNode queryNode = ObjectMapperSingleton.getObjectMapper().valueToTree(result);
                JsonNode finalResult = AST.accept(new JmesPathEvaluationVisitor(), queryNode);
                return AcceptorPathMatcher.${acceptor.matcher}(EXPECTED_RESULT, finalResult);
            }
        </#if>

        /**
          * Represents the current waiter state in the case
          * where resource state matches the expected state
          * @return Corresponding state of the waiter
          */
        @Override
        public WaiterState getState(){
            return ${acceptor.enumState};
        }
    }
    </#if>
</#list>
}
