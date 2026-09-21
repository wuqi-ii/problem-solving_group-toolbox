<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Key, Plus, Setting, UserFilled } from '@element-plus/icons-vue'

import {
  createGroup, createInvitation, grantPermission, joinGroup, listGroups, listMembers, revokePermission,
  type GroupView, type MemberView, type PermissionType,
} from '@/api/groups'

const loading = ref(true)
const creating = ref(false)
const dialogVisible = ref(false)
const joinVisible = ref(false)
const invitationVisible = ref(false)
const permissionVisible = ref(false)
const invitationCode = ref('')
const joinCode = ref('')
const permissionTarget = ref<{ groupId: string; member: MemberView } | null>(null)
const selectedPermissions = ref<PermissionType[]>([])
const groups = ref<GroupView[]>([])
const members = ref<Record<string, MemberView[]>>({})
const expanded = ref<string[]>([])
const form = reactive({ name: '', description: '' })
const permissionOptions: { value: PermissionType; label: string }[] = [
  { value: 'MEMBER_MANAGE', label: '成员管理' },
  { value: 'TASK_CREATE', label: '发布任务' },
  { value: 'TASK_REVIEW', label: '审核成果' },
  { value: 'FILE_MANAGE', label: '管理文件' },
  { value: 'GROUP_ANNOUNCE', label: '发布公告' },
]

async function refresh() {
  loading.value = true
  try { groups.value = await listGroups() }
  catch { ElMessage.error('小组列表加载失败') }
  finally { loading.value = false }
}

async function submitGroup() {
  if (!form.name.trim()) return ElMessage.warning('请输入小组名称')
  creating.value = true
  try {
    await createGroup(form.name, form.description)
    Object.assign(form, { name: '', description: '' })
    dialogVisible.value = false
    ElMessage.success('小组创建成功')
    await refresh()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message ?? '创建失败')
  } finally { creating.value = false }
}

async function handleExpand(opened: string | string[]) {
  const groupIds = Array.isArray(opened) ? opened : [opened]
  expanded.value = groupIds
  for (const groupId of groupIds) {
    if (members.value[groupId]) continue
    try { members.value[groupId] = await listMembers(groupId) }
    catch { ElMessage.error('成员列表加载失败') }
  }
}

onMounted(refresh)

async function showInvitation(groupId: string) {
  try {
    invitationCode.value = (await createInvitation(groupId)).code
    invitationVisible.value = true
  } catch (error: any) { ElMessage.error(error.response?.data?.message ?? '无法创建邀请码') }
}

async function submitJoin() {
  if (!joinCode.value.trim()) return
  try {
    await joinGroup(joinCode.value)
    joinVisible.value = false
    joinCode.value = ''
    ElMessage.success('已加入小组')
    await refresh()
  } catch (error: any) { ElMessage.error(error.response?.data?.message ?? '加入失败') }
}

function openPermissions(groupId: string, member: MemberView) {
  permissionTarget.value = { groupId, member }
  selectedPermissions.value = [...member.permissions] as PermissionType[]
  permissionVisible.value = true
}

async function savePermissions() {
  if (!permissionTarget.value) return
  const { groupId, member } = permissionTarget.value
  const previous = member.permissions as PermissionType[]
  try {
    await Promise.all([
      ...selectedPermissions.value.filter((item) => !previous.includes(item)).map((item) => grantPermission(groupId, member.userId, item)),
      ...previous.filter((item) => !selectedPermissions.value.includes(item)).map((item) => revokePermission(groupId, member.userId, item)),
    ])
    members.value[groupId] = await listMembers(groupId)
    permissionVisible.value = false
    ElMessage.success('权限已更新')
  } catch (error: any) { ElMessage.error(error.response?.data?.message ?? '权限更新失败') }
}
</script>

