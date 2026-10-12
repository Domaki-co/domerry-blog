import request from '@/utils/request'

/**
 * 获取文章详情
 */
export function getArticleDetailApi(id) {
  return request({
    url: `/api/article/detail/${id}`,
    method: 'get'
  })
} 

/**
 * 获取文章列表
 */
export function getArticlesApi(params) {
  return request({
      url: '/api/article/list',
      method: 'get',
      params: params
  })
}

/**
 * 获取评论列表
 */
export function getCommentsApi(params) {
    return request({
      url: `/api/comment/list`,
      method: 'get',
      params
    })
} 

/**
 * 获取归档列表
 */
export function getArchivesApi() {
  return request({
      url: '/api/article/archive',
      method: 'get'
  })
}

/**
 * 获取分类
 */
export function getCategoriesApi() {
  return request({
    url: '/api/article/categories',
    method: 'get'
  })
}

/**
 * 获取所有分类
 */
export function getAllCategoriesApi() {
  return request({
    url: '/api/article/categorie-all',
    method: 'get'
  })
}



/**
 * 添加评论
 */
export function addCommentApi(data) {
    return request({
        url: '/api/comment/add',
        method: 'post',
        data
    })
}

/**
 * 搜索文章
 */
export function searchArticlesApi(keyword) {
  return request({
    url: '/api/article/search',
    method: 'get',
    params: {
      keyword,
      limit: 10
    }
  })
}

/**
 * 获取轮播文章
 */
export function getCarouselArticlesApi() {
  return request({
    url: '/api/article/getCarousels',
    method: 'get',
  })
}

/**
 * 获取推荐文章
 */
export function getRecommendArticlesApi() {
  return request({
    url: '/api/article/getRecommends',
    method: 'get',
  })
}

/**
 * 获取置顶文章（首页顶部快捷入口，后端最多返回5条）
 */
export function getStickArticlesApi() {
  return request({
    url: '/api/article/getSticks',
    method: 'get',
  })
}

/**
 * 点赞
 */
export function likeArticleApi(id) {
  return request({
    url: `/api/article/like/${id}`,
    method: 'get'
  })
}

/**
 * 获取的文章
 */
export function getMyArticleApi(params) {
  return request({
      url: '/protal/user/myArticle',
      method: 'get',
      params
  })
}

/**
 * 删除文章
 */
export function delArticleApi(id) {
  return request({
      url: `/sys/article/delete/${id}`,
      method: 'delete'
  })
}

/**
 * 创建文章
 * @param {Object} data 文章数据
 */
export function createArticleApi(data) {
  return request({
    url: '/sys/article/add',
    method: 'post',
    data
  })
}

/**
 * 获取文章详情
 */
export function getArticleInfoApi(id) {
  return request({
    url: `/sys/article/detail/${id}`,
    method: 'get'
  })
}

/**
 * 更新文章
 * @param {Object} data 文章数据
 */
export function updateArticleApi(data) {
  return request({
    url: `/sys/article/update`,
    method: 'put',
    data
  })
}

/**
 * 变更我的文章状态
 * @param {Number} id 文章id
 * @param {Number} status 目标状态 0：下架 1：发布 3：审核
 */
export function changeArticleStatusApi(id, status) {
  return request({
    url: `/protal/article/changeStatus/${id}/${status}`,
    method: 'put'
  })
}

/**
 * 变更文章状态（后台接口，仅管理员可用）
 * 后端用 MyBatis-Plus updateById 做部分更新，只会写入传入的非空字段，
 * 所以置顶只提交 isStick，不会动到 status 等其它字段。
 * @param {Object} data { id, isStick } 或 { id, status }
 */
export function updateArticleStatusApi(data) {
  return request({
    url: '/sys/article/updateStatus',
    method: 'put',
    data
  })
}
