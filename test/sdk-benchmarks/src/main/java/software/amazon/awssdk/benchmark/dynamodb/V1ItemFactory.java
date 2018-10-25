package software.amazon.awssdk.benchmark.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public final class V1ItemFactory extends AbstractItemFactory<AttributeValue> {
    @Override
    protected AttributeValue av(String val) {
        return new AttributeValue()
                .withS(val);
    }

    @Override
    protected AttributeValue av(ByteBuffer val) {
        return new AttributeValue()
                .withB(val);
    }

    @Override
    protected AttributeValue av(List<AttributeValue> val) {
        return new AttributeValue()
                .withL(val);
    }

    @Override
    protected AttributeValue av(Map<String, AttributeValue> val) {
        return new AttributeValue()
                .withM(val);
    }
}
