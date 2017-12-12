${fileHeader}
package ${transformPackage};

import org.w3c.dom.Node;
import javax.annotation.Generated;

import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.util.XpathUtils;

import ${metadata.fullModelPackageName}.${shape.shapeName};

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shape.shapeName}Unmarshaller extends ${exceptionUnmarshallerImpl} {

    public ${shape.shapeName}Unmarshaller() {
        super(${shape.shapeName}.class);
    }

    @Override
    public SdkServiceException unmarshall(Node node) throws Exception {
        // Bail out if this isn't the right error code that this
        // marshaller understands
        String errorCode = parseErrorCode(node);
        if(errorCode == null || !errorCode.equals("${shape.errorCode}"))
            return null;

        return super.unmarshall(node);
    }
}
