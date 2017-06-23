package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.runtime.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToSimpleStructCopier {
    static Map<String, SimpleStruct> copy(Map<String, SimpleStruct> mapOfStringToSimpleStructParam) {
        if (mapOfStringToSimpleStructParam == null) {
            return null;
        }
        Map<String, SimpleStruct> mapOfStringToSimpleStructParamCopy = new HashMap<>(mapOfStringToSimpleStructParam.size());
        for (Map.Entry<String, SimpleStruct> e : mapOfStringToSimpleStructParam.entrySet()) {
            mapOfStringToSimpleStructParamCopy.put(StandardMemberCopier.copy(e.getKey()), e.getValue());
        }
        return mapOfStringToSimpleStructParamCopy;
    }
}

