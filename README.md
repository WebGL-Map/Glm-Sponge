# Glm-Sponge [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The WebGL-Map server endpoint for [Sponge](https://www.spongepowered.org/). This endpoint is intended to be compatible 
with [SpongeVanilla](https://github.com/SpongePowered/SpongeVanilla) and [SpongeForge](https://github.com/SpongePowered/SpongeForge).

## Usage
**Web socket server commands**, you can add you own command with the server by registering it with the CommandRegistrar 
during the GlmRegisterCommand event.
```
@Listener
public void onGlmRegisterCommand(@Nonnull final GlmRegisterCommand event) {
    event.getGlmServer().getRegistrar().registerCommand("commandName", new Command());
}
```

## Building
**Note:** If you do not have Gradle installed then use ./gradlew for Unix systems or Git Bash and gradlew.bat for Windows 
systems in place of any 'gradle' command.

In order to build Glm-Sponge just run the `gradle build` command. Once that is finished you will find library, sources, and 
javadoc .jars exported into the `./build/libs` folder and the will be labeled like the following.
```
GlmSponge-x.x.x.jar
GlmSponge-x.x.x-javadoc.jar
GlmSponge-x.x.x-sources.jar
```

**Alternatively** you can include Glm-Sponge in your build.gradle file by using the following.
```
repositories {
    maven {
        name = 'reallifegames'
        url = 'https://reallifegames.net/artifactory/gradle-release-local'
    }
}

dependencies {
    compile 'net.reallifegames:GlmSponge:x.x.x' // For compile time.
}
```