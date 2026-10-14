<template>
  <div class="not-found">
    <div class="not-found-content">
      <p class="code">404</p>
      <h1>页面不存在</h1>
      <p class="hint">这个地址没有对应的内容，也许它已经被移走或改名了。</p>
      <router-link to="/" class="back-home">
        <i class="fas fa-arrow-left"></i>
        返回首页
      </router-link>
    </div>
  </div>
</template>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  404 · 技术台账
//
//  大号等宽「404」当版面主体，下面一条实线，再是说明和返回入口。
//  不用彩色描边、不用胶囊按钮、不做上浮。
// ════════════════════════════════════════════════════════════════

.not-found {
  min-height: calc(100vh - 140px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $spacing-xl;
  background: var(--background);
}

.not-found-content {
  width: 100%;
  max-width: 460px;

  .code {
    font-family: $font-mono;
    font-variant-numeric: tabular-nums;
    font-size: clamp(4.5rem, 14vw, 7.5rem);
    line-height: 1;
    letter-spacing: 0.02em;
    color: var(--text-primary);
    margin: 0 0 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid var(--rule-strong);
  }

  h1 {
    font-family: $font-serif;
    font-size: 1.3rem;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 10px;
  }

  .hint {
    color: var(--text-secondary);
    font-size: 0.92rem;
    line-height: 1.7;
    margin: 0 0 28px;
  }

  .back-home {
    @include ledger-button;
    text-decoration: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .back-home {
    transition: none !important;
  }
}
</style>
