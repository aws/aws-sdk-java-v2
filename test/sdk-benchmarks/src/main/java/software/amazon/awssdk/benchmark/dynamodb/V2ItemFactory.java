package software.amazon.awssdk.benchmark.dynamodb;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public final class V2ItemFactory extends AbstractItemFactory<AttributeValue> {
    @Override
    protected AttributeValue av(String val) {
        return AttributeValue.builder().s(val).build();
    }

    @Override
    protected AttributeValue av(ByteBuffer val) {
        return AttributeValue.builder().b(SdkBytes.fromByteBuffer(val)).build();
    }

    @Override
    protected AttributeValue av(List<AttributeValue> val) {
        return AttributeValue.builder().l(val).build();
    }

    @Override
    protected AttributeValue av(Map<String, AttributeValue> val) {
        return AttributeValue.builder().m(val).build();
    }
}
