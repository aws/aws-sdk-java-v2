${fileHeader}
package ${transformPackage};

import org.w3c.dom.Node;
import javax.annotation.Generated;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.util.XpathUtils;

import ${metadata.fullModelPackageName}.${shape.shapeName};

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shape.shapeName}Unmarshaller extends ${exceptionUnmarshallerImpl} {

    public ${shape.shapeName}Unmarshaller() {
        super(${shape.shapeName}.class);
    }

    @Override
    public AmazonServiceException unmarshall(Node node) throws Exception {
        // Bail out if this isn't the right error code that this
        // marshaller understands
        String errorCode = parseErrorCode(node);
        if(errorCode == null || !errorCode.equals("${shape.errorCode}"))
            return null;

        return (${shape.shapeName})super.unmarshall(node);
    }
}
