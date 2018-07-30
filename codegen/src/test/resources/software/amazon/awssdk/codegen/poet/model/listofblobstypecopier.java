package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.adapter.StandardMemberCopier;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

@Generated("software.amazon.awssdk:codegen")
final class ListOfBlobsTypeCopier {
    static List<SdkBytes> copy(Collection<SdkBytes> listOfBlobsTypeParam) {
        if (listOfBlobsTypeParam == null || listOfBlobsTypeParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<SdkBytes> listOfBlobsTypeParamCopy = listOfBlobsTypeParam.stream().map(StandardMemberCopier::copy).collect(toList());
        return Collections.unmodifiableList(listOfBlobsTypeParamCopy);
    }
}
