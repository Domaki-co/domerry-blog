<template>
    <div class="editor-container">

        <!-- 附件上传入口：工具栏「插入附件」触发，不限文件类型 -->
        <input ref="attachmentInput" type="file" @change="handleAttachmentUpload" style="display: none">

        <div class="editor-main">
            <el-form :model="articleForm" :rules="rules" ref="articleForm" label-position="top" size="small">
                <div class="editor-content">
                    <div class="content-card">
                        <div class="title-cover-layout">
                            <div class="title-section">
                                <el-form-item label="文章标题" prop="title">
                                    <el-input v-model="articleForm.title" placeholder="请输入文章标题..." class="title-input" size="default" />
                                </el-form-item>
                                <el-form-item label="文章描述" prop="summary">
                                    <el-input v-model="articleForm.summary" type="textarea" :rows="4"
                                        placeholder="请输入文章描述..." class="summary-input" />
                                </el-form-item>
                            </div>
                            <div class="cover-section">
                                <el-form-item label="封面图片" prop="cover">
                                    <div class="cover-area" @click="triggerCoverUpload">
                                        <div v-if="!articleForm.cover" class="cover-placeholder">
                                            <div class="cover-icon-wrap">
                                                <i class="fas fa-image"></i>
                                            </div>
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
                                style="height: 580px; width: 100%">
                            </div>
                        </el-form-item>
                    </div>
                </div>

                <div class="editor-sidebar">
                    <div class="sidebar-section">
                        <h3 class="section-title">
                            <span class="title-icon"><i class="fas fa-folder"></i></span>
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
                            <span class="title-icon"><i class="fas fa-tags"></i></span>
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
                            <span class="title-icon"><i class="fas fa-cog"></i></span>
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
                                class="ai-btn"
                                @click="regenerateAiDescribe = !regenerateAiDescribe">
                                <i class="fas fa-magic"></i>
                                {{ regenerateAiDescribe ? '已选择：保存时生成' : (articleForm.id ? '重新生成 AI 摘要' : '生成 AI 摘要') }}
                            </el-button>
                        </div>
                    </div>
                    <div class="sidebar-section action-section">
                        <h3 class="section-title">
                            <span class="title-icon"><i class="fas fa-paper-plane"></i></span>
                            发布操作
                        </h3>
                        <div class="action-buttons">
                            <el-button v-if="isAdmin" type="primary" class="primary-action-btn" @click="publishArticle">
                                <i class="fas fa-check-circle"></i>
                                直接发布
                            </el-button>
                            <el-button v-else type="primary" class="primary-action-btn" @click="submitReview">
                                <i class="fas fa-paper-plane"></i>
                                提交审核
                            </el-button>
                            <div class="secondary-actions">
                                <el-button class="secondary-btn" @click="saveDraft">
                                    <i class="fas fa-save"></i>
                                    保存草稿
                                </el-button>
                                <el-button v-if="isAdmin" class="secondary-btn" type="warning" plain @click="savePrivate">
                                    <i class="fas fa-lock"></i>
                                    私密保存
                                </el-button>
                            </div>
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
import { buildAttachmentMarkdown, encodeHref, isImageFile } from '@/utils/attachment'
import { compressImage, formatSize } from '@/utils/image'

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
                    {
                        // async-validator 的 type: 'url' 只要求出现 "//"，javascript:// 这类伪协议能混过去，故显式校验协议
                        validator: (rule, value, callback) => {
                            if (!value) {
                                callback();
                                return;
                            }
                            try {
                                const protocol = new URL(value).protocol;
                                if (protocol === 'http:' || protocol === 'https:') {
                                    callback();
                                } else {
                                    callback(new Error('原文地址只支持 http/https 协议'));
                                }
                            } catch (error) {
                                callback(new Error('请输入有效的URL地址'));
                            }
                        },
                        trigger: 'blur'
                    }
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
        }).catch(() => {
            this.$message.error('获取分类失败')
        })
        getTagsApi().then(res => {
            this.tags = res.data
        }).catch(() => {
            this.$message.error('获取标签失败')
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
            }).catch(() => {
                this.$message.error('获取文章信息失败')
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
                // 必须显式给宽度：Vditor 默认 width = "auto"，initUI 会把容器 div 的
                // inline style 改成 width: auto，而 #vditor-editor 是 .el-form-item__content
                // （display: flex）里的 flex 项 —— width: auto 于是退化成"按内容收缩"，
                // 空内容时只剩工具栏的 min-content 宽度（约 650px），右侧留一大片空白，
                // 直到正文写到足够宽才被撑满（"字数多了框才恢复正常"就是这么来的）
                width: '100%',
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
                    {
                        name: 'insert-attachment',
                        tip: '插入附件',
                        icon: '<svg viewBox="0 0 16 16" width="16" height="16" fill="currentColor"><path d="M4 1h5l3 3v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1zm4.5 1.5V4H11L8.5 2.5zM5 6.6h6v1H5v-1zm0 2.5h6v1H5v-1zM5 11.6h4v1H5v-1z"/></svg>',
                        click: () => {
                            this.$refs.attachmentInput && this.$refs.attachmentInput.click()
                        }
                    },
                    '|',
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
            // 拖拽/粘贴进来的非图片文件不能按图片插入，否则正文里会出现破图
            if (!isImageFile(file)) {
                this.uploadAttachmentFile(file)
                return
            }
            if (file.size > 5 * 1024 * 1024) {
                this.$message.error('图片大小不能超过5MB')
                return
            }
            const formdata = new FormData()
            formdata.append('file', file)
            uploadFileApi(formdata, 'article-content').then(res => {
                this.vditor.insertValue('\n![' + (file.name || 'image') + '](' + encodeHref(res.data) + ')\n')
            }).catch(err => {
                this.$message.error('上传失败: ' + (err.message || '请重试'))
            })
        },

        /**
         * 工具栏「插入附件」选择的文件
         */
        handleAttachmentUpload(e) {
            const file = e.target.files && e.target.files[0]
            // 清空 input 的值，同一文件可重复选择
            e.target.value = ''
            if (!file) return
            this.uploadAttachmentFile(file)
        },

        /**
         * 上传附件并插入下载卡片
         */
        uploadAttachmentFile(file) {
            if (file.size > 20 * 1024 * 1024) {
                this.$message.error('附件大小不能超过20MB')
                return
            }
            const formdata = new FormData()
            formdata.append('file', file)
            uploadFileApi(formdata, 'article-content').then(res => {
                this.vditor.insertValue('\n' + buildAttachmentMarkdown(res.data, file.name, file.size) + '\n')
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
        async handleCoverUpload(e) {
            const file = e.target.files[0]
            // 清空 input 的值，这样可以重复上传同一个文件
            e.target.value = ''
            if (!file) return

            // 封面在浏览器端压缩后再上传，原图不进对象存储
            let cover = file
            try {
                cover = await compressImage(file)
                if (cover !== file) {
                    this.$message.success(`封面已压缩：${formatSize(file.size)} → ${formatSize(cover.size)}`)
                }
            } catch (err) {
                // 压缩失败不阻断上传，退回原图
                cover = file
            }

            const formData = new FormData()
            formData.append('file', cover)

            uploadFileApi(formData, 'article-cover').then(res => {
                if (res.code === 200) {
                    this.articleForm.cover = res.data
                    this.$message.success('上传成功')
                } else {
                    this.$message.error(res.message || '上传失败')
                }
            }).catch(error => {
                this.$message.error('上传失败，请重试')
            })
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
    background: var(--background);
}

.editor-main {
    flex: 1;
    max-width: 1440px;
    margin: 0 auto;
    padding: 24px 32px 48px;
    width: 100%;

    .el-form {
        display: flex;
        gap: 24px;
        align-items: flex-start;
    }
}

.editor-content {
    flex: 1;
    min-width: 0;

    .content-card {
        background: var(--surface);
        border: 1px solid var(--rule);
        border-radius: 12px;
        padding: 24px 28px;
        margin-bottom: 24px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03), 0 8px 24px rgba(0, 0, 0, 0.02);
        transition: box-shadow 0.25s ease, border-color 0.25s ease;

        &:last-child {
            margin-bottom: 0;
        }

        &:hover {
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04), 0 12px 28px rgba(0, 0, 0, 0.03);
        }

        &.flex-card {
            display: flex;
            flex-direction: column;
            padding: 20px 24px;

            :deep(.el-form-item) {
                display: flex;
                flex-direction: column;
                margin-bottom: 0;
            }
        }
    }

    :deep(.el-form-item) {
        margin-bottom: 16px;

        &:last-child {
            margin-bottom: 0;
        }

        .el-form-item__label {
            padding-bottom: 8px;
            font-size: 13px;
            font-weight: 600;
            color: var(--text-primary);
            display: flex;
            align-items: center;
        }

        .el-form-item__label:before {
            margin-right: 4px;
        }
    }

    // 标题输入框专属大字号气度
    .title-input {
        :deep(.el-input__wrapper) {
            min-height: 46px;
            padding: 0 16px;
            border-radius: 8px !important;
            box-shadow: 0 0 0 1px var(--rule-strong) inset;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

            &:hover {
                box-shadow: 0 0 0 1px var(--text-tertiary) inset;
            }

            &.is-focus {
                box-shadow: 0 0 0 1px var(--accent) inset, 0 0 0 3px var(--accent-soft) !important;
            }
        }

        :deep(.el-input__inner) {
            font-size: 16px !important;
            font-weight: 600 !important;
            color: var(--text-primary) !important;
            letter-spacing: -0.01em;

            &::placeholder {
                font-weight: normal;
                font-size: 15px;
                color: var(--text-tertiary);
            }
        }
    }

    // 描述输入框
    .summary-input {
        :deep(.el-textarea__inner) {
            border-radius: 8px !important;
            border: 1px solid var(--rule-strong) !important;
            padding: 12px 16px;
            font-size: 14px;
            line-height: 1.6;
            color: var(--text-primary);
            resize: vertical;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

            &:hover {
                border-color: var(--text-tertiary) !important;
            }

            &:focus {
                border-color: var(--accent) !important;
                box-shadow: 0 0 0 3px var(--accent-soft) !important;
            }
        }
    }

    .title-cover-layout {
        display: flex;
        gap: 24px;
        align-items: flex-start;

        .title-section {
            flex: 1;
            min-width: 0;
        }

        .cover-section {
            width: 250px;
            flex-shrink: 0;
        }
    }

    // 封面上传区域微质感
    .cover-area {
        width: 100%;
        height: 156px;
        border-radius: 10px;
        overflow: hidden;
        cursor: pointer;
        position: relative;
        background: var(--hover-bg);
        border: 1.5px dashed var(--rule-strong);
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

        &:hover {
            border-color: var(--accent);
            background: var(--surface);
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
            transform: translateY(-1px);

            .cover-icon-wrap {
                transform: scale(1.08);
                color: var(--accent);
                background: var(--accent-soft);
            }
        }
    }

    .cover-placeholder {
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8px;
        color: var(--text-secondary);

        .cover-icon-wrap {
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: var(--surface);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            color: var(--text-secondary);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
            transition: all 0.2s ease;
        }

        span {
            font-size: 13px;
            font-weight: 500;
        }
    }

    .cover-preview {
        height: 100%;
        width: 100%;
        position: relative;

        img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            border-radius: 10px;
        }

        .cover-actions {
            position: absolute;
            top: 10px;
            right: 10px;
            opacity: 0;
            transition: opacity 0.2s ease, transform 0.2s ease;
            transform: scale(0.9);
        }

        &:hover .cover-actions {
            opacity: 1;
            transform: scale(1);
        }
    }
}

// 侧边栏设计重构
.editor-sidebar {
    width: 320px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 20px;

    .sidebar-section {
        background: var(--surface);
        border: 1px solid var(--rule);
        border-radius: 12px;
        padding: 20px 22px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03), 0 6px 18px rgba(0, 0, 0, 0.02);
        transition: box-shadow 0.25s ease;

        &:hover {
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 8px 22px rgba(0, 0, 0, 0.03);
        }

        .section-title {
            font-size: 14px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 14px;
            display: flex;
            align-items: center;
            gap: 8px;

            .title-icon {
                width: 24px;
                height: 24px;
                border-radius: 6px;
                background: var(--hover-bg);
                color: var(--accent);
                display: inline-flex;
                align-items: center;
                justify-content: center;
                font-size: 12px;
            }
        }

        :deep(.el-form-item) {
            margin-bottom: 0;
        }

        :deep(.el-select) {
            width: 100%;

            .el-select__wrapper {
                border-radius: 8px !important;
                min-height: 36px;
            }

            .el-tag {
                border-radius: 6px !important;
            }
        }

        :deep(.el-input) {
            .el-input__wrapper {
                border-radius: 8px !important;
            }
        }

        .setting-item {
            padding: 12px 0;
            border-bottom: 1px solid var(--rule);

            &:last-child {
                border-bottom: none;
                padding-bottom: 0;
            }

            &:first-child {
                padding-top: 0;
            }

            .setting-label {
                font-size: 13px;
                font-weight: 500;
                color: var(--text-secondary);
                margin-bottom: 8px;
            }

            .ai-btn {
                width: 100%;
                height: 34px;
                border-radius: 8px;
                font-size: 13px;
                font-weight: 500;
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 6px;
                transition: all 0.2s ease;

                i {
                    font-size: 12px;
                }
            }
        }
    }

    // 发布操作模块升级
    .action-section {
        .action-buttons {
            display: flex;
            flex-direction: column;
            gap: 10px;

            .primary-action-btn {
                width: 100%;
                height: 40px;
                border-radius: 8px;
                font-size: 14px;
                font-weight: 500;
                letter-spacing: 0.03em;
                background: var(--accent);
                border-color: var(--accent);
                color: #fff;
                box-shadow: 0 4px 14px rgba(168, 106, 16, 0.22);
                transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

                i {
                    margin-right: 6px;
                }

                &:hover {
                    filter: brightness(1.08);
                    box-shadow: 0 6px 18px rgba(168, 106, 16, 0.3);
                    transform: translateY(-1px);
                }

                &:active {
                    filter: brightness(0.95);
                    transform: translateY(0);
                }
            }

            .secondary-actions {
                display: flex;
                gap: 10px;

                .secondary-btn {
                    flex: 1;
                    height: 36px;
                    border-radius: 8px;
                    font-size: 13px;
                    font-weight: 500;
                    margin-left: 0;
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    gap: 4px;
                    transition: all 0.2s ease;

                    i {
                        font-size: 12px;
                    }

                    &:hover {
                        transform: translateY(-1px);
                    }
                }
            }
        }
    }
}

