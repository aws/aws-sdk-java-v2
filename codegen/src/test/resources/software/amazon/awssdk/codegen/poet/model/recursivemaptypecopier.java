package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.runtime.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class RecursiveMapTypeCopier {
    static Map<String, RecursiveStructType> copy(Map<String, RecursiveStructType> recursiveMapTypeParam) {
        if (recursiveMapTypeParam == null) {
            return null;
        }
        Map<String, RecursiveStructType> recursiveMapTypeParamCopy = new HashMap<>(recursiveMapTypeParam.size());
        for (Map.Entry<String, RecursiveStructType> e : recursiveMapTypeParam.entrySet()) {
            recursiveMapTypeParamCopy.put(StandardMemberCopier.copy(e.getKey()), e.getValue());
        }
        return recursiveMapTypeParamCopy;
    }
}

