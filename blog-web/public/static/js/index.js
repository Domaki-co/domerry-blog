var vm;
var originalTitle = document.title;

document.addEventListener("visibilitychange", () => {
    if (document.hidden) {
        //离开此标签页
        document.title = "不要走吗o(>﹏<)o"
    } else {
        //进入此标签页
        document.title = "咦~ 你又回来了O(∩_∩)O哈哈~"

        window.setTimeout(() => {
            document.title = originalTitle
        }, 800)
    }
}, false);



// 禁用右键菜单
document.addEventListener('contextmenu',function(e){
    e.preventDefault();  // 阻止默认事件
});
// 禁止键盘F12键
document.addEventListener('keydown',function(e){
    if(e.key == 'F12'){
        e.preventDefault(); // 如果按下键F12,阻止事件
    }
});

/* 样式代码 */
var styleTitle1 = `
font-size: 20px;
font-weight: 600;
color: rgb(244,167,89);
`
var styleTitle2 = `
font-style: oblique;
font-size:14px;
color: rgb(244,167,89);
font-weight: 400;
`
var styleContent = `
color: rgb(30,152,255);
`

/* 内容代码 */
var title1 = '🌒 拾壹博客 '

// => 读取配置型(在配置文件里配置这些会变动的网址)
var offiUrl = 'https://www.shiyit.com/'
var content = `
版 本 号：1.0
编译日期：${new Date()}
站点介绍：
1. 一个想当全栈程序员的个人博客.
🏠官网:  ${offiUrl}
`
// console.log(`%c${title1}
// %c${content}`, styleTitle1, styleContent)
