-- 专题表：首页/经藏导航的运营分类
CREATE TABLE IF NOT EXISTS sutra_topic (
  id       INT UNSIGNED NOT NULL AUTO_INCREMENT,
  code     VARCHAR(32)  NOT NULL COMMENT '英文标识，客户端图标/路由用',
  name     VARCHAR(32)  NOT NULL COMMENT '专题名',
  slogan   VARCHAR(64)  NULL     COMMENT '一句话标语，Banner 用',
  intro    VARCHAR(255) NULL     COMMENT '列表页简介',
  icon     VARCHAR(32)  NULL     COMMENT '图标名，对应客户端资源',
  sort_no  SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  status   TINYINT      NOT NULL DEFAULT 1 COMMENT '1=显示 0=隐藏',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='佛经专题';

-- 经与专题的多对多；is_primary=1 表示主归属（每部经只有一个）
CREATE TABLE IF NOT EXISTS sutra_topic_map (
  sutra_id   BIGINT UNSIGNED NOT NULL,
  topic_id   INT UNSIGNED    NOT NULL,
  is_primary TINYINT         NOT NULL DEFAULT 0 COMMENT '1=主归属 0=关联推荐',
  sort_no    SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '专题内排序',
  PRIMARY KEY (sutra_id, topic_id),
  KEY idx_topic (topic_id, is_primary, sort_no),
  CONSTRAINT fk_map_sutra FOREIGN KEY (sutra_id) REFERENCES sutra (id) ON DELETE CASCADE,
  CONSTRAINT fk_map_topic FOREIGN KEY (topic_id) REFERENCES sutra_topic (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='经-专题关联';

-- 种子：专题
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('rudao','入门必读','一炷香，读完一部经','最短的四部经，加起来不到六千字。先读完，再谈修行。','lotus',1) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('bore','般若空慧','照见五蕴皆空','般若系的核心，讲“空”不讲“有”。想弄明白佛家到底在破什么，从这里进。','moon',2) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('yuanli','愿力救度','有求必应，有愿必成','民间信仰最深的四部。消灾、延寿、超度、护国，都在这条线上。','hands',3) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('jingtu','净土归心','一句佛号，往生极乐','三经一论，净土宗的全部依据。修行路径最简单，也最考验信心。','sunrise',4) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('chanmen','明心见性','直指人心，见性成佛','达摩付法楞伽，五祖传金刚，六祖以坛经立宗。楞严、圆觉两部长经，是禅门最常依用的根本。','bell',5) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('yicheng','一乘圆教','会三归一，一切众生皆成佛','法华开权显实，涅槃扶律谈常。天台宗立宗的经典群，讲“人人皆可成佛”。','wheel',6) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('huayan','华严世界','一花一世界，一叶一如来','两部华严，八十卷本与四十卷本。篇幅最大，境界最广，适合长期读。','flower',7) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('jielv','戒律行持','以戒为师','出家菩萨戒与在家菩萨戒各一部。想受戒、想弄清“能不能做”的看这里。','precept',8) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('yili','深观义理','破执之论，转识成智','中观与唯识两大体系：中论立中观，成唯识论立唯识，各配一部根本经。给想深究法义的人。','flame',9) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);
INSERT INTO sutra_topic (code,name,slogan,intro,icon,sort_no) VALUES ('ahan','原始圣典','佛陀原声，未加修饰','阿含部两部大经，最接近佛陀在世时的说法。文字重复，但史料价值最高。','tree',10) ON DUPLICATE KEY UPDATE name=VALUES(name),slogan=VALUES(slogan),intro=VALUES(intro),icon=VALUES(icon),sort_no=VALUES(sort_no);

-- 种子：主归属
INSERT IGNORE INTO sutra_topic_map (sutra_id,topic_id,is_primary,sort_no)
SELECT s.id, t.id, 1, x.sort_no
FROM (
  SELECT 'T0784' AS work_no, 'rudao' AS code, 1 AS sort_no UNION ALL
  SELECT 'T0779' AS work_no, 'rudao' AS code, 2 AS sort_no UNION ALL
  SELECT 'T0251' AS work_no, 'rudao' AS code, 3 AS sort_no UNION ALL
  SELECT 'T0685' AS work_no, 'rudao' AS code, 4 AS sort_no UNION ALL
  SELECT 'T0235' AS work_no, 'bore' AS code, 1 AS sort_no UNION ALL
  SELECT 'T0223' AS work_no, 'bore' AS code, 2 AS sort_no UNION ALL
  SELECT 'T0475' AS work_no, 'bore' AS code, 3 AS sort_no UNION ALL
  SELECT 'T0412' AS work_no, 'yuanli' AS code, 1 AS sort_no UNION ALL
  SELECT 'T0450' AS work_no, 'yuanli' AS code, 2 AS sort_no UNION ALL
  SELECT 'T1060' AS work_no, 'yuanli' AS code, 3 AS sort_no UNION ALL
  SELECT 'T0665' AS work_no, 'yuanli' AS code, 4 AS sort_no UNION ALL
  SELECT 'T0366' AS work_no, 'jingtu' AS code, 1 AS sort_no UNION ALL
  SELECT 'T0360' AS work_no, 'jingtu' AS code, 2 AS sort_no UNION ALL
  SELECT 'T0365' AS work_no, 'jingtu' AS code, 3 AS sort_no UNION ALL
  SELECT 'T1524' AS work_no, 'jingtu' AS code, 4 AS sort_no UNION ALL
  SELECT 'T2008' AS work_no, 'chanmen' AS code, 1 AS sort_no UNION ALL
  SELECT 'T0945' AS work_no, 'chanmen' AS code, 2 AS sort_no UNION ALL
  SELECT 'T0842' AS work_no, 'chanmen' AS code, 3 AS sort_no UNION ALL
  SELECT 'T0262' AS work_no, 'yicheng' AS code, 1 AS sort_no UNION ALL
  SELECT 'T0276' AS work_no, 'yicheng' AS code, 2 AS sort_no UNION ALL
  SELECT 'T0277' AS work_no, 'yicheng' AS code, 3 AS sort_no UNION ALL
  SELECT 'T0374' AS work_no, 'yicheng' AS code, 4 AS sort_no UNION ALL
  SELECT 'T0279' AS work_no, 'huayan' AS code, 1 AS sort_no UNION ALL
  SELECT 'T0293' AS work_no, 'huayan' AS code, 2 AS sort_no UNION ALL
  SELECT 'T1484' AS work_no, 'jielv' AS code, 1 AS sort_no UNION ALL
  SELECT 'T1488' AS work_no, 'jielv' AS code, 2 AS sort_no UNION ALL
  SELECT 'T1564' AS work_no, 'yili' AS code, 1 AS sort_no UNION ALL
  SELECT 'T1585' AS work_no, 'yili' AS code, 2 AS sort_no UNION ALL
  SELECT 'T0676' AS work_no, 'yili' AS code, 3 AS sort_no UNION ALL
  SELECT 'T0672' AS work_no, 'yili' AS code, 4 AS sort_no UNION ALL
  SELECT 'T0001' AS work_no, 'ahan' AS code, 1 AS sort_no UNION ALL
  SELECT 'T0099' AS work_no, 'ahan' AS code, 2 AS sort_no
) x
JOIN sutra s ON s.work_no = x.work_no
JOIN sutra_topic t ON t.code = x.code;

-- 种子：关联挂载
INSERT IGNORE INTO sutra_topic_map (sutra_id,topic_id,is_primary,sort_no)
SELECT s.id, t.id, 0, x.sort_no
FROM (
  SELECT 'T0251' AS work_no, 'bore' AS code, 101 AS sort_no UNION ALL
  SELECT 'T0685' AS work_no, 'yuanli' AS code, 101 AS sort_no UNION ALL
  SELECT 'T0277' AS work_no, 'jingtu' AS code, 101 AS sort_no UNION ALL
  SELECT 'T0672' AS work_no, 'chanmen' AS code, 101 AS sort_no UNION ALL
  SELECT 'T0842' AS work_no, 'yili' AS code, 101 AS sort_no UNION ALL
  SELECT 'T0945' AS work_no, 'yili' AS code, 102 AS sort_no
) x
JOIN sutra s ON s.work_no = x.work_no
JOIN sutra_topic t ON t.code = x.code;

-- 校验：每部经应恰好一个主归属，总映射数应 = 32 主 + 6 关联 = 38
-- SELECT COUNT(*) FROM sutra_topic_map WHERE is_primary=1;