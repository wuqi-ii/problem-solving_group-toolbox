<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Key, Plus, Promotion, Setting, UserFilled } from '@element-plus/icons-vue'

import {
  cancelInvitation, createGroup, createInvitation, grantPermission, joinGroup, leaveGroup,
  listGroups, listInvitations, listMembers, removeMember, revokePermission, transferLeadership, updateMyGroupNickname,
  type GroupView, type InvitationView, type MemberView, type PermissionType,
} from '@/api/groups'
import { useSessionStore } from '@/stores/session'
import { apiErrorMessage, isDialogDismissal } from '@/api/errors'

const loading = ref(true)
const creating = ref(false)
const dialogVisible = ref(false)
const joinVisible = ref(false)
const invitationVisible = ref(false)
const permissionVisible = ref(false)
const invitations = ref<InvitationView[]>([])
const invitationGroupId = ref('')
const joinCode = ref('')
const permissionTarget = ref<{ groupId: string; member: MemberView } | null>(null)
const selectedPermissions = ref<PermissionType[]>([])
const groups = ref<GroupView[]>([])
const session = useSessionStore()
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

function announceGroupChange() {
  window.dispatchEvent(new Event('groups-changed'))
}

function canManageMembers(group: GroupView) {
  return group.myRole === 'LEADER' || group.myPermissions.includes('MEMBER_MANAGE')
}

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
    announceGroupChange()
  } catch (error: unknown) {
    ElMessage.error(apiErrorMessage(error, '创建失败'))
  } finally { creating.value = false }
}

async function editMyNickname(groupId: string, member: MemberView) {
  try {
    const result = await ElMessageBox.prompt('留空时使用个人昵称。', '修改组内昵称', {
      inputValue: member.groupNickname ?? '',
      inputValidator: (value) => (value ?? '').trim().length <= 30 || '组内昵称最多 30 字',
    })
    await updateMyGroupNickname(groupId, (result.value ?? '').trim())
    members.value[groupId] = await listMembers(groupId)
    ElMessage.success('组内昵称已更新')
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '更新组内昵称失败'))
  }
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

async function showInvitations(groupId: string) {
  invitationGroupId.value = groupId
  invitationVisible.value = true
  try {
    invitations.value = await listInvitations(groupId)
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '邀请码加载失败')) }
}

async function addInvitation() {
  try {
    await createInvitation(invitationGroupId.value)
    invitations.value = await listInvitations(invitationGroupId.value)
    ElMessage.success('新邀请码已生成')
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '无法创建邀请码')) }
}

async function cancelInvite(invitation: InvitationView) {
  try {
    await ElMessageBox.confirm(`取消邀请码 ${invitation.code}？`, '取消邀请', { type: 'warning' })
    await cancelInvitation(invitationGroupId.value, invitation.id)
    invitations.value = await listInvitations(invitationGroupId.value)
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '取消失败'))
  }
}

async function submitJoin() {
  if (!joinCode.value.trim()) return
  try {
    await joinGroup(joinCode.value)
    joinVisible.value = false
    joinCode.value = ''
    ElMessage.success('已加入小组')
    await refresh()
    announceGroupChange()
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '加入失败')) }
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
    await refresh()
    announceGroupChange()
    ElMessage.success('权限已更新')
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '权限更新失败')) }
}

async function remove(groupId: string, member: MemberView) {
  try {
    await ElMessageBox.confirm(`确定将“${member.nickname}”移出小组？其历史提交仍会保留。`, '移除成员', { type: 'warning' })
    await removeMember(groupId, member.userId)
    members.value[groupId] = await listMembers(groupId)
    ElMessage.success('成员已移除')
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '移除失败'))
  }
}

async function transfer(groupId: string, member: MemberView) {
  try {
    await ElMessageBox.confirm(`将组长转让给“${member.nickname}”？转让后你将成为普通成员。`, '转让组长', { type: 'warning', confirmButtonText: '确认转让' })
    await transferLeadership(groupId, member.userId)
    delete members.value[groupId]
    await refresh()
    members.value[groupId] = await listMembers(groupId)
    announceGroupChange()
    ElMessage.success('组长已转让')
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '转让失败'))
  }
}

