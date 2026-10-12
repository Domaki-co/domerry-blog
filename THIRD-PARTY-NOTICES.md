# 第三方组件许可声明（Third-Party Notices）

> **免责说明**：本文件由项目维护者**人工核对整理**，用于尽合理努力披露本项目所使用的第三方
> 组件及其许可。开源生态版本更迭频繁，若你发现遗漏、版本过时或标注错误，欢迎提 issue 指出，
> 我们会尽快修正。**本文件不构成法律意见**，也不改变任何第三方组件自身许可条款的效力；如需
> 正式的合规结论，请咨询你的法律顾问，并以各组件的官方 LICENSE/仓库为准。
>
> 本文件中前端组件的版本取自各子项目 `package.json` 的声明值（`blog-web` 另有
> `package-lock.json` 精确锁定，实际安装版本以 lock 文件为准）；后端组件的版本取自
> `blog/pom.xml` 与 `blog/domerry-*/pom.xml`（部分版本由 `spring-boot-starter-parent` 统一
> 管理）。**传递依赖未被逐条穷举**（例如 HikariCP、Lettuce、Netty 等），它们各自携带自己的
> 许可声明，完整清单可通过 `mvn dependency:tree` 或 `npm ls --all` 生成。

本项目许可为 Apache License 2.0（见 `LICENSE`），上游归属与修改声明见 `NOTICE`。

---

## 1. 后端（Java / Maven，Spring Boot 2.7.0，JDK 8）

