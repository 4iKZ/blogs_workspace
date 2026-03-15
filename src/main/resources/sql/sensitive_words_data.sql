-- 敏感词数据导入脚本
-- 执行前请先确保 sensitive_words 表已创建

-- 清空原有的测试数据
DELETE FROM sensitive_words WHERE word IN ('敏感词1', '敏感词2', '敏感词3');

-- 插入真实敏感词数据（辱骂类）
INSERT INTO sensitive_words (word, category, level) VALUES
('傻逼', 'insult', 2),
('傻B', 'insult', 2),
('煞笔', 'insult', 2),
('煞逼', 'insult', 2),
('草泥马', 'insult', 2),
('操你妈', 'insult', 2),
('妈的', 'insult', 1),
('他妈的', 'insult', 1),
('狗日的', 'insult', 2),
('王八蛋', 'insult', 2),
('畜生', 'insult', 1),
('变态', 'insult', 1),
('去死', 'insult', 1),
('滚蛋', 'insult', 1),
('混蛋', 'insult', 2),
('贱人', 'insult', 2),
('婊子', 'insult', 2),
('垃圾', 'insult', 1),
('废物', 'insult', 1),
('白痴', 'insult', 1),
('白目', 'insult', 1),
('脑残', 'insult', 2),
('智障', 'insult', 2),
('弱智', 'insult', 1),
('傻缺', 'insult', 1),
('二逼', 'insult', 2),
('二B', 'insult', 2);

-- 插入真实敏感词数据（色情类）
INSERT INTO sensitive_words (word, category, level) VALUES
('屁眼', 'porn', 2),
('肛门', 'porn', 2),
('强奸', 'porn', 2),
('轮奸', 'porn', 2),
('乱伦', 'porn', 2),
('鸡奸', 'porn', 2),
('卖淫', 'porn', 2),
('嫖娼', 'porn', 2),
('妓女', 'porn', 2),
('婊', 'porn', 2),
('啪啪啪', 'porn', 2),
('约炮', 'porn', 2),
('一夜情', 'porn', 2),
('出轨', 'porn', 1),
('小三', 'porn', 1),
('二奶', 'porn', 2),
('龟头', 'porn', 2),
('阴道', 'porn', 2),
('乳房', 'porn', 2),
('裸体', 'porn', 2),
('脱衣', 'porn', 2),
('艳照', 'porn', 2),
('偷拍', 'porn', 2),
('走光', 'porn', 1),
('露点', 'porn', 2),
('AV女优', 'porn', 2),
('黄色网站', 'porn', 2);

-- 插入真实敏感词数据（违法犯罪类）
INSERT INTO sensitive_words (word, category, level) VALUES
('赌博', 'crime', 2),
('博彩', 'crime', 2),
('六合彩', 'crime', 2),
('地下钱庄', 'crime', 2),
('高利贷', 'crime', 2),
('传销', 'crime', 2),
('诈骗', 'crime', 2),
('非法集资', 'crime', 2),
('贩毒', 'crime', 2),
('吸毒', 'crime', 2),
('摇头丸', 'crime', 2),
('冰毒', 'crime', 2),
('海洛因', 'crime', 2),
('大麻', 'crime', 2),
('毒品', 'crime', 2),
('枪支', 'crime', 2),
('弹药', 'crime', 2),
('爆炸物', 'crime', 2);

-- 插入真实敏感词数据（政治敏感类）
INSERT INTO sensitive_words (word, category, level) VALUES
('恐怖袭击', 'politics', 2),
('恐怖分子', 'politics', 2),
('基地组织', 'politics', 2),
('ISIS', 'politics', 2),
('塔利班', 'politics', 2),
('杀人工厂', 'politics', 2),
('人体炸弹', 'politics', 2),
('自杀式袭击', 'politics', 2),
('法轮功', 'politics', 2),
('邪教', 'politics', 2),
('反华', 'politics', 2),
('分裂国家', 'politics', 2),
('台独', 'politics', 2),
('藏独', 'politics', 2),
('疆独', 'politics', 2),
('港独', 'politics', 2),
('FLG', 'politics', 2);

-- 插入真实敏感词数据（其他类）
INSERT INTO sensitive_words (word, category, level) VALUES
('GFW', 'other', 1),
('防火墙', 'other', 1),
('敏感词', 'other', 1),
('审查', 'other', 1),
('封杀', 'other', 1),
('禁言', 'other', 1),
('删帖', 'other', 1),
('跨省', 'other', 2),
('维稳', 'other', 1),
('上访', 'other', 1),
('信访', 'other', 1),
('游行', 'other', 1),
('示威', 'other', 1),
('罢工', 'other', 1),
('暴动', 'other', 2),
('骚乱', 'other', 2),
('打砸抢', 'other', 2),
('抗法', 'other', 2),
('袭警', 'other', 2);