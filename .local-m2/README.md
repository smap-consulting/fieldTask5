# Project-Local Maven Repository

This directory contains custom dependencies that are checked into version control to enable out-of-box builds without external setup.

## Contents

### JavaRosa 5.1.4-smap

Custom version of JavaRosa with smap-specific enhancements.

**Location:** `org/getodk/javarosa/5.1.4-smap/`

**Files:**
- `javarosa-5.1.4-smap.jar` - Compiled library (681KB)
- `javarosa-5.1.4-smap.module` - Gradle module metadata
- `javarosa-5.1.4-smap.pom` - Maven POM file

**Source:** Built from smap's custom JavaRosa repository

## Why This Exists

Most developers don't need to modify JavaRosa. Checking in the built artifact means:
- ✅ Clone and build works immediately
- ✅ No need to set up separate JavaRosa repository
- ✅ Consistent builds across all environments

## For Developers Modifying JavaRosa

If you need to modify JavaRosa:

1. Clone and modify the smap JavaRosa repository
2. Build and publish to your local Maven: `./gradlew publishToMavenLocal`
3. Gradle automatically uses your `~/.m2/repository/` version (higher priority)
4. When stable, copy artifacts back here and commit

See `CLAUDE.md` for detailed instructions.

## Dependency Resolution Order

Gradle checks repositories in this order:
1. **This directory** (`.local-m2/`) - Checked in version
2. User's home directory (`~/.m2/repository/`) - Local overrides
3. Remote repositories (Maven Central, JitPack, etc.)

This ensures local development can override the checked-in version without conflicts.
