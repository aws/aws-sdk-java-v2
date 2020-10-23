from changelog.git import stage_file
from changelog.util import load_all_released_changes, load_unreleased_changes, version_cmp
from functools import cmp_to_key
from operator import attrgetter

class ChangelogWriter(object):
    """
    Writes ReleaseChanges objects to a file using the following format:

    # __VERSION__ __YYYY-MM-DD__
    ## __Category__
      - ### __Features__
        - ...
      - ### __Bugfixes__
        - ...
      - ### __Deprecations__
        - ...
    """

    def __init__(self, output_file):
        self.output_file = output_file

    def write_changes(self, changes):
        self.process_changes(changes)
        self.write_header()
        for s in self.get_sorted_categories():
            self.write_category_header(s)
            self.write_items_for_category(s, self.features, "Features")
            self.write_items_for_category(s, self.bugfixes, "Bugfixes")
            self.write_items_for_category(s, self.deprecations, "Deprecations")
            self.write_items_for_category(s, self.removals, "Removals")
        self.write_contributors()

    def write_contributors(self):
        contributors = set()
        for e in self.current_changes.entries:
            if e.contributor:
                contributors.add(e.contributor)

        if contributors:
            self.output_file.write("## __Contributors__\n")
            contributors_string = ', '.join(contributors)
            self.output_file.write("Special thanks to the following contributors to this release: \n")
            self.output_file.write("\n" + contributors_string + "\n")

    def process_changes(self, changes):
        self.current_changes = changes
        self.reset_maps()
        self.group_entries()

    def reset_maps(self):
        self.features = {}
        self.bugfixes = {}
        self.deprecations = {}
        self.removals = {}
        self.categories = set()

    def group_entries(self):
        for e in self.current_changes.entries:
            m = self.get_map_for_type(e.type)
            m.setdefault(e.category, []).append(e)
            self.categories.add(e.category)

    def get_sorted_categories(self):
        return sorted(list(self.categories))

    def is_service_category(self,s):
        return s.lower() not in NON_SERVICE_CATEGORIES

    def write_header(self):
        version_string = self.current_changes.version
        if version_string is None:
            version_string = "@AWS_JAVA_SDK_VERSION@"
        self.write("# __%s__ __%s__\n" % (version_string, self.current_changes.date))

    def write_category_header(self, c):
        self.output_file.write("## __%s__\n" % c)

    def write_items_for_category(self, category, map, header):
        entries = map.get(category, [])
        items = sorted(entries, key=attrgetter('description'))
        self.write_entries_with_header(header, items)

    def write_entries_with_header(self, header, entries):
        if not len(entries) > 0:
            return
        self.write("  - ### %s\n" % header)
        for e in entries:
            self.write_entry(e)
        self.write('\n')

    def write_entry(self,e):
        description = e.description
        entry_lines = description.splitlines(True)
        self.write("    - %s" % entry_lines[0])
        for l in entry_lines[1:]:
            if len(l.strip()) == 0:
                self.write("\n")
            else:
                self.write("      %s" % l)
        self.write('\n')
        if e.contributor:
            self.write("        - ")
            self.write("Contributed by: " + e.contributor)
            self.write('\n')

    def get_map_for_type(self, t):
        if t == 'feature':
            return self.features
        elif t == 'bugfix':
            return self.bugfixes
        elif t == 'deprecation':
            return self.deprecations
        elif t == 'removal':
            return self.removals
        else:
            raise Exception("Unknown entry type %s!" % t)

    def write(self, s):
        self.output_file.write(s)

def write_changelog():
    unreleased = load_unreleased_changes(".changes/next-release")
    released = load_all_released_changes(".changes")
    released = sorted(released, key=lambda c: [c.version.major, c.version.minor, c.version.patch, c.version.prerelease_version_number()], reverse=True)

    if unreleased is not None:
        all_changes = [unreleased] + released
    else:
        all_changes = released

    if len(all_changes) == 0:
        return

    with open('CHANGELOG.md', 'w') as cl:
        writer = ChangelogWriter(cl)
        for changes in all_changes:
            writer.write_changes(changes)

    stage_file('CHANGELOG.md')
