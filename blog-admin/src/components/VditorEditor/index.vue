<template>
  <div class="vditor-editor">
    <div ref="editorRef" :style="{ height }" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { buildAttachmentMarkdown, encodeHref, isImageFile } from './attachment'

interface UploadSuccMap {
  errFiles: string[]
  succMap: Record<string, string>
}

interface UploadResult {
  file: File
  url: string
}

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  height?: string
  uploadHandler?: (file: File) => Promise<string>
}>(), {
  placeholder: '',
  height: '500px',
  uploadHandler: undefined
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'videoClick', vditorInstance: any): void
  (e: 'attachmentClick', vditorInstance: any): void
}>()

const editorRef = ref<HTMLElement | null>(null)
const vditor = ref<any>(null)
let initialized = false
let pendingValue: string | null = null

// vditor 的自定义 upload.handler 只会处理字符串返回值（错误提示），
// 上传成功后的回显必须由 handler 自己插入编辑器，否则图片不显示
const captureEditorRange = (): Range | null => {
  const instance = vditor.value
  const editable: HTMLElement | undefined = instance?.vditor?.[instance.vditor.currentMode]?.element
  if (!editable) return null
  const selection = window.getSelection()
  if (selection && selection.rangeCount > 0) {
    const range = selection.getRangeAt(0)
    if (editable.contains(range.startContainer)) {
      return range.cloneRange()
    }
  }
  return null
}

const restoreEditorRange = (range: Range | null) => {
  if (!range) return
  try {
    const selection = window.getSelection()
    selection?.removeAllRanges()
    selection?.addRange(range)
  } catch (e) {
    // 光标节点可能已随编辑失效，insertValue 会退回到默认位置
  }
}

// Vditor 上传 handler：调用外部 uploadHandler 获取图片地址，上传完成后插入 markdown 回显
const buildUploadHandler = () => {
  if (!props.uploadHandler) return undefined
  return (files: File[]) => {
    const savedRange = captureEditorRange()
    return Promise.all(files.map((file) => {
      return (props.uploadHandler as (f: File) => Promise<string>)(file)
        .then((url) => ({ file, url } as UploadResult))
        .catch(() => ({ file, url: '' } as UploadResult))
    })).then((results) => {
      const successResults = results.filter((item) => item.url)
      if (successResults.length) {
        restoreEditorRange(savedRange)
        // 图片仍走 markdown，其他文件插附件链接，否则会被渲染成破图
        const markdown = successResults.map((item) => (isImageFile(item.file)
          ? `![${item.file.name || 'image'}](${encodeHref(item.url)})\n`
          : `\n${buildAttachmentMarkdown(item.url, item.file.name, item.file.size)}\n`)).join('')
        vditor.value?.insertValue(`\n${markdown}`)
      }
      const errFiles = results.filter((item) => !item.url).map((item) => item.file.name)
      if (errFiles.length) {
        return `「${errFiles.join('、')}」上传失败`
      }
      const succMap: Record<string, string> = {}
      successResults.forEach((item) => {
        succMap[item.file.name] = item.url
      })
      const data: UploadSuccMap = { errFiles: [], succMap }
      return { msg: '上传成功', code: 0, data }
    })
  }
}

onMounted(() => {
  if (!editorRef.value) return
  vditor.value = new Vditor(editorRef.value, {
    value: props.modelValue,
    placeholder: props.placeholder,
    height: props.height,
    cache: { enable: false },
    toolbarConfig: {
      pin: true
    },
    toolbar: [
      'emoji', 'headings', 'bold', 'italic', 'strike', '|',
      'list', 'ordered-list', 'check', '|',
      'quote', 'line', 'code', 'inline-code', 'link', 'upload', '|',
      'undo', 'redo', '|',
      'preview', 'fullscreen', '|',
      {
        name: 'insert-video',
        tip: '插入视频',
        icon: '<svg viewBox="0 0 16 16" width="16" height="16" fill="currentColor"><path d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1zm-2 4.5l5 2.5-5 2.5v-5z"/></svg>',
        click: (instance: any) => {
          emit('videoClick', instance)
        }
      },
      {
        name: 'insert-attachment',
        tip: '插入附件',
        icon: '<svg viewBox="0 0 16 16" width="16" height="16" fill="currentColor"><path d="M4 1h5l3 3v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1zm4.5 1.5V4H11L8.5 2.5zM5 6.6h6v1H5v-1zm0 2.5h6v1H5v-1zM5 11.6h4v1H5v-1z"/></svg>',
        click: (instance: any) => {
          emit('attachmentClick', instance)
        }
      }
    ],
    upload: props.uploadHandler ? {
      multiple: true,
      accept: 'image/*,.svg,.png,.jpg,.jpeg,.gif,.webp',
      handler: buildUploadHandler()
    } : undefined,
    input: (value: string) => {
      emit('update:modelValue', value)
    },
    after: () => {
      initialized = true
      if (pendingValue !== null && vditor.value) {
        vditor.value.setValue(pendingValue)
        pendingValue = null
      }
    }
  })
})

watch(() => props.modelValue, (val) => {
  if (!initialized) {
    pendingValue = val
    return
  }
  if (vditor.value && val !== vditor.value.getValue()) {
    vditor.value.setValue(val)
  }
})

onBeforeUnmount(() => {
  vditor.value?.destroy()
})

defineExpose({
  instance: vditor,
  getValue: () => vditor.value?.getValue(),
  getHtml: () => vditor.value?.getHTML(),
  insertHtml: (html: string) => vditor.value?.insertValue(html),
  // 前后留空行，链接才会被当成独立块而不是接在上一段末尾
  insertAttachment: (url: string, name?: string, size?: number) =>
    vditor.value?.insertValue(`\n${buildAttachmentMarkdown(url, name, size)}\n`)
})
</script>

<style scoped>
.vditor-editor {
  width: 100%;
}
</style>
