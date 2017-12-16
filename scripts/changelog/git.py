import subprocess
def stage_file(filename):
    return subprocess.call(["git", "add", "-A", filename])