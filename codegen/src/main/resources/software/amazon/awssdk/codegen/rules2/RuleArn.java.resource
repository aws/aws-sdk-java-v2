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

    RuleArn(String partition, String service, String region, String accountId, List<String> resourceId) {
        this.partition = partition;
        this.service = service;
        this.region = region;
        this.accountId = accountId;
        this.resourceId = Collections.unmodifiableList(resourceId);
    }

    public static RuleArn parse(String arn) {
        String[] base = arn.split(":", 6);
        if (base.length != 6) {
            return null;
        }
        // service, resource and `arn` may not be null
        if (!base[0].equals("arn")) {
            return null;
        }
        if (base[1].isEmpty() || base[2].isEmpty()) {
            return null;
        }
        if (base[5].isEmpty()) {
            return null;
        }
        return new RuleArn(base[1], base[2], base[3], base[4], Arrays.asList(base[5].split("[:/]", -1)));
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
