repositories {
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents)
}