<template>
  <section class="page-heading">
    <div><span class="eyebrow">GROUP MANAGEMENT</span><h2>小组与成员</h2><p>创建协作空间，并在这里统一管理成员和权限。</p></div>
    <div class="heading-actions">
      <el-button :icon="Key" @click="joinVisible = true">使用邀请码</el-button>
      <el-button type="primary" :icon="Plus" @click="dialogVisible = true">创建小组</el-button>
    </div>
  </section>

  <el-skeleton v-if="loading" :rows="4" animated />
  <el-empty v-else-if="groups.length === 0" description="还没有加入任何小组">
    <el-button type="primary" @click="dialogVisible = true">创建第一个小组</el-button>
  </el-empty>
  <el-collapse v-else v-model="expanded" class="group-list" @change="handleExpand">
    <el-collapse-item v-for="group in groups" :key="group.id" :name="group.id">
      <template #title>
        <div class="group-title">
          <div class="group-avatar"><el-icon><UserFilled /></el-icon></div>
          <div><strong>{{ group.name }}</strong><span>{{ group.description || '暂无简介' }}</span></div>
          <el-tag :type="group.myRole === 'LEADER' ? 'success' : 'info'" effect="light">
            {{ group.myRole === 'LEADER' ? '组长' : '成员' }}
          </el-tag>
          <el-button v-if="group.myRole === 'LEADER'" size="small" @click.stop="showInvitation(group.id)">邀请</el-button>
        </div>
      </template>
      <el-table :data="members[group.id] ?? []" empty-text="正在加载成员">
        <el-table-column prop="nickname" label="成员" />
        <el-table-column prop="account" label="账号" />
        <el-table-column label="角色" width="120">
          <template #default="scope">{{ scope.row.role === 'LEADER' ? '组长' : '成员' }}</template>
        </el-table-column>
        <el-table-column label="权限" min-width="220">
          <template #default="scope">
            <span v-if="scope.row.role === 'LEADER'">全部权限</span>
            <template v-else>
              <el-tag v-for="permission in scope.row.permissions" :key="permission" size="small" class="permission-tag">{{ permission }}</el-tag>
              <span v-if="scope.row.permissions.length === 0" class="muted">基础成员</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column v-if="group.myRole === 'LEADER'" label="操作" width="100">
          <template #default="scope">
            <el-button v-if="scope.row.role !== 'LEADER'" text type="primary" :icon="Setting" @click="openPermissions(group.id, scope.row)">授权</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-collapse-item>
  </el-collapse>

  <el-dialog v-model="dialogVisible" title="创建小组" width="480px">
    <el-form label-position="top">
      <el-form-item label="小组名称"><el-input v-model="form.name" maxlength="50" show-word-limit /></el-form-item>
      <el-form-item label="小组简介"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="submitGroup">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="joinVisible" title="使用邀请码加入" width="420px">
    <el-input v-model="joinCode" maxlength="16" placeholder="输入 8 位邀请码" @keyup.enter="submitJoin" />
    <template #footer><el-button @click="joinVisible = false">取消</el-button><el-button type="primary" @click="submitJoin">加入</el-button></template>
  </el-dialog>

  <el-dialog v-model="invitationVisible" title="邀请成员" width="420px">
    <p class="muted">将邀请码发送给组员，邀请码 72 小时内有效。</p>
    <div class="invitation-code">{{ invitationCode }}</div>
    <template #footer><el-button type="primary" @click="invitationVisible = false">完成</el-button></template>
  </el-dialog>

  <el-dialog v-model="permissionVisible" title="设置成员权限" width="460px">
    <el-checkbox-group v-model="selectedPermissions" class="permission-options">
      <el-checkbox v-for="option in permissionOptions" :key="option.value" :value="option.value">{{ option.label }}</el-checkbox>
    </el-checkbox-group>
    <template #footer><el-button @click="permissionVisible = false">取消</el-button><el-button type="primary" @click="savePermissions">保存权限</el-button></template>
  </el-dialog>
</template>
