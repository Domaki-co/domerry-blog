<template>
    <div class="editor-container">

        <div class="editor-main">
            <el-form :model="articleForm" :rules="rules" ref="articleForm" label-position="top" size="small">
                <div class="editor-content">
                    <div class="content-card">
                        <div class="title-cover-layout">
                            <div class="title-section">
                                <el-form-item label="文章标题" prop="title">
                                    <el-input v-model="articleForm.title" placeholder="请输入文章标题..." />
                                </el-form-item>
                                <el-form-item label="文章描述" prop="summary">
                                    <el-input v-model="articleForm.summary" type="textarea" :rows="4"
                                        placeholder="请输入文章描述..." />
                                </el-form-item>
                            </div>
                            <div class="cover-section">
                                <el-form-item label="封面图片" prop="cover">
                                    <div class="cover-area" @click="triggerCoverUpload">
                                        <div v-if="!articleForm.cover" class="cover-placeholder">
                                            <i class="fas fa-image"></i>
                                            <span>点击上传封面图</span>
                                        </div>
                                        <div v-else class="cover-preview">
                                            <img :src="articleForm.cover" alt="文章封面">
                                            <div class="cover-actions">
                                                <el-button size="small" circle type="danger" @click.stop="removeCover">
                                                    <i class="fas fa-trash"></i>
                                                </el-button>
                                            </div>
                                        </div>
                                    </div>
                                    <input ref="coverInput" type="file" accept="image/*" @change="handleCoverUpload"
                                        style="display: none">
                                </el-form-item>
                            </div>
                        </div>
                    </div>

                    <div class="content-card flex-card">
                        <el-form-item label="文章内容" prop="contentMd" class="mb-20">
                            <div id="vditor-editor"
                                style="height: 500px; width: 100%">
                            </div>
                        </el-form-item>
                    </div>
                </div>

                <div class="editor-sidebar">
                    <div class="sidebar-section">
                        <h3 class="section-title">
                            <i class="fas fa-folder"></i>
                            文章分类
                        </h3>
                        <el-form-item prop="categoryName">
                            <el-select v-model="articleForm.categoryName" placeholder="请选择分类">
                                <el-option v-for="item in categories" :key="item.id" :label="item.name"
                                    :value="item.name">
                                </el-option>
                            </el-select>
                        </el-form-item>
                    </div>

                    <div class="sidebar-section">
                        <h3 class="section-title">
                            <i class="fas fa-tags"></i>
                            文章标签
                        </h3>
                        <el-form-item prop="tags">
                            <el-select v-model="articleForm.tags" multiple filterable allow-create default-first-option
                                placeholder="请选择标签">
                                <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.name">
                                </el-option>
                            </el-select>
                        </el-form-item>
                    </div>

                    <div class="sidebar-section">
                        <h3 class="section-title">
                            <i class="fas fa-cog"></i>
                            文章设置
                        </h3>
                        <div class="setting-item">
                            <el-switch v-model="articleForm.isOriginal" :active-value="1" :inactive-value="0" active-text="原创文章">
                            </el-switch>
                        </div>
                        <div class="setting-item" v-if="isAdmin">
                            <el-switch v-model="articleForm.isStick" :active-value="1" :inactive-value="0" active-text="是否置顶">
                            </el-switch>
                        </div>
                        <div class="setting-item" v-if="!articleForm.isOriginal">
                            <div class="setting-label">转载地址：</div>
                            <el-form-item prop="originalUrl">
                                <el-input v-model="articleForm.originalUrl" placeholder="请输入原文地址" size="small" />
                            </el-form-item>
                        </div>
                        <div class="setting-item">
                            <div class="setting-label">关键词：</div>
                            <el-form-item prop="keywords">
                                <el-input v-model="articleForm.keywords" placeholder="请输入关键词（多个用逗号隔开）" size="small" />
                            </el-form-item>
                        </div>
                        <div class="setting-item">
                            <div class="setting-label">阅读方式：</div>
                            <el-form-item prop="readType">
                              <el-select v-model="articleForm.readType" placeholder="请选择阅读方式">
                                <el-option label="免费" :value="1" />
                                <el-option label="会员" :value="2" />
                                <el-option label="付费" :value="3" />
                              </el-select>
                            </el-form-item>
                        </div>
                        <div class="setting-item">
                            <el-button size="small" :type="regenerateAiDescribe ? 'warning' : 'default'"
                                @click="regenerateAiDescribe = !regenerateAiDescribe">
                                <i class="fas fa-sync"></i>
                                {{ regenerateAiDescribe ? '已选择：保存时生成' : (articleForm.id ? '重新生成 AI 摘要' : '生成 AI 摘要') }}
                            </el-button>
                        </div>
                    </div>
                    <div class="sidebar-section">
                        <h3 class="section-title">
                            <i class="fas fa-paper-plane"></i>
                            发布操作
                        </h3>
                        <div class="action-buttons">
                            <el-button size="small" @click="saveDraft">
                                <i class="fas fa-save"></i>
                                保存草稿
                            </el-button>
                            <el-button size="small" type="primary" @click="submitReview">
                                <i class="fas fa-paper-plane"></i>
                                提交审核
                            </el-button>
                            <el-button v-if="isAdmin" size="small" type="success" @click="publishArticle">
                                <i class="fas fa-check-circle"></i>
                                直接发布
                            </el-button>
                            <el-button v-if="isAdmin" size="small" type="warning" @click="savePrivate">
                                <i class="fas fa-lock"></i>
                                私密保存
                            </el-button>
                        </div>
                    </div>

                </div>
            </el-form>
        </div>
    </div>
