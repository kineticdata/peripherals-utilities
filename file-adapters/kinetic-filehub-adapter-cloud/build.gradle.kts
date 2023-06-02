import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("net.nemerosa.versioning") version "2.14.0"
  `java-library`
  `maven-publish`
}

repositories {
  mavenLocal()
  maven {
    url = uri("https://s3.amazonaws.com/maven-repo-public-kineticdata.com/releases")
  }

  maven {
    url = uri("s3://maven-repo-private-kineticdata.com/releases")
    authentication {
      create<AwsImAuthentication>("awsIm")
    }

  }

  maven {
    url = uri("https://s3.amazonaws.com/maven-repo-public-kineticdata.com/snapshots")
  }

  maven {
    url = uri("s3://maven-repo-private-kineticdata.com/snapshots")
    authentication {
      create<AwsImAuthentication>("awsIm")
    }
  }

  maven {
    url = uri("https://repo.maven.apache.org/maven2/")
  }

  maven {
    url = uri("https://repo.springsource.org/release/")
  }

  maven {
    url = uri("https://build.shibboleth.net/nexus/content/repositories/releases/")
  }

}

dependencies {
  api("com.kineticdata.filehub:kinetic-filehub-adapter:1.1.0")
  api("com.google.guava:guava:18.0")
  api("commons-io:commons-io:2.1")
  api("org.apache.jclouds:jclouds-all:2.3.0") {
    exclude(group="log4j", module="log4j")
    exclude(group="org.bouncycastle", module="bcprov-ext-jdk15on")
    exclude(group="org.yaml", module="snakeyaml")
  }
  implementation("org.bouncycastle:bcprov-ext-jdk15on:1.61")
  implementation("org.yaml:snakeyaml:1.31")
  api("org.slf4j:slf4j-api:2.0.7")
  api("org.slf4j:slf4j-reload4j:2.0.7")
  testImplementation("junit:junit:4.10")
}

group = "com.kineticdata.filehub.adapters.cloud"
version = "1.0.3"
description = "kinetic-filehub-adapter-cloud"
java.sourceCompatibility = JavaVersion.VERSION_1_8

publishing {
  publications.create<MavenPublication>("maven") {
    from(components["java"])
  }
  repositories {
    maven {
      val releasesUrl = uri("s3://maven-repo-private-kineticdata.com/releases")
      val snapshotsUrl = uri("s3://maven-repo-private-kineticdata.com/snapshots")
      url = if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl
      authentication {
        create<AwsImAuthentication>("awsIm")
      }
    }
  }
}

tasks.withType<JavaCompile>() {
  options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
  options.encoding = "UTF-8"
}
versioning {
  gitRepoRootDir = "../../"
}
tasks.processResources {
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
  val currentDate = SimpleDateFormat("yyyy-MM-dd").format(Date())
  from("src/main/resources"){
    filesMatching("**/*.version") {    
      expand(    
        "buildNumber" to versioning.info.build,
        "buildDate" to currentDate,    
        "timestamp" to System.currentTimeMillis(),    
        "version" to project.version    
      )    
    }
  }
}
