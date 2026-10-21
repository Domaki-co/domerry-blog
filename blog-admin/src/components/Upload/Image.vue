<template>
  <div class="upload-container">
    <el-upload
      v-model:file-list="fileList"
      :action="uploadUrl"
      list-type="picture-card"
      :headers="headers"
      :multiple="multiple"
      :limit="limit"
      :on-preview="handlePreview"
      :on-remove="handleRemove"
      :on-success="handleSuccess"
      :on-exceed="handleExceed"
      :before-upload="beforeUpload"
      :accept="accept"
    >
      <el-icon><Plus /></el-icon>
      <template #tip>
        <div class="upload-tip">
          支持 {{ acceptText }} 格式，且不超过{{ fileSize }}MB
        </div>
      </template>
    </el-upload>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="dialogVisible" top="5vh" title="预览图片">
      <img :src="dialogImageUrl" alt="Preview Image" style="width: 100%; height: 500px; object-fit: contain;" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { UploadProps, UploadUserFile } from 'element-plus'
import { getToken } from '@/utils/auth'
import { uploadApi,deleteFileApi } from '@/api/file'
import { compressImage, formatSize } from '@/utils/image-compress'
const props = defineProps({
  modelValue: {
    type: [String, Array],
    default: ''
  },
  limit: {
    type: Number,
    default: 1
  },
  fileSize: {
    type: Number,
    default: 5
  },
  multiple: {
    type: Boolean,
    default: false
  },
  source: {
    type: String,
    default: 'default'
  }
})

const emit = defineEmits(['update:modelValue'])

// 上传地址
const uploadUrl =  `${import.meta.env.VITE_APP_BASE_API}/file/upload?source=${props.source}`

// 请求头
const headers = {
  Authorization: getToken()
}

const fileList = ref<UploadUserFile[]>([])
const dialogImageUrl = ref('')
const dialogVisible = ref(false)

// 初始化文件列表
const initFileList = () => {
  if (!props.modelValue) {
    fileList.value = []
    return
  }
  
  if (typeof props.modelValue === 'string') {
    fileList.value = [{
      name: props.modelValue.substring(props.modelValue.lastIndexOf('/') + 1),
      url: props.modelValue
    }]
  } else if (Array.isArray(props.modelValue)) {
    fileList.value = (props.modelValue as string[]).map(url => ({
      name: url.substring(url.lastIndexOf('/') + 1),
      url: url
    }))
  }
}

// 处理图片预览
const handlePreview: UploadProps['onPreview'] = (uploadFile) => {
  dialogImageUrl.value = uploadFile.url!
  dialogVisible.value = true
}

// 处理图片删除
const handleRemove: UploadProps['onRemove'] = async (uploadFile: any) => {
  if (props.multiple) {
    await deleteFileApi(uploadFile.url)
    const urls = (props.modelValue as string[]).filter(url => url !== uploadFile.url)
    emit('update:modelValue', urls)
  } else {
    await deleteFileApi(uploadFile.url)
    emit('update:modelValue', '')
  }
}

// 处理上传成功
const handleSuccess: UploadProps['onSuccess'] = async (response) => {
  console.log(response)
  if (response.code === 200) {
    const url = response.data
    if (props.multiple) {
      const urls = props.modelValue ? [...(props.modelValue as string[])] : []
      urls.push(url)
      emit('update:modelValue', urls)
      fileList.value = urls.map(u => ({
        name: u.substring(u.lastIndexOf('/') + 1),
        url: u
      }))
    } else {
      emit('update:modelValue', url)
      fileList.value = [{
        name: url.substring(url.lastIndexOf('/') + 1),
        url: url
      }]
    }
    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

// 处理超出限制
const handleExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning(`最多只能上传 ${props.limit} 个文件`)
}

// 允许上传的图片格式（覆盖常见位图、动图、矢量图与图标）
const IMAGE_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'avif', 'bmp', 'svg', 'ico']
// 文件选择框的过滤项，顺便用于提示文案
const accept = IMAGE_EXTS.map(ext => `.${ext}`).join(',')
const acceptText = IMAGE_EXTS.join('/')

// 取文件名后缀（小写）
const getExtension = (name?: string) => {
  const fileName = name || ''
  const index = fileName.lastIndexOf('.')
  return index > -1 ? fileName.slice(index + 1).toLowerCase() : ''
}

// 上传前的校验（文章封面在浏览器端压缩后再上传，原图不进对象存储）
const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
  // 部分浏览器对 ico/svg 给出的 MIME 不一致，扩展名优先，MIME 兜底
  const isImage = IMAGE_EXTS.includes(getExtension(file.name))
    || (!!file.type && file.type.startsWith('image/'))
  const isLt = file.size / 1024 / 1024 < props.fileSize

  if (!isImage) {
    ElMessage.error(`只能上传 ${acceptText} 格式的图片!`)
    return false
  }
  if (!isLt) {
    ElMessage.error(`图片大小不能超过 ${props.fileSize}MB!`)
    return false
  }

  // 只有文章封面做压缩，其他 source（相册、站点配置等）原样上传
  if (props.source !== 'article-cover') return true

  try {
    const compressed = await compressImage(file)
    if (compressed !== file) {
      ElMessage.success(`封面已压缩：${formatSize(file.size)} → ${formatSize(compressed.size)}`)
    }
    // 返回 File 让 el-upload 上传压缩后的文件
    return compressed
  } catch (err) {
    // 压缩失败不阻断上传，退回原图
    return true
  }
}

// 监听modelValue变化
watch(() => props.modelValue, () => {
  initFileList()
}, { immediate: true })
</script>

<style scoped>
.upload-container {
  .upload-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}

:deep(.el-upload--picture-card) {
  --el-upload-picture-card-size: 100px;
}

:deep(.el-upload-list--picture-card) {
  --el-upload-list-picture-card-size: 100px;
}
</style> 