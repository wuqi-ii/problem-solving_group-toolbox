param(
    [string]$BaseUrl = "http://localhost:8088/api/v1",
    [string]$DemoPassword = "DemoPass123!"
)

$ErrorActionPreference = "Stop"
$api = $BaseUrl.TrimEnd('/')
$suffix = Get-Date -Format "MMddHHmmss"
$leaderAccount = "demo_leader_$suffix"
$memberAccount = "demo_member_$suffix"

function Invoke-ApiJson([string]$Method, [string]$Path, $Body, [string]$Token = "") {
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $parameters = @{ Method = $Method; Uri = "$api$Path"; Headers = $headers }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json"
        $parameters.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }
    return (Invoke-RestMethod @parameters).data
}

$leader = Invoke-ApiJson POST "/auth/register" @{ account = $leaderAccount; password = $DemoPassword; nickname = "演示组长" }
$member = Invoke-ApiJson POST "/auth/register" @{ account = $memberAccount; password = $DemoPassword; nickname = "演示组员" }
$leaderToken = $leader.accessToken
$memberToken = $member.accessToken
$group = Invoke-ApiJson POST "/groups" @{ name = "课程演示小组-$suffix"; description = "自动生成的课程答辩演示数据" } $leaderToken
$invitation = Invoke-ApiJson POST "/groups/$($group.id)/invitations" @{ validHours = 24; maxUses = 5 } $leaderToken
Invoke-ApiJson POST "/groups/join" @{ code = $invitation.code } $memberToken | Out-Null
Invoke-ApiJson POST "/groups/$($group.id)/announcements" @{ content = "欢迎进入课程项目演示小组" } $leaderToken | Out-Null
Invoke-ApiJson POST "/groups/$($group.id)/discussions" @{ content = "请在演示前完成需求原型检查" } $memberToken | Out-Null

$task = Invoke-ApiJson POST "/groups/$($group.id)/tasks" @{
    title = "完成课程项目演示原型"
    description = "提交说明文件并由组长审核"
    priority = "HIGH"
    dueAt = (Get-Date).AddDays(2).ToUniversalTime().ToString("o")
    assigneeIds = @($member.user.id)
} $leaderToken

$headers = @{ Authorization = "Bearer $memberToken" }
$tempFile = Join-Path ([IO.Path]::GetTempPath()) "team-toolbox-demo-$suffix.txt"
try {
    Set-Content -LiteralPath $tempFile -Value "小组协作工具箱课程演示成果 $suffix" -Encoding utf8
    $upload = (Invoke-RestMethod -Method Post -Uri "$api/groups/$($group.id)/files" -Headers $headers -Form @{ file = Get-Item -LiteralPath $tempFile }).data
    Invoke-ApiJson POST "/tasks/$($task.id)/start" $null $memberToken | Out-Null
    Invoke-ApiJson POST "/tasks/$($task.id)/submissions" @{ content = "演示版本已经完成"; attachmentFileId = $upload.id } $memberToken | Out-Null
    Invoke-ApiJson POST "/tasks/$($task.id)/reviews" @{ decision = "APPROVED"; comment = "演示数据验收通过" } $leaderToken | Out-Null
} finally {
    if (Test-Path -LiteralPath $tempFile) { Remove-Item -LiteralPath $tempFile -Force }
}

Write-Host "Demo data created."
Write-Host "Leader: $leaderAccount / $DemoPassword"
Write-Host "Member: $memberAccount / $DemoPassword"
Write-Host "Group: $($group.name)"
