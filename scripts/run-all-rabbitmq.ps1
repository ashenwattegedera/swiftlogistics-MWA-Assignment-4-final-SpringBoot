# Starts all four services in separate console windows, running the middleware against the
# real RabbitMQ container (docker compose up -d rabbitmq) via the "rabbitmq" profile.
# Broker credentials are in swifttrack-middleware/src/main/resources/application-rabbitmq.yml.
$root = Split-Path -Parent $PSScriptRoot

$services = @(
    @{ name = "WMS";        jar = "$root\wms-service\target\wms-service-1.0.0-SNAPSHOT.jar";             args = @() },
    @{ name = "ROS";        jar = "$root\ros-service\target\ros-service-1.0.0-SNAPSHOT.jar";             args = @() },
    @{ name = "CMS";        jar = "$root\cms-service\target\cms-service-1.0.0-SNAPSHOT.jar";             args = @() },
    @{ name = "Middleware"; jar = "$root\swifttrack-middleware\target\swifttrack-middleware-1.0.0-SNAPSHOT.jar"; args = @("--spring.profiles.active=rabbitmq") }
)

foreach ($s in $services) {
    if (-not (Test-Path -LiteralPath $s.jar)) {
        Write-Warning "Jar not found: $($s.jar) - run 'mvn clean install' first."
        continue
    }
    $javaArgs = @("-jar", "`"$($s.jar)`"") + $s.args
    Start-Process -FilePath "java" -ArgumentList $javaArgs
    Write-Host "Started $($s.name)"
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "Portal:   http://localhost:8080"
Write-Host "Driver:   http://localhost:8080/driver.html"
Write-Host "CMS WSDL: http://localhost:8081/ws/cms.wsdl"