async function leave(group: GroupView) {
  try {
    await ElMessageBox.confirm(`确定退出“${group.name}”？退出后将无法访问小组内容。`, '退出小组', { type: 'warning' })
    await leaveGroup(group.id)
    delete members.value[group.id]
    await refresh()
    if (session.activeGroupId === group.id) session.switchGroup(groups.value[0]?.id ?? null)
    announceGroupChange()
    ElMessage.success('已退出小组')
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '退出失败'))
  }
}

const invitationStatus: Record<string, string> = { ACTIVE: '有效', EXPIRED: '已过期', EXHAUSTED: '次数用尽', CANCELLED: '已取消' }
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
          <el-button v-if="canManageMembers(group)" size="small" @click.stop="showInvitations(group.id)">邀请管理</el-button>
          <el-button v-else size="small" type="danger" plain @click.stop="leave(group)">退出小组</el-button>
        </div>
      </template>
      <el-table :data="members[group.id] ?? []" empty-text="正在加载成员">
        <el-table-column label="成员">
          <template #default="{ row }">
            <router-link :to="{ name: 'member-profile', params: { groupId: group.id, userId: row.userId } }">{{ row.groupNickname || row.nickname }}</router-link>
            <el-button v-if="row.userId === session.user?.id" text type="primary" @click="editMyNickname(group.id, row)">修改组内昵称</el-button>
          </template>
        </el-table-column>
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
        <el-table-column v-if="canManageMembers(group)" label="操作" width="250">
          <template #default="scope">
            <template v-if="scope.row.role !== 'LEADER'">
              <el-button v-if="group.myRole === 'LEADER'" text type="primary" :icon="Setting" @click="openPermissions(group.id, scope.row)">授权</el-button>
              <el-button v-if="group.myRole === 'LEADER'" text :icon="Promotion" @click="transfer(group.id, scope.row)">转让组长</el-button>
              <el-button v-if="scope.row.userId !== session.user?.id" text type="danger" :icon="Delete" @click="remove(group.id, scope.row)">移除</el-button>
            </template>
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

  <el-dialog v-model="invitationVisible" title="邀请码管理" width="680px">
    <div class="invitation-toolbar"><p class="muted">邀请码默认 72 小时有效，最多使用 20 次。</p><el-button type="primary" :icon="Plus" @click="addInvitation">生成邀请码</el-button></div>
    <el-empty v-if="invitations.length === 0" description="还没有邀请码" />
    <el-table v-else :data="invitations" max-height="360">
      <el-table-column prop="code" label="邀请码" width="140" />
      <el-table-column label="状态" width="95"><template #default="scope"><el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">{{ invitationStatus[scope.row.status] ?? scope.row.status }}</el-tag></template></el-table-column>
      <el-table-column label="使用次数" width="100"><template #default="scope">{{ scope.row.usedCount }}/{{ scope.row.maxUses }}</template></el-table-column>
      <el-table-column label="到期时间" min-width="175"><template #default="scope">{{ new Date(scope.row.expiresAt).toLocaleString('zh-CN', { hour12: false }) }}</template></el-table-column>
      <el-table-column label="操作" width="90"><template #default="scope"><el-button v-if="scope.row.status === 'ACTIVE'" text type="danger" @click="cancelInvite(scope.row)">取消</el-button></template></el-table-column>
    </el-table>
    <template #footer><el-button @click="invitationVisible = false">关闭</el-button></template>
  </el-dialog>

  <el-dialog v-model="permissionVisible" title="设置成员权限" width="460px">
    <el-checkbox-group v-model="selectedPermissions" class="permission-options">
      <el-checkbox v-for="option in permissionOptions" :key="option.value" :value="option.value">{{ option.label }}</el-checkbox>
    </el-checkbox-group>
    <template #footer><el-button @click="permissionVisible = false">取消</el-button><el-button type="primary" @click="savePermissions">保存权限</el-button></template>
  </el-dialog>
</template>
