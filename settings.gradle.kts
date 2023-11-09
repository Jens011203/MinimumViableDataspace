rootProject.name = "mvd"


pluginManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {

    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        mavenLocal()
    }
}


//include(":launchers:connector")

include(":extensions:httpplane")
include(":launchers:rest-connector")
include(":launchers:registrationservice")
include(":system-tests")
include(":extensions:refresh-catalog")
include(":extensions:policies")
include(":extensions:DataPlaneFramework")
include(":extensions:KafkaPlane")
//include(":extensions:provider")
//include(":launchers:consumer")
include(":rest_api")
include("consumer_rest_api")
include(":http_request_logger")
include("extensions:DataPlaneFramework")
findProject(":extensions:DataPlaneFramework")?.name = "DataPlaneFramework"
