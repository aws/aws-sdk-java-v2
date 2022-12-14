# Benchmark scripts
## single file benchmark
### usage
```
benchmark download|upload|no-op fs|tmpfs|no-op [<size>]
```
From the `.script` folder, use the `benchamrk` script to run a suite. The scripts require the operation
(download, upload or copy) and the location of the type of file system.
- operation: `download|upload`
- file system: `fs|tmpfs`
  - `fs`: regular file system, under the root (/) directory
  - `tmpfs`: in-memory file system (under the /dev/shm directory)
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

# Plot scripts
Creates _Box and Whiskers_ type bar grpah of the test data. The data is hard coped in the script file.

dependencies: [plotly](https://plotly.com/python/getting-started/)
```bash
pip install plotly
```

creating static images also requires Kaleido ([more info](https://plotly.com/python/static-image-export/))

```bash
pip install -U kaleido
```

then simply run the `plot.py` script to generate images in `../images` (will be created if it does not exist)

```bash
python plot.py
```