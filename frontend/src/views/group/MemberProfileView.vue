<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { listMembers, type MemberView } from '@/api/groups'
import { apiErrorMessage } from '@/api/errors'

const route = useRoute()
const member = ref<MemberView>()
const loading = ref(true)
const errorMessage = ref('')

watch(() => [String(route.params.groupId), String(route.params.userId)] as const,
  async ([groupId, userId], _, onCleanup) => {
    let current = true
    onCleanup(() => { current = false })
    member.value = undefined
    errorMessage.value = ''
    loading.value = true
    try {
      const roster = await listMembers(groupId)
      if (current) member.value = roster.find(item => item.userId === userId)
    } catch (error: unknown) {
      if (current) errorMessage.value = apiErrorMessage(error, '暂时无法查看成员主页')
    } finally {
      if (current) loading.value = false
    }
  }, { immediate: true })
</script>

<template>
  <section class="page-heading">
    <div><h2>成员主页</h2></div>
    <el-button @click="$router.push('/groups')">返回小组与成员</el-button>
  </section>
  <el-skeleton v-if="loading" :rows="4" animated />
  <el-descriptions v-else-if="member" :column="1" border>
    <el-descriptions-item label="个人昵称">{{ member.nickname }}</el-descriptions-item>
    <el-descriptions-item label="组内昵称">{{ member.groupNickname || '使用个人昵称' }}</el-descriptions-item>
    <el-descriptions-item label="账号">{{ member.account }}</el-descriptions-item>
    <el-descriptions-item label="小组角色">{{ member.role === 'LEADER' ? '组长' : '成员' }}</el-descriptions-item>
    <el-descriptions-item label="加入时间">{{ new Date(member.joinedAt).toLocaleDateString('zh-CN') }}</el-descriptions-item>
  </el-descriptions>
  <el-empty v-else :description="errorMessage || '该成员已离组或不存在'" />
</template>
