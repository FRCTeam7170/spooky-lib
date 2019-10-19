plugins {
    java
}

group = "frc.team7170"
version = "0.1.1"

java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    // WPI repo
    maven {
        setUrl("http://first.wpi.edu/FRC/roborio/maven/release/")
    }
    // CTRE repo
    maven {
        setUrl("http://devsite.ctr-electronics.com/maven/release/")
    }
    // Local SparkMAX repo
    maven {
        // Use uri to resolve relative path.
        setUrl(uri("lib/sparkmax/maven"))
    }
}

dependencies {
    testImplementation(group="org.junit.jupiter", name="junit-jupiter-api", version="5.5.2")
    testImplementation(group="org.hamcrest", name="hamcrest", version="2.2")
    implementation(group="edu.wpi.first.wpilibj", name="wpilibj-java", version="2019.4.1")
    implementation(group="edu.wpi.first.ntcore", name="ntcore-java", version="2019.4.1")
    implementation(group="org.msgpack", name="msgpack-core", version="0.8.16")
    implementation(group="com.ctre.phoenix", name="api-java", version="5.14.1")
    implementation(group="com.revrobotics.frc", name="SparkMax-java", version="1.4.1")
}
