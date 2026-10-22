-- ============================================================
-- 佛经语料库建表脚本
-- 配套 E:\Android\Buddha\ui设计\佛经 下的 txt 与 csv
-- MySQL 8.0+ / utf8mb4
-- ============================================================

-- 1. 经目表（只存档案，不含正文）
CREATE TABLE IF NOT EXISTS sutra (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_no      VARCHAR(16)   NOT NULL COMMENT 'CBETA 编号，如 T0251',
  title        VARCHAR(128)  NOT NULL COMMENT '简体经名',
  title_trad   VARCHAR(128)  NOT NULL COMMENT '繁体经名',
  byline       VARCHAR(128)  NULL     COMMENT '译者／作者',
  dynasty      VARCHAR(32)   NULL     COMMENT '朝代',
  category     VARCHAR(32)   NULL     COMMENT '部类，如 般若部类',
  juan_count   SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  char_count   INT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '全经字数（含标点）',
  folder       VARCHAR(160)  NOT NULL COMMENT '源文件夹名',
  content_hash CHAR(32)      NULL     COMMENT '全经内容指纹，用于客户端增量更新',
  status       TINYINT       NOT NULL DEFAULT 1 COMMENT '1=上架 0=下架',
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_work (work_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='佛经元数据';

-- 2. 正文表（一个 txt = 一行）
CREATE TABLE IF NOT EXISTS sutra_chapter (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sutra_id      BIGINT UNSIGNED NOT NULL,
  juan_no       SMALLINT UNSIGNED NOT NULL COMMENT '卷号',
  juan_title    VARCHAR(128)  NULL     COMMENT '卷题',
  section_kind  VARCHAR(8)    NOT NULL COMMENT '正文／品／序／附',
  section_title VARCHAR(128)  NULL     COMMENT '品名等',
  part_no       SMALLINT UNSIGNED NOT NULL COMMENT '本小节内第几片',
  seq           INT UNSIGNED  NOT NULL COMMENT '全经排序号，从 1 连续',
  content       MEDIUMTEXT    NOT NULL COMMENT '简体正文',
  content_trad  MEDIUMTEXT    NOT NULL COMMENT '繁體正文',
  char_count    INT UNSIGNED  NOT NULL COMMENT '字数，含标点不含空白',
  char_start    INT UNSIGNED  NOT NULL COMMENT '本片在该卷内的起始偏移',
  char_end      INT UNSIGNED  NOT NULL COMMENT '结束偏移',
  content_hash  CHAR(32)      NULL     COMMENT '本片指纹',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sutra_seq (sutra_id, seq),
  KEY idx_sutra_juan (sutra_id, juan_no, part_no),
  CONSTRAINT fk_chapter_sutra FOREIGN KEY (sutra_id) REFERENCES sutra (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='经文正文，按约1000字切片';

-- 全文检索（中文需 ngram 解析器；数据量大时再建，会显著增加导入时间）
-- ALTER TABLE sutra_chapter ADD FULLTEXT KEY ft_content (content) WITH PARSER ngram;

-- 3. 附件表（原典扫描件、音频等，只存 URL）
CREATE TABLE IF NOT EXISTS sutra_asset (
  id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sutra_id BIGINT UNSIGNED NOT NULL,
  type     TINYINT NOT NULL COMMENT '1=PDF 2=扫描图 3=音频',
  url      VARCHAR(512) NOT NULL,
  bytes    BIGINT UNSIGNED NULL,
  PRIMARY KEY (id),
  KEY idx_asset_sutra (sutra_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 用户进度相关
-- ============================================================

-- 4. 读经进度（每部经一条，覆盖写）
CREATE TABLE IF NOT EXISTS user_reading_progress (
  user_id            BIGINT UNSIGNED NOT NULL,
  sutra_id           BIGINT UNSIGNED NOT NULL,
  last_chapter_seq   INT UNSIGNED NOT NULL DEFAULT 1,
  last_char_offset   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '章内字符偏移',
  progress_permille  SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '全书千分比，列表页用',
  updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, sutra_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. 抄经会话流水（每次进入抄写提交一条，只增不改）
CREATE TABLE IF NOT EXISTS user_copy_session (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id      BIGINT UNSIGNED NOT NULL,
  sutra_id     BIGINT UNSIGNED NOT NULL,
  chapter_seq  INT UNSIGNED NOT NULL,
  start_offset INT UNSIGNED NOT NULL,
  end_offset   INT UNSIGNED NOT NULL,
  char_count   INT UNSIGNED NOT NULL,
  duration_ms  INT UNSIGNED NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_session_user (user_id, sutra_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. 抄经累计进度（每部经一条）
CREATE TABLE IF NOT EXISTS user_copy_progress (
  user_id             BIGINT UNSIGNED NOT NULL,
  sutra_id            BIGINT UNSIGNED NOT NULL,
  copied_chars        BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计抄写字数，重复抄写也累加',
  total_chars         INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '该经总字数，冗余避免 join',
  last_chapter_seq    INT UNSIGNED NOT NULL DEFAULT 1,
  last_char_offset    INT UNSIGNED NOT NULL DEFAULT 0,
  finished_chapters   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已抄完的章数',
  first_started_at    DATETIME NULL,
  last_copied_at      DATETIME NULL,
  PRIMARY KEY (user_id, sutra_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
