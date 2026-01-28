# 静态资源目录说明

这个目录包含网站所需的静态资源文件。

## 目录结构

- `/images` - 图片资源
  - `/icons` - 图标文件
  - `/theme` - 主题相关图片
- `/css` - 样式表文件
- `/js` - JavaScript 文件
- `/fonts` - 字体文件
- `/videos` - 视频文件
- `/docs` - 文档文件
- `/uploads` - 用户上传文件
- `/temp` - 临时文件
- `/vendor` - 第三方库文件

## 使用说明

将静态资源文件放置在对应的目录中，可以通过以下方式引用：

```html
<!-- 在HTML中引用 -->
<img src="/images/logo.svg" alt="Logo">

<!-- 在CSS中引用 -->
background-image: url('/images/theme/background.jpg');

<!-- 在JavaScript中引用 -->
import '/css/custom.css';
```

## 注意事项

1. 所有路径都是相对于网站根目录的
2. 文件名应使用小写字母和连字符命名
3. 推荐使用现代格式（如 WebP 图片、WOFF2 字体等）
4. 注意文件大小优化，特别是图片和视频文件