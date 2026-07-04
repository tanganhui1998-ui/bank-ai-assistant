param(
    [string]$Profile = "local"
)

$ErrorActionPreference = "Stop"

# Reuse build.ps1 so local startup gets the same Java/Maven selection.
.\scripts\build.ps1 -SkipTests

$env:SPRING_PROFILES_ACTIVE = $Profile
& mvn spring-boot:run
