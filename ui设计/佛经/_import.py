# -*- coding: utf-8 -*-
"""
把 佛经/ 目录导入 MySQL。

用法：
    pip install pymysql
    python _import.py --host 127.0.0.1 --user root --password xxx --db buddha
    python _import.py ... --dry-run          # 只统计不写库
    python _import.py ... --only T0251,T0262 # 只导指定经

先执行 _schema.sql 建表。重复导入会覆盖同一 work_no 的旧数据。
"""
import os, io, csv, json, hashlib, argparse, sys

BASE = os.path.dirname(os.path.abspath(__file__))

def md5(s):
    return hashlib.md5(s.encode("utf-8")).hexdigest()

def read_csv(p):
    with io.open(p, encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))

def load_work(folder):
    root = os.path.join(BASE, folder)
    meta = json.load(io.open(os.path.join(root, "_meta.json"), encoding="utf-8"))
    idx = read_csv(os.path.join(root, "_index.csv"))
    chapters = []
    for r in idx:
        fn = r["file"]
        s = io.open(os.path.join(root, "简体", fn), encoding="utf-8").read().rstrip("\n")
        t = io.open(os.path.join(root, "繁體原文", fn), encoding="utf-8").read().rstrip("\n")
        chapters.append({
            "juan_no": int(r["juan"]),
            "juan_title": r["juan_title"],
            "section_kind": r["section_kind"],
            "section_title": r["section_title"],
            "part_no": int(r["part"]),
            "content": s,
            "content_trad": t,
            "char_count": int(r["chars"]),
            "char_start": int(r["char_start"]),
            "char_end": int(r["char_end"]),
            "content_hash": md5(s),
        })
    return meta, chapters

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="rm-bp105nu39fi2s3a7f1o.mysql.rds.aliyuncs.com")
    ap.add_argument("--port", type=int, default=3306)
    ap.add_argument("--user", default="bebeep")
    ap.add_argument("--password", default="Yy654231")
    ap.add_argument("--db", default="bebeep")
    ap.add_argument("--only", default="")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()

    folders = sorted([d for d in os.listdir(BASE)
                      if os.path.isdir(os.path.join(BASE, d)) and os.path.exists(os.path.join(BASE, d, "_meta.json"))])
    only = [x for x in a.only.split(",") if x]
    if only:
        folders = [d for d in folders if any(d.endswith("_" + o) for o in only)]

    total_ch = 0
    conn = None
    if not a.dry_run:
        import pymysql
        conn = pymysql.connect(host=a.host, port=a.port, user=a.user,
                               password=a.password, database=a.db, charset="utf8mb4")
        cur = conn.cursor()

    for f in folders:
        meta, chapters = load_work(f)
        total_ch += len(chapters)
        if a.dry_run:
            print("%-58s %3d 章 %7d 字" % (f, len(chapters), meta["extracted_chars_with_punc"]))
            continue

        cur.execute("DELETE FROM sutra WHERE work_no=%s", (meta["work"],))
        cur.execute(
            """INSERT INTO sutra (work_no,title,title_trad,byline,dynasty,category,
                                  juan_count,char_count,folder,content_hash)
               VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
            (meta["work"], meta["title_simp"], meta["title_trad"], meta.get("byline"),
             meta.get("dynasty"), meta.get("category"), meta["juan_count"],
             meta["extracted_chars_with_punc"], meta["folder"],
             md5("".join(c["content"] for c in chapters))))
        sid = cur.lastrowid

        cur.executemany(
            """INSERT INTO sutra_chapter
               (sutra_id,seq,juan_no,juan_title,section_kind,section_title,part_no,
                content,content_trad,char_count,char_start,char_end,content_hash)
               VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
            [(sid, i, c["juan_no"], c["juan_title"], c["section_kind"], c["section_title"],
              c["part_no"], c["content"], c["content_trad"], c["char_count"],
              c["char_start"], c["char_end"], c["content_hash"])
             for i, c in enumerate(chapters, 1)])
        conn.commit()
        print("%-58s %3d 章 OK" % (f, len(chapters)))

    if conn:
        conn.close()
    print("\n合计 %d 部 / %d 章" % (len(folders), total_ch))

if __name__ == "__main__":
    main()
