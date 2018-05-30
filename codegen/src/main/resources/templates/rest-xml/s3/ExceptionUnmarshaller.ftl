${fileHeader}
package ${transformPackage};

import org.w3c.dom.Node;
import javax.annotation.Generated;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.util.XpathUtils;
import software.amazon.awssdk.services.s3.transform.S3ExceptionUnmarshaller;
import ${metadata.fullModelPackageName}.${shape.shapeName};

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class ${shape.shapeName}Unmarshaller extends S3ExceptionUnmarshaller {

    public ${shape.shapeName}Unmarshaller() {
        super(${shape.shapeName}.class, "${shape.errorCode}");
    }

    @Override
    public AwsServiceException unmarshall(Node node) throws Exception {
        return super.unmarshall(node);
    }
}