| 名称 | 版本 | 许可 | 版权行 | 项目地址 |
| --- | --- | --- | --- | --- |
| Spring Boot（starter-web / starter-aop / starter-data-redis / starter-websocket / starter-mail / starter-cache / starter-parent） | 2.7.0 | Apache-2.0 | Copyright 2012-2022 the original author or authors（Pivotal Software, Inc. / VMware, Inc.） | https://spring.io/projects/spring-boot |
| MyBatis-Plus（mybatis-plus-boot-starter） | 3.5.2 | Apache-2.0 | Copyright (c) 2011-2023 baomidou | https://github.com/baomidou/mybatis-plus |
| dynamic-datasource-spring-boot-starter | 4.3.0 | Apache-2.0 | Copyright (c) 2018-2023 baomidou | https://github.com/baomidou/dynamic-datasource |
| Sa-Token（sa-token-spring-boot-starter、sa-token-redis-jackson） | 1.39.0 | Apache-2.0 | Copyright (c) 2019-present Dromara | https://github.com/dromara/Sa-Token |
| Project Lombok | 1.18.22 | MIT | Copyright (c) 2009-2021 The Project Lombok Authors | https://projectlombok.org |
| Hutool（hutool-all） | 5.8.26 | **Mulan PSL v2**（木兰宽松许可证，第 2 版；OSI 批准，非 copyleft） | Copyright (c) 2014-2023 looly | https://github.com/dromara/hutool |
| fastjson2 | 2.0.26 | Apache-2.0 | Copyright (c) 2019-2023 Alibaba Group | https://github.com/alibaba/fastjson2 |
| OSHI（oshi-core） | 6.4.0 | MIT | 版权归 The OSHI Project Contributors 所有 | https://github.com/oshi/oshi |
| JNA（net.java.dev.jna，由 OSHI 传递引入） | 5.13.0 | LGPL-2.1-or-later OR Apache-2.0 —— **本项目声明选择 Apache-2.0** | Copyright (c) 2007-2023 Timothy Wall and contributors | https://github.com/java-native-access/jna |
| Apache Commons IO | 2.11.0 | Apache-2.0 | Copyright 2002-2021 The Apache Software Foundation | https://commons.apache.org/proper/commons-io/ |
| Apache Commons Pool2 | 2.11.1 | Apache-2.0 | Copyright 2001-2021 The Apache Software Foundation | https://commons.apache.org/proper/commons-pool/ |
| Apache Velocity Engine Core | 2.3 | Apache-2.0 | Copyright 2000-2021 The Apache Software Foundation | https://velocity.apache.org/ |
| Quartz Scheduler | 2.3.2 | Apache-2.0 | Copyright (c) Terracotta, Inc.；Copyright 2001-2019 The Apache Software Foundation | https://www.quartz-scheduler.org/ |
| flexmark-java（flexmark-all） | 0.62.2 | **BSD-2-Clause** | Copyright (c) 2016 Vladimir Schneider | https://github.com/vsch/flexmark-java |
| jsoup | 1.14.3 | MIT | Copyright (c) 2009-2022 Jonathan Hedley | https://jsoup.org/ |
| ip2region（org.lionsoul:ip2region） | 2.7.0 | Apache-2.0 | Copyright (c) 2016 The ip2region Authors | https://github.com/lionsoul2014/ip2region |
| UserAgentUtils（eu.bitwalker） | 1.21 | **New BSD / BSD-3-Clause** | 版权归其作者所有（见组件随附 LICENSE） | https://github.com/HaraldWalker/user-agent-utils |
| Knife4j（knife4j-openapi2-spring-boot-starter） | 4.4.0 | Apache-2.0 | Copyright (c) 2017-2023 八一菜刀（xiaoymin） | https://github.com/xiaoymin/knife4j |
| JustAuth（me.zhyd.oauth） | 1.16.7 | MIT | Copyright (c) 2019-2023 yadong.zhang | https://github.com/justauth/JustAuth |
| weixin-java-tools（weixin-java-mp） | 3.4.0 | Apache-2.0 | Copyright (c) 2013-2022 Binary Wang | https://github.com/Wechat-Group/WxJava |
| jasypt-spring-boot-starter | 3.0.5 | MIT | Copyright (c) 2019 Ulises Bocchio | https://github.com/ulisesbocchio/jasypt-spring-boot |
| x-file-storage（x-file-storage-spring） | 2.2.1 | Apache-2.0 | Copyright (c) 2022-2023 Dromara | https://github.com/dromara/x-file-storage |
| MinIO Java SDK（io.minio:minio） | 8.0.3（`domerry-commom`）与 8.5.2（`domerry-file`） | Apache-2.0 | Copyright (c) 2015-2023 MinIO, Inc. | https://github.com/minio/minio-java |
| 七牛 Java SDK（qiniu-java-sdk） | 7.12.1 | MIT | 版权归 Qiniu 所有（见组件随附 LICENSE） | https://github.com/qiniu/java-sdk |
| 阿里云 OSS SDK（aliyun-sdk-oss） | 3.16.1 | Apache-2.0 | Copyright (c) 2009-2023 Alibaba Cloud | https://github.com/aliyun/aliyun-oss-java-sdk |
| 腾讯云 COS SDK（cos_api） | 5.6.137 | MIT | 版权归 Tencent 所有（见项目仓库 LICENSE） | https://github.com/tencentyun/cos-java-sdk-v5 |
| 火山引擎 Java SDK（volcengine-java-sdk-ark-runtime，方舟大模型） | 2.0.28 | Apache-2.0 | 版权归北京火山引擎科技有限公司所有 | https://github.com/volcengine/volc-sdk-java |
| MySQL Connector/J（mysql:mysql-connector-java） | 8.0.29 | **GPL-2.0-only WITH Universal-FOSS-Exception-1.0** | Copyright (c) 2000-2022 Oracle and/or its affiliates | https://github.com/mysql/mysql-connector-j |

### 1.1 关于 MySQL Connector/J 的特别说明

`mysql:mysql-connector-java` 采用 `GPL-2.0-only WITH Universal-FOSS-Exception-1.0`：

- 本项目**只分发源码**，该 JDBC 驱动由 Maven 从 Maven Central 解析下载，**不由本项目再分发**
  其二进制或源码。
- 若你在分发本项目的构建产物（例如打包好的 jar 或容器镜像）时一并分发了该驱动，则须一并
  附上 **GNU General Public License v2** 全文与 **Universal FOSS Exception, Version 1.0** 全文
  （或指向 <https://oss.oracle.com/licenses/universal-foss-exception/> 与
  <https://www.gnu.org/licenses/old-licenses/gpl-2.0.html> 的链接），并提供对应源码：
  该版本（8.0.29）所在仓库的对应提交为 `dd61577595edad45c398af508cf91ad26fc4144f`。
- 若你不希望受此约束，可改用其它许可更宽松的 MySQL/MariaDB JDBC 驱动（例如
  MariaDB Connector/J，LGPL-2.1）。

### 1.2 关于 flexmark / openhtmltopdf（已排除 LGPL 组件）

