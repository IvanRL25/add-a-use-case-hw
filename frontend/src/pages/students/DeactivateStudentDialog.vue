<template>
  <el-dialog :model-value="visible" title="Deactivate student" width="460px" @update:model-value="emit('cancel')">
    <p>
      Deactivating {{ student?.firstName }} {{ student?.lastName }} immediately revokes access to protected
      resources. The student's account, weekly activity reports, and peer evaluations are retained.
    </p>
    <template #footer>
      <el-button @click="emit('cancel')">Cancel</el-button>
      <el-button type="danger" @click="confirm">Deactivate</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { deactivateStudent } from '@/apis/student'
import type { Student } from '@/apis/student/types'

const props = defineProps<{ visible: boolean; student?: Student }>()
const emit = defineEmits<{ cancel: []; deactivated: [] }>()

async function confirm() {
  if (!props.student?.id) return
  await deactivateStudent(props.student.id)
  ElMessage.success('Student deactivated successfully')
  emit('deactivated')
}
</script>
