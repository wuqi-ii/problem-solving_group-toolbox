<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAuditLogs, type AuditLogView } from '@/api/audit'

const loading = ref(false)
const logs = ref<AuditLogView[]>([])

function operationName(item: AuditLogView) {
  const map: Record<string, string> = { POST: '新增/执行', PATCH: '修改', PUT: '更新', DELETE: '删除' }
  return map[item.method] ?? item.method
}

function targetName(path: string) {
  const rules: Array<[RegExp, string]> = [
    [/\/auth\//, '账号会话'], [/\/invitations/, '小组邀请码'], [/\/permissions/, '成员权限'],
    [/\/members/, '小组成员'], [/\/task-results/, '任务成果归档'], [/\/tasks\/.+\/reviews/, '任务审核'],
    [/\/tasks\/.+\/submissions/, '任务成果提交'], [/\/tasks/, '任务'], [/\/files/, '共享资料'],
    [/\/transfers|\/pickup/, '临时文件中转'], [/\/device-items/, '跨设备内容'],
    [/\/announcements/, '公告'], [/\/discussions|\/comments/, '讨论与留言'], [/\/notifications/, '通知'],
    [/\/groups/, '小组'],
  ]
  return rules.find(([pattern]) => pattern.test(path))?.[1] ?? '系统操作'
}

function displayIp(ip: string) {
  return /^(172\.|10\.|192\.168\.)/.test(ip) ? `${ip}（代理/内网）` : ip
}

async function load() {
  loading.value = true
  try { logs.value = await listAuditLogs() }
  catch { ElMessage.error('操作记录加载失败') }
  finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div><span class="eyebrow">安全与追溯</span><h2>我的操作记录</h2><p>展示最近 100 条写操作，便于核对重要变更。</p></div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>
    <el-card shadow="never">
      <el-table v-loading="loading" :data="logs" empty-text="暂无操作记录">
        <el-table-column label="时间" min-width="180">
          <template #default="scope">{{ new Date(scope.row.occurredAt).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="scope"><el-tag effect="plain">{{ operationName(scope.row) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="业务对象" min-width="180">
          <template #default="scope"><span :title="scope.row.path">{{ targetName(scope.row.path) }}</span></template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="scope"><el-tag :type="scope.row.responseStatus < 400 ? 'success' : 'danger'">{{ scope.row.responseStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="来源 IP" min-width="190"><template #default="scope">{{ displayIp(scope.row.clientIp) }}</template></el-table-column>
      </el-table>
    </el-card>
  </section>
</template>
