# OpenDJ Community Edition

OpenDJ is a directory server which implements a wide range of Lightweight Directory Access Protocol and related standards, including full compliance with LDAPv3 but also support for Directory Service Markup Language (DSMLv2). Written in Java, OpenDJ offers multi-master replication, access control, and many extensions.

### About the Community Version

ForgeRock created OpenDJ Community Version from their End of Service Life Identity Platform. This code was first released as part of the ForgeRock Identity Platform. 

To find out about the enterprise release of the ForgeRock platform [here][ForgeRock Identity Platform].

## How do I build it?

Once you have checked out the OpenDJ code from the git repository, you may build and package it.

Building OpenDJ requires Java Development Kit (JDK) 6.0 or higher. The build script automatically tries to find a suitable version of the Java SDK, but you may need to specify the location of the JDK using the JAVA_HOME environment variable, or to download it and install it on your build machine. The JDK can be downloaded from Oracle website, for most platforms.

The easiest way to build OpenDJ, is to do this from the command line.

Go in the directory that was created when you checked out the code. Depending on the checkout, you may need to go down lower directories like "OpenDJ/trunk/opends". The correct directory contains the build.xml, build.sh and build.bat scripts.

On Unix, run the `build.sh` command to build and package the OpenDJ project.

On Windows, run the `build.bat` command.
No argument is required and the build should be done in a few seconds and ends with a "BUILD SUCCESSFUL" message.

The output of the build can be found in the build/package/ directory.

There are a number of targets that may be specified when building OpenDJ, to select what is performed during the build. These targets include :

##### package
This is the default target and is does perform a clean build, compiling OpenDJ server and packaging it in a ZIP file.
#### dsml
This target first performs the package target, and then builds the DSML to LDAP gateway which is a web application, delivered in the form of a War file. The war file is built in the build/package/ directory.
#### javadoc
First performs the dsml target and then generates the JavaDoc documentation from the server source code. The resulting documentation is located in the build/javadoc directory.
#### test
Runs the unit tests against the built server. The package target must have been run first. 
#### checkstyle
Performs a set of verifications against the OpenDJ source code to check that the code guidelines defined for the project are met.
#### precommit
Performs all of the work done by the checkstyle, javadoc and test targets. The precommit target is the one that all developers MUST run before committing any change to the OpenDJ code repository.
#### clean
Removes all the files that are generated during a previous build.
#### compile
Compiles the main OpenDJ source code into the build/classes directory. The result is not usable for tests, but the target can be used to quickly check if the code compiles.

### Using Targets
The use of targets is simple, just name the target on the command line. For example:
`./build.sh precommit`

## Modifying the GitHub Project Page

The OpenDJ Community Edition project pages are published via the `gh-pages` branch, which contains all the usual artifacts to create the web page. The GitHub page is served up directly from this branch by GitHub.

## Getting Started with OpenDJ

ForgeRock provide a comprehensive set of documents for OpenDJ. They maybe found [here][OpenDJ 2.6 Docs].

## Issues

Issues are handled via the [GitHub issues page for the project][GitHub Issues].

## How to Collaborate

Collaborate by:

- [Reporting an issue][GitHub Issues]
- [Fixing an issue][Help Wanted Issues]
- [Contributing to the Wiki][Project Wiki]

Code collaboration is done by creating an issue, discussing the changes in the issue. When the issue's been agreed then, fork, modify, test and submit a pull request. 

## Licensing

The Code an binaries are covered under the [CDDL 1.0 license](https://forgerock.org/cddlv1-0/).

# All the Links

- [GitHub Project]
- [Project Wiki]
- [GitHub Issues]
- [Help Wanted Issues]
- [OpenDJ 2.6 Docs]
- [ForgeRock Identity Platform]

[GitHub Project]:https://github.com/ForgeRock/opendj-community-edition-2.6.4
[GitHub Issues]:https://github.com/ForgeRock/opendj-community-edition-2.6.4/issues
[Help Wanted Issues]:https://github.com/ForgeRock/opendj-community-edition-2.6.4/labels/help%20wanted
[Project Wiki]:https://github.com/ForgeRock/opendj-community-edition-2.6.4/wiki
[ForgeRock Identity Platform]:https://www.forgerock.com/platform/
[OpenDJ 2.6 Docs]:https://backstage.forgerock.com/docs/opendj/2.6
