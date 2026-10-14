<template>
  <div class="about-page">
    <div class="about-content" v-html="$sanitize($store.state.webSiteInfo.aboutMe)" ref="content">
    </div>
    <mj-image-preview ref="imagePreview" />
  </div>
</template>

<script>

export default {
  name: 'About',

  mounted() {
    this.initImagePreview();
  },
  methods: {
    initImagePreview() {
      this.$nextTick(() => {
        setTimeout(() => {  
        const content = this.$refs.content;
        if (content) {
          const images = content.getElementsByTagName('img');
          Array.from(images).forEach(img => {
            img.style.cursor = 'zoom-in';
            img.addEventListener('click', () => {
              this.$refs.imagePreview.show(img.src);
            });
          });
        }
      }, 500); 
      });
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  关于 · 技术台账
//
//  正文是一栏可读的窄版心（<80 字符），行高放宽；不再套带阴影的卡片。
// ════════════════════════════════════════════════════════════════

.about-page {
  max-width: 760px;
  margin: 0 auto;
  padding: clamp(28px, 5vh, 56px) clamp(20px, 4vw, 32px) $spacing-xl;

  @include responsive(lg) {
    padding: clamp(24px, 4vh, 40px) $spacing-md $spacing-lg;
  }
}

.about-content {
  line-height: 1.85;
  color: var(--text-primary);
  font-size: 0.98rem;

  :deep(h1), :deep(h2), :deep(h3) {
    font-family: $font-serif;
    color: var(--text-primary);
    margin: 1.6em 0 0.6em;
  }

  :deep(a) {
    color: var(--accent);
  }

  :deep(img) {
    border-radius: $border-radius-sm;
    border: 1px solid var(--rule);
  }
}

@include responsive(sm) {
  :deep(img) {
    width: 100% !important;
  }
}
</style>
