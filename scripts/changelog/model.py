class ReleaseChanges(object):
    def __init__(self, version, date, entries):
        self.version = version
        self.date = date
        self.entries = entries

class ChangelogEntry(object):
    def __init__(self, type, category, description, contributor):
        self.type = type
        self.category = category
        self.contributor = contributor
        self.description = description

class Version(object):
    def __init__(self, major, minor, patch, prerelease=""):
        self.major = major
        self.minor = minor
        self.patch = patch
        self.prerelease =  prerelease

    def __str__(self):
        s = "%d.%d.%d" % (self.major, self.minor, self.patch)
        if self.prerelease != "":
            s = "%s-%s" % (s, self.prerelease)
        return s

    # TODO Remove it when we remove "preview" from the version number
    # Returns the prerelease version number
    # Example: Version is "preview-11", this method returns "11" as integer
    def prerelease_version_number(self):
        if self.prerelease != "":
            preview_prefix_len = len("preview-")
            prerelease_version = self.prerelease[preview_prefix_len:]
            if prerelease_version != "":
                return int(prerelease_version)