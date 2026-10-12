<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <div class="search-wrapper">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="标题" prop="title">
          <el-input v-model="queryParams.title" placeholder="请输入文章标题" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable>
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tagId">
          <el-select v-model="queryParams.tagId" placeholder="请选择标签" clearable>
            <el-option v-for="item in tagOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option v-for="item in statusOptions" :key="item.id" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作按钮区域 -->
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <ButtonGroup>
            <el-button type="primary" icon="Plus" @click="handleAdd" v-permission="['sys:article:add']">新增文章</el-button>
            <el-button type="danger" icon="Delete" :disabled="selectedIds.length === 0"
              v-permission="['sys:article:delete']" @click="handleBatchDelete">批量删除</el-button>
            <el-button type="warning" icon="Setting" v-permission="['sys:article:reptile']"
              @click="reptileDialog.visible = true">爬取文章</el-button>
          </ButtonGroup>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table v-loading="loading" :data="tableData" row-key="id" style="width: 100%"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="封面" align="center" width="133">
          <template #default="scope">
            <el-image style="width: 120px; height: 80px;border-radius: 10px" :src="resolveCover(scope.row)" fit="cover">
              <template #error>
                <img src="/static/img/book.png" style="width: 120px; height: 80px; border-radius: 10px; object-fit: cover;" />
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="标题" align="center" prop="title" width="200" show-overflow-tooltip>
          <template #default="scope">
            <span style="color: var(--el-color-primary);">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="作者" align="center" prop="nickname" show-overflow-tooltip />
        <el-table-column label="分类" align="center" prop="categoryName" />
        <el-table-column label="标签" align="center" width="200">
          <template #default="scope">
            <el-tag v-for="tag in scope.row.tags" :key="tag.id" class="mx-1" size="small">
              {{ tag.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" align="center" prop="status" width="110">
          <template #default="scope">
            <el-select v-model="scope.row.status" size="small" style="width: 92px"
              @change="handleChangeStatus(scope.row)">
              <el-option v-for="item in getRowStatusOptions(scope.row)" :key="item.value" :value="Number(item.value)"
                :label="item.label" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="是否推荐" align="center">
          <template #default="{ row }">
            <span v-for="item in yesNoOptions">
              <el-tag :type="item.style" v-if="row.isRecommend === Number(item.value)">
                {{ item.label }}
              </el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="是否置顶" align="center">
          <template #default="{ row }">
            <el-tooltip content="仅上架/私密文章可置顶；私密文章只有作者本人能在门户看到" placement="top" :disabled="!isStickLocked(row)">
              <el-switch @change="handleChangeStick(row)"
                style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
                v-model="row.isStick" :active-value="1" :inactive-value="0" :disabled="isStickLocked(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="阅读量" align="center" prop="quantity" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
        <el-table-column label="操作" align="center" width="200" fixed="right">
          <template #default="scope">
            <el-button type="primary" link icon="Edit" @click="handleUpdate(scope.row)"
              v-permission="['sys:article:update']">修改</el-button>
            <el-button type="danger" link icon="Delete" @click="handleDelete(scope.row)"
              v-permission="['sys:article:delete']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container">
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 30, 50]" :total="total" :background="true"
          layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="dialog.title" v-model="dialog.visible" width="1400px" top="3vh" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="文章标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题" />
        </el-form-item>

        <el-form-item label="文章封面" prop="cover">
          <UploadImage v-model="form.cover" :limit="1" :source="'article-cover'" />
        </el-form-item>

        <el-form-item label="文章简介" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请输入文章简介" />
        </el-form-item>

        <el-row :gutter="20" class="mb-20">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryName">
              <el-tag
                type="success"
                v-show="form.categoryName"
                style="margin: 0 1rem 0 0"
                :closable="true"
                @close="removeCategory()"
              >
                {{ form.categoryName }}
              </el-tag>
              <!-- 分类选项 -->
              <el-popover
                placement="bottom-start"
                width="460"
                trigger="click"
                v-if="!form.categoryName"
              >
                <div class="popover-title">分类</div>
                <!-- 输入框 -->
                <el-input
                  style="width: 100%"
                  v-model="categoryName"
                  placeholder="请输入分类名,enter添加自定义分类"
                  @keyup.enter="saveCategory"
                />
                <!-- 分类 -->
                <div class="popover-container">
                  <div>添加分类</div>
                  <el-tag
                    v-for="(item, index) of categoryOptions"
                    :key="index"
                    style="margin-left: 3px; margin-top: 2px"
                    class="category-item"
                    @click="addCategory(item.name)"
                  >
                    {{ item.name }}
                  </el-tag>
                </div>
                <template #reference>
                  <el-button type="success" plain> 添加分类 </el-button>
                </template>
              </el-popover>
            </el-form-item>

          </el-col>
          <el-col :span="12">
            <el-form-item label="标签" prop="tags">
              <el-tag
                v-for="(item, index) of form.tags"
                :key="index"
                style="margin: 0 1rem 0 0"
                :closable="true"
                @close="removeTag(item)"
              >
                {{ item }}
              </el-tag>
              <!-- 标签选项 -->
              <el-popover
                placement="bottom-start"
                width="460"
                trigger="click"
                v-if="form.tags && form.tags.length < 3"
              >
                <div class="popover-title">标签</div>
                <!-- 搜索框 -->
                <el-input
                  style="width: 100%"
                  v-model="tagName"
                  placeholder="请输入标签名,enter添加自定义标签"
                  @keyup.enter="saveTag"
                />
                <!-- 标签 -->
                <div class="popover-container">
                  <div>添加标签</div>
                  <el-tag
                    v-for="(item, index) of tagOptions"
                    :key="index"
                    style="margin-left: 3px; margin-top: 2px"
                    @click="addTag(item.name)"
                  >
                    {{ item.name }}
                  </el-tag>
                </div>
                <template #reference>
                  <el-button type="primary" plain> 添加标签 </el-button>
                </template>
              </el-popover>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="mb-20">
          <el-col :span="8">
            <el-form-item label="阅读方式" prop="readType">
              <el-select v-model="form.readType" placeholder="请选择阅读方式">
                <el-option label="免费" :value="1" />
                <el-option label="会员" :value="2" />
                <el-option label="付费" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="文章类型" prop="isOriginal">
              <el-select v-model="form.isOriginal" placeholder="请选择文章类型">
                <el-option label="原创" :value="1" />
                <el-option label="转载" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关键词" prop="keywords">
              <el-input v-model="form.keywords" placeholder="请输入关键词" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="转载地址" prop="originalUrl" v-if="form.isOriginal === 0" class="mb-20">
          <el-input v-model="form.originalUrl" placeholder="请输入转载地址" />
        </el-form-item>

        <el-row :gutter="20" class="mb-20">
          <el-col :span="6">
            <el-form-item label="是否置顶" prop="isStick">
              <el-switch style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949" v-model="form.isStick"
                :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="是否发布" prop="status">
              <el-select v-model="form.status" placeholder="请选择文章状态">
                <el-option v-for="item in statusOptions" :key="item.id" :value="Number(item.value)"
                  :label="item.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="首页轮播" prop="isCarousel">
              <el-switch style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949" v-model="form.isCarousel"
                :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="是否推荐" prop="isRecommend">
              <el-switch style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
                v-model="form.isRecommend" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="AI 摘要">
          <el-button size="small" :type="form.regenerateAiDescribe ? 'warning' : 'default'"
            @click="form.regenerateAiDescribe = !form.regenerateAiDescribe">
            {{ form.regenerateAiDescribe ? '已选择：保存时生成' : (dialog.type === 'add' ? '生成 AI 摘要' : '重新生成 AI 摘要') }}
          </el-button>
          <span style="margin-left: 12px; color: var(--el-text-color-secondary);">
            不选择则不生成，也不会覆盖已有摘要
          </span>
        </el-form-item>

        <el-form-item label="文章内容" prop="contentMd" class="mb-20">
          <VditorEditor ref="mdRef" v-model="form.contentMd" placeholder="输入文章内容..." height="500px"
            :upload-handler="uploadImage" @video-click="handleVideoClick" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 爬取文章对话框 -->
    <el-dialog title="爬取文章" v-model="reptileDialog.visible" width="800px">
      <el-form ref="reptileFormRef" :model="reptileForm" :rules="rules" label-width="100px">
        <el-form-item label="来源类型" prop="type">
          <el-select v-model="reptileForm.type" style="width: 100%">
            <el-option label="CSDN 文章（单篇）" value="csdn" />
            <el-option label="通用网页（个人博客等，单篇）" value="html" />
            <el-option label="RSS 订阅源（导入最新文章）" value="rss" />
            <el-option label="Discourse 论坛（导入最新主题）" value="discourse" />
          </el-select>
        </el-form-item>
        <el-form-item label="爬取地址" prop="url">
          <el-input v-model="reptileForm.url" placeholder="请输入爬取地址" />
        </el-form-item>
      </el-form>
      <div style="margin-top: 20px;">
        <el-alert
          title="CSDN/通用网页填单篇文章链接；RSS 填订阅源地址（如博客的 /feed、/index.xml）；Discourse 填站点地址。导入后均为私密状态，确认后再发布"
          type="success" :closable="false" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitReptile">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 插入视频对话框 -->
    <el-dialog center title="插入视频" v-model="dialogVisible" width="40%">
      <div class="video-upload-row">
        <el-upload style="display: inline-block" :show-file-list="false" name="filedatas" action=""
          :http-request="uploadVideo">
          <el-button type="primary" icon="Upload">上传视频文件</el-button>
        </el-upload>
      </div>
      <el-divider>或</el-divider>
      <el-input v-model="videoInput" placeholder="输入视频地址">
        <template #append>
          <el-button type="primary" @click="addVideo">插入地址</el-button>
        </template>
      </el-input>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox,ElLoading  } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import UploadImage from '@/components/Upload/Image.vue'
