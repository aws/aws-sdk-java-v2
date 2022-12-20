import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An AWS Arn.
 */
@SdkInternalApi
public final class Arn {
    private final String partition;
    private final String service;
    private final String region;
    private final String accountId;
    private final List<String> resource;

    public Arn(String partition, String service, String region, String accountId, List<String> resource) {
        this.partition = partition;
        this.service = service;
        this.region = region;
        this.accountId = accountId;
        this.resource = resource;
    }

    public static Optional<Arn> parse(String arn) {
        String[] base = arn.split(":", 6);
        if (base.length != 6) {
            return Optional.empty();
        }
        // service, resource and `arn` may not be null
        if (!base[0].equals("arn")) {
            return Optional.empty();
        }
        if (base[1].isEmpty() || base[2].isEmpty()) {
            return Optional.empty();
        }
        if (base[5].isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Arn(base[1], base[2], base[3], base[4],
                                   Arrays.stream(base[5].split("[:/]", -1))
                                         .collect(Collectors.toList())));
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

    public List<String> resource() {
        return resource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Arn arn = (Arn) o;

        if (partition != null ? !partition.equals(arn.partition) : arn.partition != null) {
            return false;
        }
        if (service != null ? !service.equals(arn.service) : arn.service != null) {
            return false;
        }
        if (region != null ? !region.equals(arn.region) : arn.region != null) {
            return false;
        }
        if (accountId != null ? !accountId.equals(arn.accountId) : arn.accountId != null) {
            return false;
        }
        return resource != null ? resource.equals(arn.resource) : arn.resource == null;
    }

    @Override
    public int hashCode() {
        int result = partition != null ? partition.hashCode() : 0;
        result = 31 * result + (service != null ? service.hashCode() : 0);
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Arn[" +
                "partition=" + partition + ", " +
                "service=" + service + ", " +
                "region=" + region + ", " +
                "accountId=" + accountId + ", " +
                "resource=" + resource + ']';
    }

}
