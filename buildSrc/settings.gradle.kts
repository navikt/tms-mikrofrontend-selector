/*
Fixes annoying gradle idea plugin bug. The idea gradle plugin expects unique root project name for buildSrc in gradle composite builds
https://github.com/gradle/gradle/issues/8920
 */

rootProject.name = rootProject.projectDir.parentFile.name + "-buildSrc"