import VditorEditor from '@/components/VditorEditor/index.vue'
import { getCategoryListApi } from '@/api/article/category'
import { getTagListApi } from '@/api/article/tag'
import {
  getArticleListApi, getDetailApi, deleteArticleApi,
  addArticleApi, updateArticleApi, updateStatusApi, reptileArticleApi
} from '@/api/article'
import { uploadApi } from '@/api/file'
import { getDictDataByDictTypesApi } from '@/api/system/dict'

const DEFAULT_COVER = '/static/img/book.png'

const resolveCover = (row: any) => row.cover || DEFAULT_COVER

// 模拟数据
const categoryOptions = ref<any>([])

const tagOptions = ref<any>([])

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  categoryId: undefined,
  tagId: undefined,
  status: undefined
})

const loading = ref(false)
const total = ref(0)
const tableData = ref([])
const queryFormRef = ref<FormInstance>()
const formRef = ref<FormInstance>()
const mdRef = ref();
const submitLoading = ref(false)

// 选中项数组
const selectedIds = ref<string[]>([])

// 弹窗控制
const dialog = reactive({
  title: '',
  visible: false,
  type: 'add'
})

const reptileDialog = reactive({
  visible: false,
})

// 表单数据
const form = reactive<any>({
  id: undefined,
  title: '',
  cover: '',
  summary: '',
  categoryName: '',
  tags: [],
  content: '',
  contentMd: '',
  readType: 1,
  isOriginal: 1,
  originalUrl: '',
  isStick: 0,
  status: 1,
  isCarousel: 0,
  isRecommend: 0,
  keywords: '',
  regenerateAiDescribe: false
})

