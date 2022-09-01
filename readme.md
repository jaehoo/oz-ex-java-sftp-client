[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.test%3Asftp-client&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=com.test%3Asftp-client)

# java-sftp-client

A demo project to show how to create a single sftp client with Jsch and Sshd libraries.


## Requirements

- Oracle Java JDK 64-Bit Server VM +**1.8.0_221**
- Apache maven +**3.6.3**

## Configuration


| **Resource**        | **Location**       | **Description**      |
|:--------------------|:-------------------|:---------------------|
| `logaback-test.xml` | src/test/resources | logger configuration |


## Build

### Test

```bash 
# Testing
mvn clean test

# Testing with code coverage
mvn clean
```

## Release

Use one of the next commands according that you need to execute. Please, note that it will do some changes in the repository. 

```bash
# clean release
mvn release:clean -P scm-release

#update project versions
mvn release:update-versions P scm-release

# Simulation mode 
mvn release:prepare -P scm-release -DskipTests  \
-Darguments="-DskipTests -DpushChanges=false -DdryRun=true -DdeveloperConnectionUrl=scm:git:${git.repo.url}"

# Interactive mode
mvn release:prepare -P scm-release -DskipTests  \
-Darguments="-DskipTests -DdryRun=false -DdeveloperConnectionUrl=scm:git:${git.repo.url}"

# Non interactive example
mvn release:prepare -P scm-release \
-DdryRun=false \
-DdevelopmentVersion=1.0.5-SNAPSHOT \
-DreleaseVersion=1.0.5 \
-Dtag=v1.0.5 \
-DdeveloperConnectionUrl="scm:git:${git.repo.url}"

```
---

<p align="center">
  <img src="src/site/resources/images/company-logo-min.png" style=" height: 90px; border-bottom:1px none #ebb349; padding:0 10px 6px 10px ;"/>
</p>

License
-------

This project is copyright by **Orbital Zero**, it is free software and may be redistributed under the terms specified in the `LICENSE` file.

The names and logos for this sample code are trademarks of their respective owners, which are in no way associated or affiliated with **Orbital Zero**. Product names and logos are used solely for the purpose to show specific examples of software development, not for commercial use. Use of these names does not imply any co-operation or endorsement.