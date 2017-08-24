package software.amazon.awssdk.services.jsonprotocoltests.model;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.runtime.StandardMemberCopier;

@Generated("software.amazon.awssdk:codegen")
final class MapOfStringToStringCopier {
    static Map<String, String> copy(Map<String, String> mapOfStringToStringParam) {
        if (mapOfStringToStringParam == null) {
            return null;
        }

        Map<String, String> mapOfStringToStringParamCopy = mapOfStringToStringParam.entrySet().stream()
                                                                                   .collect(toMap(e -> StandardMemberCopier.copy(e.getKey()), Map.Entry::getValue));

        return Collections.unmodifiableMap(mapOfStringToStringParamCopy);
    }
}

