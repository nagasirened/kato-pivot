<template>
  <el-dialog
    :model-value="modelValue"
    :title="data?.id ? '编辑敏感词' : '新增敏感词'"
    width="500px"
    @update:model-value="$emit('update:modelValue', $event)"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="敏感词" prop="word">
        <el-input v-model="form.word" placeholder="请输入敏感词" />
      </el-form-item>
      <el-form-item label="等级" prop="level">
        <el-select v-model="form.level" placeholder="请选择等级" style="width: 100%">
          <el-option label="紧急" value="URGENT" />
          <el-option label="中等" value="MEDIUM" />
          <el-option label="普通" value="NORMAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
          <el-option label="政治敏感" value="POLITICAL" />
          <el-option label="暴力恐怖" value="VIOLENCE" />
          <el-option label="色情低俗" value="PORN" />
          <el-option label="广告推广" value="AD" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio value="ENABLE">启用</el-radio>
          <el-radio value="DISABLE">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import manager from '@/api/manager'

const props = defineProps({
  modelValue: Boolean,
  data: Object
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  word: '',
  level: 'NORMAL',
  category: '',
  status: 'ENABLE',
  remark: ''
})

const rules = {
  word: [{ required: true, message: '请输入敏感词', trigger: 'blur' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

watch(() => props.data, (val) => {
  if (val) {
    Object.assign(form, val)
  } else {
    Object.assign(form, {
      word: '',
      level: 'NORMAL',
      category: '',
      status: 'ENABLE',
      remark: ''
    })
  }
}, { immediate: true })

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (form.id) {
      await manager.updateSensitiveWord(form)
      ElMessage.success('更新成功')
    } else {
      await manager.saveSensitiveWord(form)
      ElMessage.success('新增成功')
    }
    emit('success')
    handleClose()
  } catch (e) {
    ElMessage.error(form.id ? '更新失败' : '新增失败')
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
  formRef.value?.resetFields()
}
</script>
