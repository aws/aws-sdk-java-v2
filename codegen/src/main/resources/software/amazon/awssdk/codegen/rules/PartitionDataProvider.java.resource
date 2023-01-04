import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface PartitionDataProvider {
    Partitions loadPartitions();
}