// Vditor 编辑器容器美化
.vditor {
    --border-color: var(--rule);
    background: var(--surface);
    border-radius: 10px !important;
    // 这里不能写 overflow: hidden 切圆角：工具栏图标的文字提示是
    // .vditor-tooltipped__n::after（vditor/dist/index.css 里 26 个按钮都用 __n，
    // 定位在按钮上方 bottom:100%），一旦被裁剪整条提示不显示，用户就只剩一排
    // 没有说明的图标。圆角由 border-radius 与工具栏自身圆角维持，
    // 顶层底色与工具栏同色（var(--surface)），不裁剪也不会在角上露出色块。
    //
    // overflow: visible 之后必须显式 min-width: 0：.vditor 是
    // .el-form-item__content（display: flex）的子项，flex 子项的 min-width: auto
    // 只在 overflow 非 visible 时才收缩到 0，改成 visible 后它的下限变成内容的
    // min-content 宽度（工具栏 26 个按钮 + 编辑区的宽表格），会把整个编辑器
    // 撑出内容卡片、盖住右侧栏。
    width: 100%;
    min-width: 0;
    border: 1px solid var(--rule) !important;
    transition: border-color 0.2s ease;

    :deep(.vditor-toolbar) {
        background-color: var(--surface) !important;
        border-bottom: 1px solid var(--rule) !important;
        padding: 6px 10px;
        border-radius: 9px 9px 0 0;
    }

    // 提示文本取自按钮的 aria-label（Vditor 自带中文），默认 11px 偏小
    :deep(.vditor-tooltipped::after) {
        font-size: 12px;
    }

    &:focus-within {
        border-color: var(--rule-strong) !important;
    }
}

// .vditor-reset 由 Vditor 自己创建，没有 scoped 的 data-v 属性，必须走 :deep 才能命中
:deep(.vditor-reset) {
    color: var(--text-primary);
}

@media screen and (max-width: 1200px) {
    .editor-main {
        padding: 20px;

        .el-form {
            flex-direction: column;
        }
    }

    .editor-sidebar {
        width: 100%;
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 16px;

        .action-section {
            grid-column: 1 / -1;
        }
    }
}

@media screen and (max-width: 768px) {
    .editor-main {
        padding: 12px 14px 32px;

        .el-form {
            gap: 16px;
        }
    }

    .editor-content {
        .content-card {
            padding: 16px;
            margin-bottom: 16px;
            border-radius: 10px;
        }

        .cover-area {
            height: 130px;
        }
    }

    .editor-sidebar {
        display: flex;
        flex-direction: column;
        gap: 14px;

        .sidebar-section {
            padding: 16px;
            border-radius: 10px;
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
</style>
