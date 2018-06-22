package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.core.internal.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class BlobMapTypeCopier {
    static Map<String, ByteBuffer> copy(Map<String, ByteBuffer> blobMapTypeParam) {
        if (blobMapTypeParam == null) {
            return null;
        }
        Map<String, ByteBuffer> blobMapTypeParamCopy = blobMapTypeParam.entrySet().stream()
                                                                       .collect(toMap(Map.Entry::getKey, e -> StandardMemberCopier.copy(e.getValue())));
        return Collections.unmodifiableMap(blobMapTypeParamCopy);
    }
}
