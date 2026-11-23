param(
    [int]$DebugPort = 9224,
    [string]$OutputDirectory = $PSScriptRoot
)

$ErrorActionPreference = 'Stop'
$tab = Invoke-RestMethod "http://127.0.0.1:$DebugPort/json/list" |
    Where-Object { $_.type -eq 'page' -and $_.url -eq 'about:blank' } |
    Select-Object -First 1
if (-not $tab) { throw '未找到可用的无头浏览器页面。' }

$socket = [System.Net.WebSockets.ClientWebSocket]::new()
$socket.ConnectAsync([Uri]$tab.webSocketDebuggerUrl, [Threading.CancellationToken]::None).GetAwaiter().GetResult()
$script:messageId = 0

function Send-CdpCommand {
    param([string]$Method, [hashtable]$Parameters = @{})
    $script:messageId++
    $id = $script:messageId
    $json = @{ id = $id; method = $Method; params = $Parameters } | ConvertTo-Json -Depth 20 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $socket.SendAsync(
        [ArraySegment[byte]]::new($bytes),
        [System.Net.WebSockets.WebSocketMessageType]::Text,
        $true,
        [Threading.CancellationToken]::None
    ).GetAwaiter().GetResult() | Out-Null

    while ($true) {
        $stream = [IO.MemoryStream]::new()
        do {
            $buffer = New-Object byte[] 16384
            $result = $socket.ReceiveAsync(
                [ArraySegment[byte]]::new($buffer),
                [Threading.CancellationToken]::None
            ).GetAwaiter().GetResult()
            $stream.Write($buffer, 0, $result.Count)
        } until ($result.EndOfMessage)
        $message = [Text.Encoding]::UTF8.GetString($stream.ToArray()) | ConvertFrom-Json
        if ($message.id -eq $id) {
            if ($message.error) { throw ($message.error | ConvertTo-Json -Compress) }
            return $message.result
        }
    }
}

function Invoke-JavaScript {
    param([string]$Expression, [switch]$AwaitPromise)
    $result = Send-CdpCommand 'Runtime.evaluate' @{
        expression = $Expression
        awaitPromise = [bool]$AwaitPromise
        returnByValue = $true
    }
    return $result.result.value
}

function Open-TestPage {
    param([string]$Path)
    Send-CdpCommand 'Page.navigate' @{ url = "http://127.0.0.1:8088$Path" } | Out-Null
    Start-Sleep -Milliseconds 1800
}

function Save-TestEvidence {
    param([string]$Name)
    $metrics = Invoke-JavaScript @"
({
  title: document.title,
  clientWidth: document.documentElement.clientWidth,
  scrollWidth: document.documentElement.scrollWidth,
  clientHeight: document.documentElement.clientHeight,
  bodyText: document.body.innerText.slice(0, 3000)
})
"@
    $capture = Send-CdpCommand 'Page.captureScreenshot' @{ format = 'png'; fromSurface = $true }
    [IO.File]::WriteAllBytes((Join-Path $OutputDirectory "$Name.png"), [Convert]::FromBase64String($capture.data))
    return $metrics
}

try {
    Send-CdpCommand 'Page.enable' | Out-Null
    Send-CdpCommand 'Runtime.enable' | Out-Null
    Send-CdpCommand 'Emulation.setDeviceMetricsOverride' @{
        width = 390
        height = 844
        deviceScaleFactor = 1
        mobile = $true
        screenWidth = 390
        screenHeight = 844
    } | Out-Null

    Open-TestPage '/login'
    $loginResult = Invoke-JavaScript @"
(async () => {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({account: 'demo_leader_0928185730', password: 'DemoPass123!'})
  });
  const payload = await response.json();
  if (!response.ok || !payload.data) return {ok: false, status: response.status, payload};
  sessionStorage.setItem('team-toolbox-token', payload.data.accessToken);
  sessionStorage.setItem('team-toolbox-user', JSON.stringify(payload.data.user));
  sessionStorage.setItem('team-toolbox-active-group', '5c064d38-8f1b-4760-96d4-49fe5b2e4e6e');
  return {ok: true, account: payload.data.user.account};
})()
"@ -AwaitPromise
    if (-not $loginResult.ok) { throw "自动登录失败：$($loginResult | ConvertTo-Json -Compress)" }

    Open-TestPage '/tasks'
    $tasks = Save-TestEvidence 'mobile-tasks-390'

    Open-TestPage '/files'
    $filesBefore = Save-TestEvidence 'mobile-files-390'
    $previewClick = Invoke-JavaScript "(() => { const button = [...document.querySelectorAll('button')].find(item => item.textContent?.includes('预览')); if (!button) return false; button.click(); return true; })()"
    Start-Sleep -Milliseconds 1200
    $filesPreview = Save-TestEvidence 'mobile-file-preview-390'

    Open-TestPage '/a-route-that-does-not-exist'
    $notFound = Save-TestEvidence 'mobile-404-390'

    $summary = [pscustomobject]@{
        login = $loginResult
        tasks = $tasks
        files = $filesBefore
        previewButtonClicked = $previewClick
        filePreview = $filesPreview
        notFound = $notFound
    }
    $summaryJson = $summary | ConvertTo-Json -Depth 8
    [IO.File]::WriteAllText((Join-Path $OutputDirectory 'ui-retest-results.json'), $summaryJson, [Text.Encoding]::UTF8)
    $summaryJson
}
finally {
    if ($socket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
        $socket.CloseAsync(
            [System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure,
            'test complete',
            [Threading.CancellationToken]::None
        ).GetAwaiter().GetResult() | Out-Null
    }
    $socket.Dispose()
}
