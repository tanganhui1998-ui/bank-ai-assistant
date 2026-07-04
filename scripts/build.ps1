param(
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

# Prefer pinned local runtimes so Spring Boot 3 is not compiled with Java 8 / old Maven.
$jdkHome = "D:\software\Java\jdk17"
$mavenBin = "D:\software\apache-maven-3.6.3\bin"

if (Test-Path $jdkHome) {
    $env:JAVA_HOME = $jdkHome
    $env:Path = "$jdkHome\bin;$env:Path"
}

if (Test-Path $mavenBin) {
    $env:Path = "$mavenBin;$env:Path"
}

$arguments = @("clean")
if ($SkipTests) {
    $arguments += "-DskipTests"
    $arguments += "compile"
} else {
    $arguments += "test"
}

Write-Host "JAVA_HOME=$env:JAVA_HOME"
java -version
mvn -version
& mvn @arguments
