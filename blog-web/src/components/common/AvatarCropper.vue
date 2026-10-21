<template>
  <div>
    <el-dialog :title="title" v-model="visible" width="800px" append-to-body @opened="modalOpened" @close="closeDialog">
      <el-row>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <!-- 旧的 vue2 裁剪库没有 Vue 3 版本，改用 vue-advanced-cropper：
               img → src，@realTime → @change（回传裁剪画布），
               autoCropWidth/Height + fixedBox → stencil-props.aspectRatio + 默认 image-restriction="fit-area" -->
          <Cropper
            ref="cropper"
            class="avatar-cropper"
            :src="options.img"
            :canvas="canvasOptions"
            :stencil-props="{ aspectRatio: 1 }"
            @change="realTime"
            v-if="visible"
          />
        </el-col>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <div class="avatar-upload-preview">
            <img v-if="previews.url" :src="previews.url" class="avatar-preview-img" />
          </div>
        </el-col>
      </el-row>
      <br />
      <el-row>
        <el-col :lg="2" :sm="3" :xs="3">
          <el-upload action="#" :http-request="requestUpload" :show-file-list="false" :before-upload="beforeUpload">
            <el-button size="small">
              选择
              <el-icon><Upload /></el-icon>
            </el-button>
          </el-upload>
        </el-col>
        <el-col :lg="{span: 1, offset: 2}" :sm="2" :xs="2">
          <el-button icon="Plus" size="small" @click="changeScale(1)"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :sm="2" :xs="2">
          <el-button icon="Minus" size="small" @click="changeScale(-1)"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :sm="2" :xs="2">
          <el-button icon="RefreshLeft" size="small" @click="rotateLeft()"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :sm="2" :xs="2">
          <el-button icon="RefreshRight" size="small" @click="rotateRight()"></el-button>
        </el-col>
        <el-col :lg="{span: 2, offset: 6}" :sm="2" :xs="2">
          <el-button type="primary" size="small" @click="uploadImg()">提 交</el-button>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
import { Cropper } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'
import { uploadFileApi } from '@/api/file'
import { updateProfileApi } from '@/api/user'

export default {
  name: 'AvatarCropper',
  components: { Cropper },
  props: {
    user: {
      type: Object,
      default: () => ({})
    },
    // 父组件用的是 v-model（原组件声明的是 visible，两边对不上，一直打不开）
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      title: '修改头像',
      options: {
        img: null
      },
      // vue-advanced-cropper 用 canvas 尺寸设置代替旧库的 autoCropWidth/Height + outputType：
      // 同时驱动右侧实时预览和提交时的导出（正方形头像，400px 兼顾高清屏）
      canvasOptions: {
        width: 400,
        height: 400
      },
      previews: {},
      resizeHandler: null
    }
  },
  computed: {
    // Element Plus 的 el-dialog 只有 modelValue（v-model）语义；props 只读，
    // 内层关闭必须 emit 回传，直接 v-model 到 prop 上会报 readonly 警告并关不掉
    visible: {
      get() {
        return this.modelValue
      },
      set(val) {
        this.$emit('update:modelValue', val)
      }
    }
  },
  watch: {
    modelValue: {
      handler(newVal) {
        if (newVal) {
          this.options.img = (this.user && this.user.avatar) || null
        }
      },
      immediate: true
    }
  },
  methods: {
    /**
     * 打开裁剪器
     */
    editCropper() {
      this.$emit('update:modelValue', true)
    },
    /**
     * 弹出层
     */
    modalOpened() {
      if (!this.resizeHandler) {
        this.resizeHandler = () => {
          this.refresh()
        }
      }
      window.addEventListener('resize', this.resizeHandler)
    },
    /**
     * 刷新
     */
    refresh() {
      if (this.$refs.cropper) {
        this.$refs.cropper.refresh()
      }
    },
    /**
     * 上传头像（真正上传走 beforeUpload，这里无需实现）
     */
    requestUpload() {
    },
    /**
     * 旋转左
     */
    rotateLeft() {
      this.$refs.cropper.rotate(-90)
    },
    /**
     * 旋转右
     */
    rotateRight() {
      this.$refs.cropper.rotate(90)
    },
    /**
     * 缩放
     */
    changeScale(num) {
      num = num || 1
      // 旧库的 changeScale 在 vue-advanced-cropper 里对应 zoom(factor)
      this.$refs.cropper.zoom(num > 0 ? 1.25 : 0.8)
    },
    /**
     * 选择图片
     */
    beforeUpload(file) {
      if (file.type.indexOf('image/') === -1) {
        this.$message.error('文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。')
        return false
      }
      const reader = new FileReader()
      reader.readAsDataURL(file)
      reader.onload = () => {
        this.options.img = reader.result
      }
      return false
    },
    /**
     * 提交裁剪结果
     */
    async uploadImg() {
      const result = this.$refs.cropper && this.$refs.cropper.getResult()
      const canvas = result && result.canvas
      if (!canvas) {
        this.$message.error('裁剪失败，请重新选择图片')
        return
      }
      // 旧库用 getCropBlob(cb)，这里从裁剪画布导出
      const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'))
      const file = new File([blob], 'avatar.png', { type: 'image/png' })
      let formData = new FormData()
      formData.append('file', file)
      // 调用上传接口
      const response = await uploadFileApi(formData, 'avatar')
      if (response.data) {
        // 更新头像
        await updateProfileApi({
          id: this.user.id,
          avatar: response.data
        })
        this.visible = false
        this.$message.success('头像更新成功')
        this.$store.state.userInfo.avatar = response.data
        // 触发更新事件，通知父组件更新用户信息
        this.$emit('update-avatar', response.data)
      }
    },
    /**
     * 实时预览（@change 回传 { image, coordinates, visibleArea, imageTransforms, canvas }）
     */
    realTime({ canvas }) {
      this.previews = canvas ? { url: canvas.toDataURL() } : {}
    },
    /**
     * 关闭弹出层
     */
    closeDialog() {
      this.options.img = null
      this.previews = {}
      this.visible = false
      if (this.resizeHandler) {
        window.removeEventListener('resize', this.resizeHandler)
      }
    }
  },
  beforeUnmount() {
    // el-dialog 的 @close 要等 afterLeave 才触发，弹窗还开着就导航离开时 closeDialog 不会执行，
    // 这里兜底解绑，否则每次打开弹窗都会留下一个实例和它缓存的 previews dataURL
    if (this.resizeHandler) {
      window.removeEventListener('resize', this.resizeHandler)
      this.resizeHandler = null
    }
  }
}
</script>
<style scoped lang="scss">
.avatar-cropper {
	height: 350px;
}
.avatar-upload-preview {
	position: relative;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	width: 200px;
	height: 200px;
	border-radius: 50%;
	box-shadow: 0 0 4px #ccc;
	overflow: hidden;
}
.avatar-preview-img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
</style>
