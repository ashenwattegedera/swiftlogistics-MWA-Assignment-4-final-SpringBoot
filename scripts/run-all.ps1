# Starts all four services in separate console windows (for the live demo).
# The middleware runs an embedded AMQP broker by default, so RabbitMQ/Docker is NOT required.
$root = Split-Path -Parent $PSScriptRoot

$services = @(
    @{ name = "WMS";        jar = "$root\wms-service\target\wms-service-1.0.0-SNAPSHOT.jar" },
    @{ name = "ROS";        jar = "$root\ros-service\target\ros-service-1.0.0-SNAPSHOT.jar" },
    @{ name = "CMS";        jar = "$root\cms-service\target\cms-service-1.0.0-SNAPSHOT.jar" },
    @{ name = "Middleware"; jar = "$root\swifttrack-middleware\target\swifttrack-middleware-1.0.0-SNAPSHOT.jar" }
)

foreach ($s in $services) {
    if (-not (Test-Path -LiteralPath $s.jar)) {
        Write-Warning "Jar not found: $($s.jar) - run 'mvn clean install' first."
        continue
    }
    Start-Process -FilePath "java" -ArgumentList @("-jar", "`"$($s.jar)`"")
    Write-Host "Started $($s.name)"
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "Portal:   http://localhost:8080"
Write-Host "Driver:   http://localhost:8080/driver.html"
Write-Host "CMS WSDL: http://localhost:8081/ws/cms.wsdl"
