export default function devUtilityPlugin() {
  return {
    name: 'dev-utility',
    transformIndexHtml(html, context) {
      if (context.server) {
        return html.replace('<script src="/static/js/index.js"></script>', '')
      }
      return html
    }
  }
}
