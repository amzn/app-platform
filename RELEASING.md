# Production Releases

Run the release helper from a clean checkout at the same commit as `origin/main`:

```
./release.sh
```

The script will:

1. Fetch `origin` and verify that `HEAD` matches `origin/main`.
2. Ask for the release version.
3. Move the current `CHANGELOG.md` `Unreleased` section to the release version and today's date.
4. Remove empty changelog sections, insert a fresh `Unreleased` template, and update the comparison links.
5. Ask you to review and confirm the changelog diff.
6. Update `VERSION_NAME` in `gradle.properties` to the release version.
7. Commit the release, create a matching Git tag, bump `VERSION_NAME` to the next patch `-SNAPSHOT`, and commit that bump.

After the script finishes, push the two commits and tag:

```
git push && git push --tags
```

Pushing the tag starts the GitHub Actions release workflow, which publishes to Maven Central and creates the GitHub release.

## Manual fallback

If you need to do the release without the script, use the same steps manually:

1. Checkout `origin/main`.
2. Update `CHANGELOG.md` using the [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format:
   * Copy the template for the next unreleased version at the top.
   * Delete unused sections in the new release.
   * Update the links at the bottom of `CHANGELOG.md`, including the `Unreleased` link.
3. Update `VERSION_NAME` in `gradle.properties` and remove the `-SNAPSHOT` suffix.
4. Commit the changes and create a tag:
   ```
   git commit -am "Releasing 0.1.0."
   git tag 0.1.0
   ```
5. Update `VERSION_NAME` in `gradle.properties` to the next `-SNAPSHOT` version.
6. Commit the change:
   ```
   git commit -am "Prepare next development version."
   ```
7. Push the two commits and tag:
   ```
   git push && git push --tags
   ```

# Snapshot Releases

Snapshot releases are automatically created whenever a non-documentation commit is pushed to `main`.

# Manually uploading a release

Depending on the version in the `gradle.properties` file it will be either a production or snapshot release.

```
./gradlew clean publish --no-build-cache
./gradlew -p gradle-plugin clean publish --no-build-cache
```

# Installing in Maven Local

```
./gradlew publishToMavenLocal
./gradlew -p gradle-plugin publishToMavenLocal
```
