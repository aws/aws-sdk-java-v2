# S3 Benchmark Harness


This module contains performance tests for `S3AsyncClient` and 
`S3TransferManager`

## How to run

```
# Build the JAR
mvn clean install -pl :s3-benchmarks -P quick --am

# download
java -jar s3-benchmarks.jar --bucket=bucket --key=key -file=/path/to/destionfile/ --operation=download --partSizeInMB=20 --maxThroughput=100.0

# upload
java -jar s3-benchmarks.jar --bucket=bucket --key=key -file=/path/to/sourcefile/ --operation=upload --partSizeInMB=20 --maxThroughput=100.0
```
