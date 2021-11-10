package software.amazon.awssdk.regions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A tag applied to endpoints to specify that they're to be used in certain contexts. For example, FIPS tags are applied
 * to endpoints discussed here: https://aws.amazon.com/compliance/fips/ and DUALSTACK tags are applied to endpoints that
 * can return IPv6 addresses.
 */
@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public final class EndpointTag {
    public static final EndpointTag DUALSTACK = EndpointTag.of("dualstack");

    public static final EndpointTag FIPS = EndpointTag.of("fips");

    private static final List<EndpointTag> ENDPOINT_TAGS = Collections.unmodifiableList(Arrays.asList(DUALSTACK, FIPS));

    private final String id;

    private EndpointTag(String id) {
        this.id = id;
    }

    public static EndpointTag of(String id) {
        return EndpointTagCache.put(id);
    }

    public static List<EndpointTag> endpointTags() {
        return ENDPOINT_TAGS;
    }

    public String id() {
        return this.id;
    }

    @Override
    public String toString() {
        return id;
    }

    private static class EndpointTagCache {
        private static final ConcurrentHashMap<String, EndpointTag> IDS = new ConcurrentHashMap<>();

        private EndpointTagCache() {
        }

        private static EndpointTag put(String id) {
            return IDS.computeIfAbsent(id, EndpointTag::new);
        }
    }
}
