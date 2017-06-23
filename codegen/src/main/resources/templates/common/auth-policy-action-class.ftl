${fileHeader}
package software.amazon.awssdk.auth.policy.actions;

import javax.annotation.Generated;

import software.amazon.awssdk.auth.policy.Action;

<#assign serviceAbbreviation = (metadata.serviceAbbreviation)!metadata.serviceFullName/>

/**
 * The available AWS access control policy actions for ${serviceAbbreviation}.
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
 public enum ${serviceName}Actions implements Action {

    /** Represents any action executed on ${serviceAbbreviation}. */
    All${serviceName}Actions("${actionPrefix}:*"),

    <#list operations as operation>
        /** Action for the ${operation} operation. */
        ${operation}("${actionPrefix}:${operation}"),
    </#list>

    ;

    private final String action;

    private ${serviceName}Actions(String action) {
        this.action = action;
    }

    public String getActionName() {
        return this.action;
    }
 }
