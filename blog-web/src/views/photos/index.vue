<template>
  <div class="photos-container">
    <header class="photos-header">
      <h1>相册</h1>
      <span class="head-meta">{{ albums.length }} 个相册</span>
    </header>
    <div class="photos-grid">
      <div v-for="(album, index) in albums" :key="index" class="album-card" @click="openAlbum(album)">
        <div class="album-cover">
          <img v-lazy="album.cover" :key="album.cover" :alt="album.name">
          <div v-if="album.isLock === 1" class="lock-icon">
            <i class="fas fa-lock"></i>
          </div>
        </div>
        <div class="album-info">
          <div class="album-line">
            <h3>{{ album.name }}</h3>
            <span class="photo-count">{{ album.photoNum }} 张</span>
          </div>
          <p v-if="album.description">{{ album.description }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getAlbumListApi } from '@/api/album'

export default {
  name: 'Photos',
  data() {
    return {
      albums: [],
    }
  },
  created() {
    this.getAlbumList()
  },
  methods: {
    /**
     * 获取相册列表
     */
    getAlbumList() {
      getAlbumListApi().then(res => {
        this.albums = res.data
      })
    },
    /**
     * 打开相册详情
     */
    openAlbum(album) {
      this.$router.push({
        name: 'PhotoDetail',
        params: { id: album.id }
      })
    },
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  相册 · 技术台账
//
//  封面 + 一条 1px 底边，名称与「N 张」等宽数字对齐。
//  没有上浮、没有阴影、没有装饰圆。
// ════════════════════════════════════════════════════════════════

.photos-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: clamp(24px, 4vh, 44px) clamp(20px, 4vw, 48px);
}

.photos-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: $spacing-md;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--rule-strong);
  margin-bottom: clamp(28px, 4vh, 44px);

  h1 {
    font-family: $font-serif;
    font-size: clamp(1.6rem, 3vw, 2.1rem);
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  .head-meta {
    @include ledger-meta;
  }
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: clamp(24px, 3vw, 40px);
}

.album-card {
  cursor: pointer;

  &:hover {
    .album-cover img {
      filter: saturate(1);
    }

    .album-info h3 {
      color: var(--accent);
    }
  }

  .album-cover {
    position: relative;
    aspect-ratio: 4 / 3;
    overflow: hidden;
    border: 1px solid var(--rule);
    border-radius: $border-radius-sm;
    background: var(--row-hover);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      filter: saturate(0.9);
      transition: filter 0.3s ease;
    }

    .lock-icon {
      position: absolute;
      top: 8px;
      right: 8px;
      width: 26px;
      height: 26px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(18, 23, 29, 0.6);
      color: #fff;
      font-size: 12px;
      border-radius: $border-radius-sm;
    }
  }

  .album-info {
    padding: 12px 2px;
    border-bottom: 1px solid var(--rule);

    .album-line {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: 12px;
    }

    h3 {
      font-size: 1rem;
      font-weight: 500;
      color: var(--text-primary);
      margin: 0;
      transition: color 0.2s ease;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    p {
      color: var(--text-secondary);
      font-size: 0.84rem;
      line-height: 1.55;
      margin: 6px 0 0;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .photo-count {
      @include ledger-meta;
      flex: none;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .album-card img,
  .album-card h3 {
    transition: none !important;
  }
}
</style> 