import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.MapUtils;

@SdkInternalApi
public class PartitionFn extends SingleArgFn {
    public static final String ID = "aws.partition";

    public static final Identifier NAME = Identifier.of("name");
    public static final Identifier DNS_SUFFIX = Identifier.of("dnsSuffix");
    public static final Identifier DUAL_STACK_DNS_SUFFIX = Identifier.of("dualStackDnsSuffix");
    public static final Identifier SUPPORTS_FIPS = Identifier.of("supportsFIPS");
    public static final Identifier SUPPORTS_DUAL_STACK = Identifier.of("supportsDualStack");
    public static final Identifier INFERRED = Identifier.of("inferred");

    private final LazyValue<PartitionData> partitionData = LazyValue.<PartitionData>builder()
            .initializer(this::loadPartitionData)
            .build();

    private final LazyValue<Partition> awsPartition = LazyValue.<Partition>builder()
                                                                   .initializer(this::findAwsPartition)
                                                                   .build();

    public PartitionFn(FnNode node) {
        super(node);
    }

    public static PartitionFn ofExprs(Expr expr) {
        return new PartitionFn(FnNode.ofExprs(ID, expr));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitPartition(this);
    }

    public static PartitionFn fromParam(Parameter param) {
        return PartitionFn.ofExprs(param.expr());
    }

    @Override
    public Value evalArg(Value arg) {
        String regionName = arg.expectString();

        PartitionData data = partitionData.value();

        Partition matchedPartition;
        boolean inferred = false;

        // Known region
        matchedPartition = data.regionMap.get(regionName);
        if (matchedPartition == null) {
            // try matching on region name pattern
            for (Partition p : data.partitions) {
                Pattern regex = Pattern.compile(p.regionRegex());
                if (regex.matcher(regionName).matches()) {
                    matchedPartition = p;
                    inferred = true;
                    break;
                }
            }
        }

        // Couldn't find the region by name or pattern matching. Fallback to 'aws' partition.
        if (matchedPartition == null) {
            matchedPartition = awsPartition.value();
        }

        Outputs matchedOutputs = matchedPartition.outputs();
        return Value.fromRecord(MapUtils.of(
                NAME, Value.fromStr(matchedPartition.id()),
                DNS_SUFFIX, Value.fromStr(matchedOutputs.dnsSuffix()),
                DUAL_STACK_DNS_SUFFIX, Value.fromStr(matchedOutputs.dualStackDnsSuffix()),
                SUPPORTS_FIPS, Value.fromBool(matchedOutputs.supportsFips()),
                SUPPORTS_DUAL_STACK, Value.fromBool(matchedOutputs.supportsDualStack()),
                INFERRED, Value.fromBool(inferred)));
    }

    private PartitionData loadPartitionData() {
        PartitionDataProvider provider = new DefaultPartitionDataProvider();

        // TODO: support custom partitions.json
        Partitions partitions = provider.loadPartitions();

        PartitionData partitionData = new PartitionData();

        partitions.partitions().forEach(part -> {
            partitionData.partitions.add(part);
            part.regions().forEach((name, override) -> {
                partitionData.regionMap.put(name, part);
            });
        });

        return partitionData;
    }

    private Partition findAwsPartition() {
        return partitionData.value()
            .partitions
            .stream()
            .filter(p -> p.id().equalsIgnoreCase("aws"))
            .findFirst().orElse(null);
    }

    private static class PartitionData {
        private final List<Partition> partitions = new ArrayList<>();
        private final Map<String, Partition> regionMap = new HashMap<>();
    }


    private static final class LazyValue<T> {
        private final Supplier<T> initializer;
        private T value;
        private boolean initialized;

        private LazyValue(Builder<T> builder) {
            this.initializer = builder.initializer;
        }

        public T value() {
            if (!initialized) {
                value = initializer.get();
                initialized = true;
            }
            return value;
        }

        public static <T> Builder<T> builder() {
            return new Builder<>();
        }

        public static class Builder<T> {
            private Supplier<T> initializer;

            public Builder<T> initializer(Supplier<T> initializer) {
                this.initializer = initializer;
                return this;
            }

            public LazyValue<T> build() {
                return new LazyValue<>(this);
            }
        }
    }

}