`com.vladsch.flexmark:flexmark-all` 自身为 **BSD-2-Clause**。它会传递引入
`com.openhtmltopdf:*`（**LGPL-2.1-or-later**）——该链路只用于 flexmark 的 Markdown→PDF 转换，
本项目**只用 Markdown→HTML**，因此已在 `blog/domerry-admin/pom.xml` 中通过 `<exclusions>`
显式排除 `flexmark-pdf-converter` 与 `com.openhtmltopdf:{core, pdfbox, rtl-support,
jsoup-dom-converter, slf4j, log4j}`，不将其纳入构建与分发。

若你修改依赖配置、取消上述排除，则 `com.openhtmltopdf` 组件的 LGPL-2.1-or-later 条款将适用，
你需要自行遵守（例如保留许可声明、提供可替换该库的方式或对应源码）。

---

## 2. 前端门户（`blog-web`，Vue 2.7 + element-ui + Vite）

### 2.1 运行时依赖

| 名称 | 版本 | 许可 | 版权行 | 项目地址 |
| --- | --- | --- | --- | --- |
| Vue | 2.7.16 | MIT | Copyright (c) 2013-present Evan You | https://github.com/vuejs/vue |
| vue-router | 3.6.5 | MIT | Copyright (c) 2013-present Evan You | https://github.com/vuejs/vue-router |
| Vuex | 3.6.2 | MIT | Copyright (c) 2013-present Evan You | https://github.com/vuejs/vuex |
| element-ui | 2.15.14 | MIT | Copyright (c) 2016-present ElemeFE | https://github.com/ElemeFE/element |
| axios | 1.6.7 | MIT | Copyright (c) 2014-present Matt Zabriskie & Collaborators | https://github.com/axios/axios |
| marked | 15.0.7 | MIT | Copyright (c) 2018+ MarkedJS；Copyright (c) 2011-2018 Christopher Jeffrey | https://github.com/markedjs/marked |
| DOMPurify | 3.4.16 | (MPL-2.0 OR Apache-2.0) —— **本项目声明选择 Apache-2.0** | Copyright (c) Cure53 and other contributors | https://github.com/cure53/DOMPurify |
| GSAP | 3.12.5 | **GSAP Standard License（专有许可，非开源；no-charge 免费使用）** | Copyright (c) Webflow, Inc.（GreenSock） | https://gsap.com/standard-license |
| highlight.js | 11.11.0 | BSD-3-Clause | Copyright (c) 2006 Ivan Sagalaev；Copyright (c) 2006-2023 Josh Goebel 及贡献者 | https://highlightjs.org/ |
| js-cookie | 3.0.5 | MIT | Copyright (c) 2018 Klaus Hartl & Fagner Brack | https://github.com/js-cookie/js-cookie |
| APlayer | 1.10.1 | MIT | Copyright (c) DIYgod | https://github.com/DIYgod/APlayer |
| mavon-editor | 2.10.4 | MIT | Copyright (c) 2018 hinesboy | https://github.com/hinesboy/mavonEditor |
| Vditor | 3.11.3 | MIT | Copyright (c) 2020-present Vanessa（B3log 社区） | https://github.com/Vanessa219/vditor |
| vue-cropper | 0.6.5 | **ISC** | Copyright (c) xyxiao001 | https://github.com/xyxiao001/vue-cropper |
| vue-danmaku | 1.7.2 | MIT | Copyright (c) hellodigua | https://github.com/hellodigua/vue-danmaku |
| vue-lazyload | 1.3.3 | MIT | Copyright (c) hilongjw | https://github.com/hilongjw/vue-lazyload |
| animate.css | 4.1.1 | MIT | Copyright (c) Daniel Eden | https://animate.style/ |
| fast-glob | 3.3.3 | MIT | Copyright (c) Denis Malinochkin | https://github.com/mrmlnc/fast-glob |
| vite-plugin-svg-icons | 2.0.1 | MIT | Copyright (c) Vben | https://github.com/vbenjs/vite-plugin-svg-icons |

### 2.2 构建期依赖（devDependencies）

| 名称 | 版本 | 许可 | 版权行 | 项目地址 |
| --- | --- | --- | --- | --- |
| Vite | 5.1.4 | MIT | Copyright (c) 2019-present Evan You & Vite Contributors | https://github.com/vitejs/vite |
| @vitejs/plugin-vue2 | 2.3.1 | MIT | Copyright (c) 2019-present Evan You & Vite Contributors | https://github.com/vitejs/vite-plugin-vue |
| Sass（dart-sass） | 1.71.1 | MIT | Copyright (c) 2016-present Google Inc. | https://github.com/sass/dart-sass |

