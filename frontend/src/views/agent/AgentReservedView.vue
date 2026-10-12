<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Lock, MagicStick } from '@element-plus/icons-vue'
import { getAgentCapabilities, type AgentCapabilitiesView } from '@/api/agent'

const capabilities = ref<AgentCapabilitiesView>()
onMounted(async () => { capabilities.value = await getAgentCapabilities().catch(() => undefined) })
</script>

<template>
  <section class="page-heading"><div><span class="eyebrow">AI AGENT · RESERVED</span><h2>AI 协作助手</h2><p>接口和扩展位置已经预留，完成基础系统后再接入 DeepSeek API。</p></div><el-tag type="warning" effect="light">最后阶段接入</el-tag></section>
  <div class="agent-reserved-grid">
    <el-card class="tool-panel agent-intro"><el-icon size="32"><MagicStick /></el-icon><h3>当前不会调用任何模型</h3><p>AI 总开关保持关闭，没有配置 API 密钥，也不会将任务、聊天或文件发送给第三方。未来 Agent 只能通过经过权限校验的工具调用现有业务服务。</p><el-alert title="预留供应商：DeepSeek；业务层通过统一适配接口调用，可替换模型实现。" type="info" :closable="false" /></el-card>
    <el-card class="tool-panel"><template #header><strong>已预留的工具契约</strong></template><div v-for="tool in capabilities?.plannedTools ?? []" :key="tool.name" class="agent-tool-row"><div><strong>{{ tool.description }}</strong><code>{{ tool.name }}</code></div><div><el-tag size="small">{{ tool.riskLevel }}</el-tag><el-icon v-if="tool.confirmationRequired"><Lock /></el-icon></div></div></el-card>
  </div>
</template>
