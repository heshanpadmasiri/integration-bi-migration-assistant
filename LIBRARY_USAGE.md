# Using the Common Library

The `common` module has been configured as a library and published to Maven local. You can now use it in other projects.

## Publishing to Maven Local

To publish the library to your local Maven repository, run:

```bash
./gradlew :common:publishToMavenLocal
```

## Using in Other Projects

### Gradle Projects

Add the following dependency to your `build.gradle` file:

```gradle
dependencies {
    implementation 'com.wso2:integration-bi-migration-assistant-common:0.1.0-SNAPSHOT'
}
```

Make sure you have `mavenLocal()` in your repositories:

```gradle
repositories {
    mavenLocal()
    mavenCentral()
    // ... other repositories
}
```

### Maven Projects

Add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>com.wso2</groupId>
    <artifactId>integration-bi-migration-assistant-common</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Published Artifacts

The following artifacts are published:

1. **Main JAR**: `integration-bi-migration-assistant-common-0.1.0-SNAPSHOT.jar` - Contains the compiled classes
2. **Sources JAR**: `integration-bi-migration-assistant-common-0.1.0-SNAPSHOT-sources.jar` - Contains the source code
3. **Javadoc JAR**: `integration-bi-migration-assistant-common-0.1.0-SNAPSHOT-javadoc.jar` - Contains the generated documentation
4. **POM**: `integration-bi-migration-assistant-common-0.1.0-SNAPSHOT.pom` - Contains the dependency metadata

## Location in Maven Local

The library is installed at:
```
~/.m2/repository/com/wso2/integration-bi-migration-assistant-common/0.1.0-SNAPSHOT/
```

## Updating the Version

To publish a new version, update the `version` property in the `common/build.gradle` file and run the publish task again.

## Available APIs

The library includes the `BallerinaModel` class and other common utilities for the Integration BI Migration Assistant. Refer to the Javadoc for detailed API documentation.
