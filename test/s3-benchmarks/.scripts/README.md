# Benchmark scripts

## running the scripts
from the `.script` folder, use the `benchamrk` script to run a suite. The scripts require the operation 
(download, upload or copy) and the location of the type of file system. The available file systems are:
- `fs`: regular file system, under the root (/) directory
- `tmpfs`: in-memory file system (under the /dev/shm directory)
The Benchmark suite will run the specified operation with different file sizes (1b 8MB+1 8MB-1 128MB 4GB 30GB) and with 
- the different client (v1 TM, v2 TM and CRT S3 client) and save the result for each benchmark under `result/$operation_$location_$name_$version_$size".txt` ie: `result/download_tmpfs_TMv2_128MB.txt`

The files to upload must exist for the benchmarks to succeed. The `create_benchmak_file` script 
can be used to create them.