### 2.3 关于 GSAP 的特别说明

GSAP 采用 **GSAP Standard License**（GreenSock 自有的专有许可，**不是** MIT / Apache-2.0 / 任何
开源许可），其「no charge（免费）」条件适用于大多数普通使用场景。本项目**未向终端用户收取
任何费用**，符合该许可的 no-charge 条件。若你基于本项目构建需要收费的产品或服务，或需移除
许可中限制的商业用途，请自行确认并遵守 <https://gsap.com/standard-license>
（或购买相应商业许可）。你不希望受此约束时，可移除 GSAP 相关动画代码并改用其它许可的动画库。

---

## 3. 前端后台（`blog-admin`，Vue 3 + element-plus + Pinia + Vite）

### 3.1 运行时依赖

| 名称 | 版本 | 许可 | 版权行 | 项目地址 |
| --- | --- | --- | --- | --- |
| Vue | 3.2.47 | MIT | Copyright (c) 2018-present Evan You | https://github.com/vuejs/core |
| vue-router | 4.1.6 | MIT | Copyright (c) 2014-present Evan You | https://github.com/vuejs/router |
| Pinia | 2.0.33 | MIT | Copyright (c) 2019-present Eduardo San Martin Morote | https://github.com/vuejs/pinia |
| pinia-plugin-persistedstate | 3.2.3 | MIT | Copyright (c) prazdevs | https://github.com/prazdevs/pinia-plugin-persistedstate |
| element-plus | 2.3.0 | MIT | Copyright (c) 2020-present Element Plus | https://github.com/element-plus/element-plus |
| @element-plus/icons-vue | 2.1.0 | MIT | Copyright (c) 2020-present Element Plus | https://github.com/element-plus/element-plus-icons |
| axios | 1.3.4 | MIT | Copyright (c) 2014-present Matt Zabriskie & Collaborators | https://github.com/axios/axios |
| Apache ECharts | 5.5.1 | Apache-2.0 | Copyright 2017-2022 The Apache Software Foundation | https://echarts.apache.org/ |
| @wangeditor/editor-for-vue | 5.1.12 | MIT | Copyright (c) 2021-present wangEditor | https://github.com/wangeditor-team/wangEditor-for-Vue |
| Vditor | 3.11.3 | MIT | Copyright (c) 2020-present Vanessa（B3log 社区） | https://github.com/Vanessa219/vditor |
| highlight.js | 11.10.0 | BSD-3-Clause | Copyright (c) 2006 Ivan Sagalaev；Copyright (c) 2006-2023 Josh Goebel 及贡献者 | https://highlightjs.org/ |
| js-cookie | 3.0.5 | MIT | Copyright (c) 2018 Klaus Hartl & Fagner Brack | https://github.com/js-cookie/js-cookie |
| NProgress | 0.2.0 | MIT | Copyright (c) 2013-2014 Rico Sta. Cruz | https://github.com/rstacruz/nprogress |
| screenfull | 6.0.2 | MIT | Copyright (c) Sindre Sorhus | https://github.com/sindresorhus/screenfull.js |
| file-saver | 2.0.5 | MIT | Copyright (c) 2015-2018 Eli Grey | https://github.com/eligrey/FileSaver.js |
| svg-sprite-loader | 6.0.11 | MIT | Copyright (c) kisenka | https://github.com/kisenka/svg-sprite-loader |
| cron-parser | 4.9.0 | MIT | Copyright (c) Harri Siirak | https://github.com/harrisiirak/cron-parser |

### 3.2 构建期依赖（devDependencies）

