# Smoke test: starts all four services, submits an order, waits for the saga to reach
# ROUTE_ASSIGNED, then has the driver mark it delivered. Verifies the full integration
# against the REAL CMS (SOAP), ROS (REST) and WMS (TCP) services.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

$jars = @{
    cms = "$root\cms-service\target\cms-service-1.0.0-SNAPSHOT.jar"
    ros = "$root\ros-service\target\ros-service-1.0.0-SNAPSHOT.jar"
    wms = "$root\wms-service\target\wms-service-1.0.0-SNAPSHOT.jar"
    mw  = "$root\swifttrack-middleware\target\swifttrack-middleware-1.0.0-SNAPSHOT.jar"
}

$procs = @()
$logdir = Join-Path $env:TEMP "swifttrack-logs"

function Start-Svc($name, $jar) {
    $out = "$logdir\$name.out.log"
    $err = "$logdir\$name.err.log"
    $p = Start-Process -FilePath "java" -ArgumentList @("-jar", "`"$jar`"") `
        -RedirectStandardOutput $out -RedirectStandardError $err -PassThru -WindowStyle Hidden
    $procs += $p
    Write-Host "Started $name (PID $($p.Id))"
}

function Wait-Tcp($port, $name) {
    for ($i = 0; $i -lt 80; $i++) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $result = $client.BeginConnect("127.0.0.1", $port, $null, $null)
            $ok = $result.AsyncWaitHandle.WaitOne(1000)
            if ($ok -and $client.Connected) { $client.Close(); Write-Host "$name ready (port $port)"; return }
        } catch { }
        finally { $client.Close() }
        Start-Sleep -Milliseconds 500
    }
    throw "$name did not start on port $port"
}

try {
    New-Item -ItemType Directory -Force -Path $logdir | Out-Null

    Start-Svc "wms" $jars.wms
    Start-Svc "ros" $jars.ros
    Start-Svc "cms" $jars.cms
    Start-Sleep -Seconds 5
    Start-Svc "middleware" $jars.mw

    Wait-Tcp 9090 "WMS"
    Wait-Tcp 8082 "ROS"
    Wait-Tcp 8081 "CMS"
    Wait-Tcp 8080 "Middleware"
    Start-Sleep -Seconds 3

    $body = @{
        clientId        = "C-100"
        clientReference = "REF-SMOKE"
        recipient       = @{ name = "Anura Silva"; phone = "0771234567" }
        address         = @{ street = "42 Galle Road"; city = "Colombo 03"; postalCode = "00300"; latitude = 6.9170; longitude = 79.8500 }
        items           = @(@{ sku = "SKU-1"; description = "Ceylon Tea"; quantity = 2 })
    } | ConvertTo-Json -Depth 5

    $ack = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -ContentType "application/json" -Body $body
    Write-Host "Order accepted: $($ack.orderId)"

    $order = $null
    for ($i = 0; $i -lt 60; $i++) {
        $order = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($ack.orderId)"
        Write-Host "  status: $($order.status)"
        if ($order.status -eq "ROUTE_ASSIGNED") { break }
        Start-Sleep -Seconds 1
    }
    if ($order.status -ne "ROUTE_ASSIGNED") { throw "Order did not reach ROUTE_ASSIGNED (last $($order.status))" }

    $null = Invoke-RestMethod -Uri "http://localhost:8080/api/deliveries/$($ack.orderId)/deliver" -Method Post
    Start-Sleep -Seconds 1
    $delivered = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($ack.orderId)"
    Write-Host "Final status: $($delivered.status)"
    if ($delivered.status -ne "DELIVERED") { throw "Expected DELIVERED, got $($delivered.status)" }

    Write-Host "SMOKE TEST PASSED"
}
finally {
    foreach ($p in $procs) { try { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue } catch { } }
}
