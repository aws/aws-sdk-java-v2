${fileHeader}
package ${metadata.fullModelPackageName};

import javax.annotation.Generated;

import software.amazon.awssdk.AmazonWebServiceResult;

/**
 * <#if shape.documentation?has_content>${shape.documentation}</#if>
 */
@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shape.shapeName} extends ${baseClassFqcn}
    implements Cloneable {

    <@VariableDeclarationMacro.content shape/>

    <#if shape.additionalConstructors?has_content>
        <@ConstructorDefinitionMacro.content shape/>
    </#if>

    <@MethodDefinitionMacro.content customConfig shape shape.shapeName/>

    <#if shape.shapeName == "GetConsoleOutputResult">
    /**
     * The decoded console output.
     *
     * @return The decoded console output.
     */
    public String getDecodedOutput() {
        byte[] bytes = software.amazon.awssdk.utils.BinaryUtils.fromBase64(output);
        return new String(bytes, software.amazon.awssdk.util.StringUtils.UTF8);
    }
    </#if>

    <@OverrideMethodsMacro.content shape/>

    @Override
    public ${shape.shapeName} clone() {
        try {
            return (${shape.shapeName}) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(
                    "Got a CloneNotSupportedException from Object.clone() "
                    + "even though we're Cloneable!",
                    e);
        }
    }
}
