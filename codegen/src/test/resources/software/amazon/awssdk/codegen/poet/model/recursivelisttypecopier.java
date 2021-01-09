package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

@Generated("software.amazon.awssdk:codegen")
final class RecursiveListTypeCopier {
    static List<RecursiveStructType> copy(Collection<RecursiveStructType> recursiveListTypeParam) {
        if (recursiveListTypeParam == null || recursiveListTypeParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<RecursiveStructType> recursiveListTypeParamCopy = new ArrayList<>(recursiveListTypeParam);
        return Collections.unmodifiableList(recursiveListTypeParamCopy);
    }

    static List<RecursiveStructType> copyFromBuilder(Collection<? extends RecursiveStructType.Builder> recursiveListTypeParam) {
        if (recursiveListTypeParam == null || recursiveListTypeParam instanceof DefaultSdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        return copy(recursiveListTypeParam.stream().map(RecursiveStructType.Builder::build).collect(toList()));
    }
}