const reptileForm = reactive({
  url: '',
  type: 'csdn'
})

const statusOptions = ref<any>([])
const yesNoOptions = ref<any>([])

// 列表内允许直接切换的三种状态：上架 / 下架 / 私密
const ROW_STATUS_VALUES = [1, 0, 4]

// 兼容历史数据中可能存在的草稿(2)/审核(3)：当前值不在三态内时补进选项，保证能正常回显
const getRowStatusOptions = (row: any) => {
  const opts = statusOptions.value.filter((item: any) => ROW_STATUS_VALUES.includes(Number(item.value)))
  if (row.status != null && !opts.some((item: any) => Number(item.value) === Number(row.status))) {
    const current = statusOptions.value.find((item: any) => Number(item.value) === Number(row.status))
    if (current) opts.unshift(current)
  }
  return opts
}

// 只有能出现在门户置顶栏的文章才允许置顶：上架(1)所有人可见、私密(4)仅作者本人可见；
// 下架/草稿/审核不会展示，因此禁止置顶。已置顶的仍放行，避免状态变化后开关关不掉
const STICKABLE_STATUSES = [1, 4]
const isStickLocked = (row: any) =>
  !STICKABLE_STATUSES.includes(Number(row.status)) && row.isStick !== 1

const dialogVisible = ref(false)
const videoInput = ref('')

const tagName = ref('')
const categoryName = ref('')





