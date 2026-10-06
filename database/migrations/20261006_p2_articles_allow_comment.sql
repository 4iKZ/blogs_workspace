-- 2026-10-06 P3-5：articles 表补 allow_comment 列，落地文章级"允许评论"开关。
-- 背景：前端 UI 与 ArticleCreateDTO/ArticleDTO 早有该字段，但 articles 表从未有对应列
-- （allow_comment 此前只存在于 article_moderation_submissions 审核快照表），
-- 故 Article.allowComment 原为 @TableField(exist=false)，开关从未生效。本次补列并建立映射。
ALTER TABLE articles
    ADD COLUMN allow_comment tinyint DEFAULT 1 COMMENT '是否允许评论：0-不允许，1-允许';