</template>

<script>
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { uploadFileApi, deleteFileApi } from '@/api/file'
import { createArticleApi, updateArticleApi, getArticleInfoApi } from '@/api/article'
import { getTagsApi,getCategoriesApi } from '@/api/tags'

// 文章状态，与后端 sys_dict_data(article_status) 保持一致
const STATUS = {
    OFFLINE: 0,
    PUBLISHED: 1,
    DRAFT: 2,
    REVIEW: 3,
    PRIVATE: 4
}

export default {
    name: 'Editor',
    data() {
        return {
            articleForm: {
                title: '',
                summary: '',
                content: '',
                contentMd: '',
                cover: '',
                keywords: '',
                readType: '',
                isOriginal: 1,
                originalUrl: '',
                //是否置顶（仅管理员可切换，保存时随文章一起提交）
                isStick: 0,
                categoryName: '',
                tags: [],
                status: ''
            },
            rules: {
                title: [
                    { required: true, message: '请输入文章标题', trigger: 'blur' },
                    { min: 1, max: 100, message: '标题长度应在1-100个字符之间', trigger: 'blur' }
                ],
                readType: [{ required: true, message: '请选择阅读方式', trigger: 'blur' }],
                summary: [
                    { required: true, message: '请输入文章描述', trigger: 'blur' },
                    { min: 1, max: 500, message: '描述长度应在1-500个字符之间', trigger: 'blur' }
                ],
                contentMd: [
                    { required: true, message: '请输入文章内容', trigger: 'blur' },
                    {
                        validator: (rule, value, callback) => {
                            if (!value) {
                                callback();
                                return;
                            }

                            // 检查最小长度
                            if (value.length < 10) {
                                callback(new Error('文章内容至少需要10个字符'));
                                return;
                            }

                            // 检查重复字符
                            const repeatedCharPattern = /(.)\1{4,}/;

                            // 检查内容是否有意义
                            const meaninglessPattern = /^[a-zA-Z]{10,}$/;
                            if (meaninglessPattern.test(value)) {
                                callback(new Error('文章内容似乎没有实际意义，请认真编写'));
                                return;
                            }


                            callback();
                        },
                        trigger: ['blur', 'change']
                    }
                ],
                cover: [
                    { required: false, message: '请上传封面图片', trigger: 'change' }
                ],
                categoryName: [
                    { required: true, message: '请选择文章分类', trigger: 'blur' }
                ],
                tags: [
                    { required: true, message: '请选择文章标签', trigger: 'blur' },
                    {
                        validator: (rule, value, callback) => {
                            if (value.length > 3) {
                                callback(new Error('最多只能选择3个标签'));
                            } else {
                                callback();
                            }
                        },
                        trigger: 'change'
                    }
                ],
                originalUrl: [
                    { required: true, message: '请输入原文地址', trigger: 'blur' },
                    { type: 'url', message: '请输入有效的URL地址', trigger: 'blur' }
                ],
                keywords: [
                    {
                        validator: (rule, value, callback) => {
                            if (value && value.split(',').length > 5) {
                                callback(new Error('关键词最多不超过5个'));
                            } else {
                                callback();
                            }
                        },
                        trigger: 'blur'
                    }
                ]
            },
            categories: [],
            tags: [],
            isSubmitting: false,
            vditor: null,
            pendingContent: '',
            regenerateAiDescribe: false,
        }
    },
    computed: {
        /**
         * 是否管理员（管理员可在前台直接发布）
         */
        isAdmin() {
            return this.$store.getters.isAdmin
        }
    },
    created() {
        getCategoriesApi().then(res => {
            this.categories = res.data
        })
        getTagsApi().then(res => {
            this.tags = res.data
        })
        this.articleForm.id = this.$route.query.id
        if (this.articleForm.id) {
            getArticleInfoApi(this.articleForm.id).then(res => {
                this.articleForm = res.data
                //is_stick 在库中可能为 NULL，统一成 0/1，避免 el-switch 收到既非 active 也非 inactive 的值
                this.articleForm.isStick = this.articleForm.isStick ? 1 : 0
                this.pendingContent = this.articleForm.contentMd || ''
                // 数据加载完再初始化编辑器，确保内容渲染
                this.$nextTick(() => {
                    if (!this.vditor) {
                        this.initVditor()
                    } else {
                        this.vditor.setValue(this.pendingContent)
                    }
                })
            })
        }
    },
    mounted() {
        // 编辑模式由数据加载完成后初始化，新建模式直接初始化
        if (!this.articleForm.id) {
            this.initVditor()
        }
    },
    beforeUnmount() {
        if (this.vditor) {
            try {
                this.vditor.destroy()
            } catch (e) { /* ignore */ }
        }
    },
    methods: {
        /**
         * 初始化 Vditor（npm 方式加载）
         */
        initVditor() {
            if (this.vditor) return
            const _this = this
            this.vditor = new Vditor('vditor-editor', {
                height: 500,
                mode: 'ir',
                theme: 'classic',
                value: this.pendingContent || this.articleForm.contentMd || '',
                cache: {
                    enable: false
                },
                preview: {
                    math: true,
                    markdown: {
                        toc: true
                    },
                    theme: {
                        current: 'light'
                    }
                },
                toolbar: [
                    'headings', 'bold', 'italic', 'strike', '|',
                    'list', 'ordered-list', 'check', 'outdent', 'indent', '|',
                    'link', 'upload', 'table', 'code', 'inline-code', '|',
                    'fullscreen', 'preview', 'export', 'info'
                ],
                upload: {
                    handler(files) {
                        _this.handleUploadFiles(files)
                    }
                },
                after: () => {}
            })
        },

        /**
         * 保存草稿
         */
        async saveDraft() {
            this.articleForm.status = STATUS.DRAFT
            await this.submitArticle()
        },
        /**
         * 提交审核
         */
        async submitReview() {
            this.articleForm.status = STATUS.REVIEW
            await this.submitArticle()
        },
        /**
         * 直接发布（仅管理员）
         */
        async publishArticle() {
            this.articleForm.status = STATUS.PUBLISHED
            await this.submitArticle()
        },
        /**
         * 私密保存（仅作者本人和管理员可见，仅管理员可操作）
         */
        async savePrivate() {
            this.articleForm.status = STATUS.PRIVATE
            await this.submitArticle()
        },
        /**
         * 提交文章
         */
        submitArticle() {
            if (this.isSubmitting) return
            // 编辑器内容没有 input 回调实时同步，必须在校验前先取一次值，否则 contentMd 校验的是空旧值
            this.articleForm.contentMd = this.vditor ? this.vditor.getValue() : this.articleForm.contentMd;
            // 详情页按 HTML 渲染 content 字段，与 admin 端一致：存 getHTML() 渲染结果，不能存 markdown 源码
            this.articleForm.content = this.vditor ? this.vditor.getHTML() : this.articleForm.content;
            this.articleForm.regenerateAiDescribe = this.regenerateAiDescribe;
            this.$refs.articleForm.validate(valid => {
                if (!valid) return
                this.isSubmitting = true
                const api = this.articleForm.id ? updateArticleApi : createArticleApi
                api(this.articleForm).then(res => {
                    this.$message.success('保存成功')
                    this.regenerateAiDescribe = false
                    // 编辑已有文章保存后回到文章详情继续阅读，新建仍跳转个人主页文章列表
                    if (this.articleForm.id) {
                        this.$router.push(`/post/${this.articleForm.id}`)
                    } else {
                        this.$router.push({ path: '/user/profile', query: { tab: 'posts' } })
                    }
                }).catch(err => {
                    this.$message.error(err.message)
                }).finally(() => {
                    this.isSubmitting = false
                })
            })
        },
        /**
         * 删除图片
         */
        imgDel(url) {
            if (!url) return
            deleteFileApi(url).then(() => {
                this.$message.success('图片已删除')
            }).catch(() => {
                this.$message.error('图片删除失败')
            })
        },
        /**
         * Vditor 上传图片处理
         */
        handleUploadFiles(files) {
            const file = Array.isArray(files) ? files[0] : files
            if (!file) return
            if (file.size > 5 * 1024 * 1024) {
                this.$message.error('图片大小不能超过5MB')
                return
            }
            const formdata = new FormData()
            formdata.append('file', file)
            uploadFileApi(formdata, 'article-content').then(res => {
                const url = res.data
                this.vditor.insertValue('\n![image](' + url + ')\n')
            }).catch(err => {
                this.$message.error('上传失败: ' + (err.message || '请重试'))
            })
        },
        /**
         * 触发封面上传
         */
        triggerCoverUpload() {
            this.$refs.coverInput.click()
        },
        /**
         * 封面上传
         */
        handleCoverUpload(e) {
            const file = e.target.files[0]
            if (!file) return

            const formData = new FormData()
            formData.append('file', file)

            uploadFileApi(formData,'article-cover').then(res => {
                if (res.code === 200) {
                    this.articleForm.cover = res.data
                    this.$message.success('上传成功')
                } else {
                    this.$message.error(res.message || '上传失败')
                }
            }).catch(error => {
                this.$message.error('上传失败，请重试')
            })

            // 清空 input 的值，这样可以重复上传同一个文件
            e.target.value = ''
        },
        /**
         * 删除封面
         */
        removeCover() {
            this.articleForm.cover = ''
        }
    }
}
</script>