// 表单校验规则
const rules = reactive<FormRules>({
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  categoryName: [
    { required: true, message: '请选择文章分类', trigger: 'change' }
  ],
  contentMd: [
    { required: true, message: '请输入文章内容', trigger: 'blur' }
  ],
  summary: [
    { required: true, message: '请输入文章简介', trigger: 'blur' },
    { max: 500, message: '简介最多500个字符', trigger: 'blur' }
  ],
  readType: [
    { required: true, message: '请选择阅读方式', trigger: 'change' }
  ],
  isOriginal: [
    { required: true, message: '请选择文章类型', trigger: 'change' }
  ],
  tags: [
    { required: true, message: '请选择文章标签', trigger: 'change' }
  ],
  originalUrl: [
    {
      required: true,
      message: '请输入转载地址',
      trigger: 'blur',
      validator: (rule: any, value: string, callback: any) => {
        if (form.isOriginal === 0 && !value) {
          callback(new Error('转载文章必须填写转载地址'))
        } else {
          callback()
        }
      }
    }
  ]
})

const removeTag = (tag: string) => {
  form.tags = form.tags.filter((item: string) => item !== tag)
}

const addTag = (tag: any) => {
  if (form.tags.includes(tag)) {
    ElMessage.warning('标签已存在')
    return
  }
  form.tags.push(tag)
}

const saveTag = () => {
  if (tagName.value.trim() !== "") {
    addTag(tagName.value);
    tagName.value = "";
  }
}

const removeCategory = () => {
  form.categoryName = ''
}

const addCategory = (category: any) => {
  if (form.categoryName.includes(category)) {
    ElMessage.warning('分类已存在')
    return
  }
  form.categoryName = category
}

const saveCategory = () => {
  if (categoryName.value.trim() !== "") {
    addCategory(categoryName.value);
    categoryName.value = "";
  }
}



// 图片上传（Vditor 内置上传，通过 uploadHandler 调用后端接口）
const uploadImage = async (file: File) => {
  const formdata = new FormData()
  formdata.append('file', file)
  const res = await uploadApi(formdata, 'article-content')
  return res.data
}

// 工具栏点击"插入视频"
const handleVideoClick = () => {
  dialogVisible.value = true
}

// 上传视频文件
const uploadVideo = (param: any) => {
  const loading = ElLoading.service({
    lock: true,
    text: 'Loading',
    background: 'rgba(0, 0, 0, 0.7)',
  })
  const formData = new FormData()
  formData.append('file', param.file)
  return uploadApi(formData, 'article-content').then((res) => {
    insertVideo(res.data)
    return res
  }).finally(() => {
    loading.close()
  })
}

// 插入视频到编辑器光标处
const insertVideo = (src: string) => {
  mdRef.value?.insertHtml(`<video height=100% width=100% controls autoplay src="${src}"></video>`)
  dialogVisible.value = false
  videoInput.value = ''
}

/**
 * 添加网络视频地址
 */
const addVideo = () => {
  if (!videoInput.value.trim()) {
    ElMessage.warning('请输入视频地址')
    return
  }
  insertVideo(videoInput.value)
}

// 获取分类列表
const getList = async () => {
  loading.value = true
  try {
    const { data } = await getArticleListApi(queryParams)
    // is_stick / status 可能是 NULL，必须规范成 0/1 等确定值。
    // 否则 el-switch 在渲染时会因为 modelValue 既不等于 active-value 也不等于
    // inactive-value 而自动 emit 一次 change，导致「一进列表文章被静默改状态」
    // 「点一个置顶、其它置顶开关跟着变」这类连动问题。
    tableData.value = (data.records || []).map((item: any) => ({
      ...item,
      status: item.status ?? 0,
      isStick: item.isStick ?? 0
    }))
    total.value = data.total
  } catch (error) {
  }
  loading.value = false
}

// 获取状态列表
const getStatusList = async () => {
  const { data } = await getDictDataByDictTypesApi(['article_status', 'sys_yes_no'])
  statusOptions.value = data.article_status.list
  yesNoOptions.value = data.sys_yes_no.list
}

// 表格选择项变化
const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 爬取文章
const submitReptile = () => {
  if (!reptileForm.url) return
  reptileArticleApi(reptileForm.url, reptileForm.type).then((res) => {
    ElMessage.success(`爬取成功，导入 ${res.data ?? 0} 篇`)
    getList()
    reptileDialog.visible = false
    reptileForm.url = ''
    reptileForm.type = 'csdn'
  })
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) return

  ElMessageBox.confirm(`是否确认删除 ${selectedIds.value.length} 篇文章?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteArticleApi(selectedIds.value)
      ElMessage.success('删除成功')
      selectedIds.value = []
      getList()
    } catch (error) {
    }
  })
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`是否确认删除 ${row.title} 这篇文章?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteArticleApi(row.id)
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
    }
  })
}

