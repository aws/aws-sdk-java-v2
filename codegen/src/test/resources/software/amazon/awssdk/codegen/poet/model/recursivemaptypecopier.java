package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.runtime.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class RecursiveMapTypeCopier {
    static Map<String, RecursiveStructType> copy(Map<String, RecursiveStructType> recursiveMapTypeParam) {
        if (recursiveMapTypeParam == null) {
            return null;
        }
        Map<String, RecursiveStructType> recursiveMapTypeParamCopy = recursiveMapTypeParam.entrySet().stream()
                                                                                          .collect(toMap(e -> StandardMemberCopier.copy(e.getKey()), Map.Entry::getValue));
        return Collections.unmodifiableMap(recursiveMapTypeParamCopy);
    }

    static Map<String, RecursiveStructType> copyFromBuilder(
        Map<String, ? extends RecursiveStructType.Builder> recursiveMapTypeParam) {
        if (recursiveMapTypeParam == null) {
            return null;
        }
        return copy(recursiveMapTypeParam.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().build())));
    }
}
