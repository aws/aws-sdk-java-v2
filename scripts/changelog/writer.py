from git import stage_file
from util import load_all_released_changes, load_unreleased_changes, version_cmp


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
            m.setdefault(e.category, []).append(e.description)
            self.categories.add(e.category)

    def get_sorted_categories(self):
        def category_cmp(a,b):
            return cmp(a,b)

        return sorted(list(self.categories), cmp=category_cmp)

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
        items = sorted(map.get(category, []))
        self.write_entries_with_header(header, items)

    def write_entries_with_header(self, header, entries):
        if not len(entries) > 0:
            return
        self.write("  - ### %s\n" % header)
        for e in entries:
            self.write_entry(e)
        self.write('\n')

    def write_entry(self,e):
        entry_lines = e.splitlines(True)
        self.write("    - %s" % entry_lines[0])
        for l in entry_lines[1:]:
            if len(l.strip()) == 0:
                self.write("\n")
            else:
                self.write("      %s" % l)
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
    released = sorted(released, key=lambda c: c.version, cmp=version_cmp)

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