// 发布文章
const handleChangeStatus = (row: any) => {
  updateStatusApi({ id: row.id, status: row.status }).then((res) => {
    ElMessage.success('修改成功')
    getList()
  })
}

// 置顶/取消置顶（后端 updateById 只更新非空字段，这里只提交 isStick）
const handleChangeStick = (row: any) => {
  updateStatusApi({ id: row.id, isStick: row.isStick }).then((res) => {
    ElMessage.success(row.isStick === 1 ? '已置顶' : '已取消置顶')
    getList()
  })
}

// 搜索
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

// 重置查询
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

//清空表单
const clearForm = () => {
  form.id = undefined
  form.title = ''
  form.cover = ''
  form.summary = ''
  form.categoryName = ''
  form.tags = []
  form.content = ''
  form.contentMd = ''
  form.originalUrl = ''
  form.isStick = 0
  form.status = 1
  form.isCarousel = 0
  form.isRecommend = 0
  form.keywords = ''
  form.regenerateAiDescribe = false
}

// 新增用户
const handleAdd = () => {
  dialog.type = 'add'
  dialog.title = '新增文章'
  dialog.visible = true
  clearForm()
}

// 修改分类
const handleUpdate = (row: any) => {
  clearForm()
  dialog.type = 'edit'
  dialog.title = '修改文章'
  dialog.visible = true
  getDetailApi(row.id).then((res) => {
    Object.assign(form, res.data)
  })
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      form.content = mdRef.value?.getHtml()
      try {
        if (dialog.type === 'add') {
          await addArticleApi(form)
          ElMessage.success('新增成功')
        } else {
          await updateArticleApi(form)
          ElMessage.success('修改成功')
        }
        getList()
        dialog.visible = false
      } catch (error) {
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 取消按钮
const cancel = () => {
  dialog.visible = false
  reptileDialog.visible = false
  formRef.value?.resetFields()
  reptileForm.url = ''
}

// 分页大小改变
const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

// 页码改变
const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

// 初始化
onMounted(() => {
  getList()
  getCategoryListApi({ pageNum: 1, pageSize: 1000 }).then((res) => {
    categoryOptions.value = res.data.records
  })
  getTagListApi({ pageNum: 1, pageSize: 1000 }).then((res) => {
    tagOptions.value = res.data.records
  })

  getStatusList()
})

// 图片上传前的处理
const beforeAvatarUpload = (file: File) => {
  // 这里添加文件类型和大小限制
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('上传头像图片只能是 JPG/PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!')
    return false
  }

  // 模拟上传
  form.cover = URL.createObjectURL(file)
  return false
}
</script>

<style lang="scss" scoped>
.avatar-uploader {
  :deep(.el-upload) {
    border: 2px dashed var(--el-border-color-lighter);
    border-radius: 8px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);

    &:hover {
      border-color: var(--el-color-primary);
      background-color: var(--el-color-primary-light-9);
    }
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: var(--el-text-color-secondary);
    width: 120px;
    height: 120px;
    text-align: center;
    line-height: 120px;
  }

  .avatar {
    width: 120px !important;
    height: 120px !important;
  }
}

.app-container {

  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  :deep(.el-dialog) {
    .el-dialog__body {
      padding: 20px 40px;
    }

    .el-form {
      .el-form-item {
        margin-bottom: 22px;

        &:last-child {
          margin-bottom: 0;
        }

        .el-form-item__label {
          font-weight: 500;
          padding-right: 20px;
        }
      }

      .el-select {
        width: 100%;
      }
    }

    .vditor-editor {
      z-index: 1;
      margin-bottom: 20px;
    }
  }

  .mb-20 {
    margin-bottom: 20px;
  }



  .video-upload-row {
    text-align: center;
  }

  .dialog-footer {
    text-align: right;
    padding-top: 20px;
    border-top: 1px solid var(--el-border-color-lighter);

    .el-button {
      padding: 12px 25px;

      &+.el-button {
        margin-left: 12px;
      }
    }
  }

  .el-tag {
    margin-right: 8px;
    margin-bottom: 8px;
  }
}

:root[data-theme='dark'] {
  .app-container {
    :deep(.el-dialog) {
      .el-form {
        .el-input__wrapper {
          box-shadow: 0 0 0 1px var(--el-border-color) inset;
        }
      }
    }
  }
}
</style>
