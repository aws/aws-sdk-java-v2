import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class RuleArn {
    private final String partition;
    private final String service;
    private final String region;
    private final String accountId;
    private final List<String> resourceId;

    private static final int ARN_MIN_LENGTH = 8;
    private static final int ARN_PREFIX_LENGTH = 3;

    RuleArn(String partition, String service, String region, String accountId, List<String> resourceId) {
        this.partition = partition;
        this.service = service;
        this.region = region;
        this.accountId = accountId;
        this.resourceId = Collections.unmodifiableList(resourceId);
    }

    public static RuleArn parse(String arn) {
        if (arn == null || arn.length() < ARN_MIN_LENGTH || !arn.startsWith("arn:")) {
            return null;
        }

        // find each of the first five ':' positions
        int p0 = ARN_PREFIX_LENGTH; // after "arn"
        int p1 = arn.indexOf(':', p0 + 1);
        if (p1 < 0) {
            return null;
        }

        int p2 = arn.indexOf(':', p1 + 1);
        if (p2 < 0) {
            return null;
        }

        int p3 = arn.indexOf(':', p2 + 1);
        if (p3 < 0) {
            return null;
        }

        int p4 = arn.indexOf(':', p3 + 1);
        if (p4 < 0) {
            return null;
        }

        // extract and validate mandatory parts
        String partition = arn.substring(p0 + 1, p1);
        String service = arn.substring(p1 + 1, p2);
        String region = arn.substring(p2 + 1, p3);
        String accountId = arn.substring(p3 + 1, p4);
        String resource = arn.substring(p4 + 1);

        if (partition.isEmpty() || service.isEmpty() || resource.isEmpty()) {
            return null;
        }
        return new RuleArn(partition, service, region, accountId, splitResource(resource));
    }

    private static List<String> splitResource(String resource) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int length = resource.length();
        for (int i = 0; i < length; i++) {
            char c = resource.charAt(i);
            if (c == ':' || c == '/') {
                result.add(resource.substring(start, i));
                start = i + 1;
            }
        }
        result.add(resource.substring(start));
        return result;
    }

    public String partition() {
        return partition;
    }

    public String service() {
        return service;
    }

    public String region() {
        return region;
    }

    public String accountId() {
        return accountId;
    }

    public List<String> resourceId() {
        return resourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RuleArn ruleArn = (RuleArn) o;
        return partition.equals(ruleArn.partition) && service.equals(ruleArn.service) && region.equals(ruleArn.region)
               && accountId.equals(ruleArn.accountId) && resourceId.equals(ruleArn.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partition, service, region, accountId, resourceId);
    }

    @Override
    public String toString() {
        return "Arn[" + "partition=" + partition + ", " + "service=" + service + ", " + "region=" + region + ", " + "accountId="
               + accountId + ", " + "resource=" + resourceId + ']';
    }
}
