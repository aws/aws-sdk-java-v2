package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.internal.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class ListOfBlobsTypeCopier {
    static List<ByteBuffer> copy(Collection<ByteBuffer> listOfBlobsTypeParam) {
        if (listOfBlobsTypeParam == null) {
            return null;
        }
        List<ByteBuffer> listOfBlobsTypeParamCopy = listOfBlobsTypeParam.stream().map(StandardMemberCopier::copy)
                .collect(toList());
        return Collections.unmodifiableList(listOfBlobsTypeParamCopy);
    }
}