<style lang="scss" scoped>
.editor-container {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}


.editor-main {
    flex: 1;
    max-width: 1400px;
    margin: 0 auto;
    padding: 24px 40px;
    width: 100%;

    .el-form {
        display: flex;
        gap: 24px;
    }
}

.editor-content {
    flex: 1;
    min-width: 0;

    .content-card {
        background: var(--card-bg);
        border-radius: 8px;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
        padding: 24px;
        margin-bottom: 24px;

        &:last-child {
            margin-bottom: 0;
        }

        &.flex-card {
            flex: 1;
            display: flex;
            flex-direction: column;

            :deep(.el-form-item) {
                flex: 1;
                display: flex;
                flex-direction: column;
                margin-bottom: 0;

                .el-form-item__content {
                    flex: 1;
                    display: flex;
                    flex-direction: column;

                    .el-textarea {
                        flex: 1;
                        display: flex;

                        textarea {
                            flex: 1;
                            resize: none;
                        }
                    }
                }
            }
        }
    }

    :deep(.el-form-item) {
        margin-bottom: 0;

        .el-form-item__label {
            padding-bottom: 12px;
            font-size: 13px;
            color: #999;
            font-weight: normal;
        }
    }

    .title-cover-layout {
        display: flex;
        gap: 24px;
        align-items: flex-start;

        .title-section {
            flex: 1;
        }

        .cover-section {
            width: 240px;
            flex-shrink: 0;
        }
    }

    .cover-area {
        width: 100%;
        height: 135px;
        border-radius: 4px;
        overflow: hidden;
        cursor: pointer;
        position: relative;
        background: var(--card-bg);
        border: 1px dashed var(--border-color);
        transition: all 0.15s;

        &:hover {
            border-color: #409EFF;
        }
    }

    .cover-placeholder {
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8px;
        color: #999;

        i {
            font-size: 24px;
        }

        span {
            font-size: 13px;
        }
    }

    .cover-preview {
        height: 100%;

        img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .cover-actions {
            position: absolute;
            top: 12px;
            right: 12px;
            opacity: 0;
            transition: all 0.15s;
        }

        &:hover .cover-actions {
            opacity: 1;
        }
    }
}

