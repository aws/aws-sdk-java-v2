from datetime import date
import os
import json
from model import ReleaseChanges, ChangelogEntry, Version

def version_cmp(a,b):
    aa = [a.major, a.minor, a.patch, a.prerelease_version_number()]
    bb = [b.major, b.minor, b.patch, b.prerelease_version_number()]
    return cmp(bb,aa)

def load_all_released_changes(d):
    if not os.path.isdir(d):
        return []
    return [load_release_changes(os.path.join(d, fn)) for fn in os.listdir(d) if fn.endswith('.json')]

def load_release_changes(fn):
    with open(fn) as f:
        return parse_release_changes(json.loads(f.read()))

def load_unreleased_changes(d):
    if not os.path.exists(d):
        return None
    return ReleaseChanges(None, date.today().isoformat(), load_unreleased_entries(d))

def load_unreleased_entries(d):
    entries = []
    for f in [f for f in os.listdir(d) if f.endswith('.json')]:
        with open(os.path.join(d,f)) as e:
            entries.append(parse_changelog_entry(json.loads(e.read())))
    return entries

def parse_release_changes(changes_json):
    version = parse_version_string(changes_json['version'])
    date = changes_json['date']
    entries = [parse_changelog_entry(e) for e in changes_json['entries']]
    return ReleaseChanges(version, date, entries)

def parse_changelog_entry(entry_json):
    return ChangelogEntry(entry_json['type'], entry_json['category'], entry_json['description'])

def parse_version_string(s):
    version_parts = [s for s in s.split('.')]
    prerelease = ""
    hyphen_index = version_parts[2].find('-')
    if hyphen_index != -1:
        p = version_parts[2]
        version_parts[2] = p[0:hyphen_index]
        prerelease = p[hyphen_index + 1:]
    return Version(int(version_parts[0]), int(version_parts[1]), int(version_parts[2]), prerelease)

class ReleaseChangesEncoder(json.JSONEncoder):
    def default(self, o):
        if type(o) is Version:
            # store as a "MAJOR.MINOR.PATCH" string so it's a little easier to
            # read the raw JSON
            return o.__str__()
        return o.__dict__

def marshall_release_changes(changes):
    return json.dumps(changes, indent=4, separators=(',', ': '), cls=ReleaseChangesEncoder)