| 名称 | 版本 | 许可 | 版权行 | 项目地址 |
| --- | --- | --- | --- | --- |
| Vite | 4.2.0 | MIT | Copyright (c) 2019-present Evan You & Vite Contributors | https://github.com/vitejs/vite |
| @vitejs/plugin-vue | 4.1.0 | MIT | Copyright (c) 2019-present Evan You & Vite Contributors | https://github.com/vitejs/vite-plugin-vue |
| vite-svg-loader | 5.1.0 | MIT | Copyright (c) Jan-Paul Kleemans | https://github.com/jpkleemans/vite-svg-loader |
| Sass（dart-sass） | 1.71.0 | MIT | Copyright (c) 2016-present Google Inc. | https://github.com/sass/dart-sass |
| TypeScript | 5.4.5 | Apache-2.0 | Copyright (c) Microsoft Corporation | https://github.com/microsoft/TypeScript |
| unplugin-auto-import | 0.18.4 | MIT | Copyright (c) Anthony Fu | https://github.com/unplugin/unplugin-auto-import |
| vue-tsc | 1.2.0 | MIT | Copyright (c) 2021-present Johnson Chu | https://github.com/vuejs/language-tools |
| @types/node、@types/js-cookie、@types/file-saver、@types/vue-router | 见 `package.json` | MIT | Copyright (c) Microsoft Corporation（DefinitelyTyped 贡献者） | https://github.com/DefinitelyTyped/DefinitelyTyped |

---

## 4. 字体

本项目**不包含**任何商业字体文件（字体文件均未随仓库分发）。前端界面统一使用**操作系统自带的
系统字体栈**（`-apple-system`、`BlinkMacSystemFont`、`Segoe UI`、`PingFang SC`、
`Microsoft YaHei` 等），由用户本地系统提供，不涉及字体授权与再分发。

若你希望使用自定义展示字体，请**自行确认该字体的商用授权**（例如思源黑体/思源宋体为
SIL OFL 1.1，可自由商用；而汉仪、方正等厂商字体通常需要购买授权），并自行承担相应责任与
字体文件的再分发合规性。

---

## 5. 未随仓库分发的资产

以下上游资产已在清理中移除，**不包含**在本仓库内：

- 上游品牌 Logo 文件（根目录 `logo.ico`）；
- 上游站点截图（`statics/` 目录下的全部 PNG 图片）；
- 微信小程序端 `uniapp-blog/`（整包删除）；
- 未获商用授权的商业字体文件；
- 含个人信息的二维码图片（登录页二维码图片为项目自带的**中性占位图**，请替换为你自己的素材）。

---

## 6. 第三方商标与图标

以下资源位于 `blog-web/src/assets/icons/`，是第三方服务的**商标 / 品牌标识**：
`github.svg`、`gitee.svg`、`baidu.svg`、`chrome.svg`、`zip.svg`、`pdf.svg`、`doc.svg`、
`excel.svg`。

本项目仅在**指名性用途（nominative use）**中使用它们，即：说明「该图标所指向的第三方服务」
（GitHub / Gitee / 百度等登录方式）或「该图标所代表的文件格式」（ZIP / PDF / DOC / XLSX），
以便用户识别。本项目**不主张**对上述商标的任何权利，也不代表上述权利人与本项目之间存在
任何授权、合作、赞助或背书关系。相关商标归各自所有者所有；如权利人认为使用方式不当，
请通过 `SECURITY.md` / issue 联系我们，我们会及时移除或调整。

同样的原则适用于 `blog-web/src/App.vue` 通过 CDN 引入的 Font Awesome 图标字体（见
`README.md` 的「第三方资源与外部接口」一节）。

---

## 7. 运行时使用的外部服务（需自备账号或自行托管）

| 服务 | 用途 | 说明 |
| --- | --- | --- |
| `cdnjs.cloudflare.com`（Font Awesome 5.15.4） | 前端图标字体（`blog-web/src/App.vue` 通过 `@import` 引入） | 第三方公开 CDN，不含任何凭证；离线部署需自行托管 |
| `baidubox-emoji.cdn.bcebos.com`（百度 emoji） | 前端评论/说说表情图片（`blog-web/src/assets/emoji.json`） | 第三方公开 CDN，不含任何凭证；离线部署需自行托管或替换为本地表情图 |
| `coderutil.com` 随机图 API | 后端随机封面/音视频占位图 | 第三方提供的公开接口，可用性不受本项目控制，可替换为本地图库 |
| 火山引擎方舟（`ark.cn-beijing.volces.com`） | AI 文章摘要 / 对话 | **需自备 API Key** |
| SMTP 邮件服务 | 邮件通知（评论、友链、找回密码） | **需自备账号** |
| MinIO / 七牛 / 阿里云 OSS / 腾讯云 COS | 对象存储 | **需自备账号与密钥**，可只启用其中一种 |
| QQ / 微博 / Gitee / GitHub OAuth | 第三方登录 | **需自备 app-id / app-secret** |

以上服务的具体配置项见 `README.md` 的「配置说明」与各 `.env.example` 文件。