.editor-sidebar {
    width: 320px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 24px;

    .sidebar-section {
        background: var(--card-bg);
        border-radius: 8px;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
        padding: 24px;

        .section-title {
            font-size: 13px;
            font-weight: 500;
            color: #999;
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            gap: 6px;

            i {
                font-size: 14px;
                color: #409EFF;
            }
        }

        .setting-item {
            padding: 12px 0;
            border-bottom: 1px solid var(--border-color);

            &:last-child {
                border-bottom: none;
                padding-bottom: 0;
            }

            &:first-child {
                padding-top: 0;
            }

            .setting-label {
                font-size: 13px;
                color: #999;
                margin-bottom: 8px;
            }

            :deep(.el-form-item) {
                margin-bottom: 0;
            }
        }

        .action-buttons {
            display: flex;
            flex-direction: column;
            gap: 12px;

            :deep(.el-button) {
                width: 100%;
                margin-left: 0;

                i {
                    margin-right: 4px;
                }
            }
        }
    }
}

@media screen and (max-width: 1200px) {


    .editor-main {
        padding: 24px;

        .el-form {
            flex-direction: column;
        }
    }

    .editor-sidebar {
        width: 100%;
    }
}

@media screen and (max-width: 768px) {


    .editor-main {
        padding: 16px;

        .el-form {
            gap: 16px;
        }
    }

    .editor-content {
        .content-card {
            padding: 20px;
            margin-bottom: 16px;
        }

        .cover-area {
            height: 120px;
        }
    }

    .editor-sidebar {
        gap: 16px;

        .sidebar-section {
            padding: 20px;
        }
    }

    .title-cover-layout {
        flex-direction: column;
        gap: 16px;

        .cover-section {
            width: 100%;
        }
    }
}

.vditor {
    --border-color: var(--border-color);
    background: var(--card-bg);
    border-radius: 4px;
}
.vditor-reset {
    color: var(--text-secondary);
}
</style>
