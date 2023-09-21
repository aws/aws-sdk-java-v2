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

## Command line arguments

### Benchmark version

The `--version` command line option is used to determine which component is under test:

- `--version=crt` : Indicate to run the benchmark for the CRT's S3Client
- `--version=java` : Indicate to run the benchmark for the java based S3 Async Client (`MultipartS3AsyncClient` class)
- `--version=v2`: SDK v2 transfer manager (using `S3CrtAsyncClient` to delegate requests)
- `--version=v1`: SDK v1 transfer manager (using `AmazonS3Client` to delegate requests)

### Operation

The `--operation` command line argument determine which transfer operation is used

|operation|supported version|
|---|-------|
|download | v1 v2 java crt |
|upload | v1 v2 java crt |
|download_directory | v1 v2 |
|upload_directory | v1 v2 |
|copy | v1 v2 java |

> All command line argument can be found in the `BenchmarkRunner` class.

# Benchmark scripts Automation
From the `.script` folder, use one of the `benchamrk` scripts to run a test suite.

## single file benchmark
### usage
```
benchmark download|upload fs|tmpfs|no-op [<size>]
```
The scripts require the operation
(download or upload) and the location of the type of file system.
- operation: `download|upload`
- file system: `fs|tmpfs`
    - `fs`: regular file system, under the root (`/`) directory
    - `tmpfs`: in-memory file system (under the `/dev/shm` directory)
- size (opt): if specified, will only run the test with the specified size (file must exist)

The Benchmark suite will run the specified operation with different file sizes (1b 8MB+1 8MB-1 128MB 4GB 30GB, if no
size are specified) and with
the different client (v1 TM, v2 TM and CRT S3 client) and save the result for each benchmark under
`result/$operation_$location_$name_$version_$size".txt` ie: `result/download_tmpfs_TMv2_128MB.txt`.
For upload benchjmarks, the files
`1b 8MB+1 8MB-1 128MB 4GB 30GB` must all exists under `/` for `fs` or `/dev/shm/` for `tmpfs`. The `create_benchmak_file`
script can be used to create them.

## copy benchmark
```
benchmark-copy [<size>]
```
Files `1b 8MB+1 8MB-1 128MB 4GB 30GB`, or the one passed as an argument, need to exist in the s3 bucket.

## directory benchmark
```
benchmark-dir download|upload fs|tmpfs [1B|4K|16M|5G]
```
- `fs` is located ia `~/tm_dire_file`
- `tmpfs` is located at `/dev/shm/tm_dir_file`

# Graph scripts
The `ploy.py` creates _Box and Whiskers_ type bar graphs of the test data. **The data is hard coped in the script file.**

dependencies: [plotly](https://plotly.com/python/getting-started/)
```bash
pip install plotly
```

creating static images also requires Kaleido ([more info](https://plotly.com/python/static-image-export/))

```bash
pip install kaleido
```

then simply run the `plot.py` script to generate images in `../images` (will be created if it does not exist)

```bash
python plot.py
```