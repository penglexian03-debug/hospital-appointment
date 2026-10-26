from app.rag.loader import load_knowledge_chunks


def test_loads_markdown_sections(tmp_path) -> None:
    (tmp_path / "hospital.md").write_text(
        "# 安和医院\n\n## 地址\n\n北京市朝阳区安和路88号。\n\n## 电话\n\n010-60000000。",
        encoding="utf-8",
    )

    chunks = load_knowledge_chunks(tmp_path, chunk_size=200, chunk_overlap=20)

    assert len(chunks) == 2
    assert chunks[0].title == "安和医院"
    assert chunks[0].heading == "地址"
    assert "安和路88号" in chunks[0].content
    assert chunks[1].heading == "电话"


def test_missing_directory_returns_empty(tmp_path) -> None:
    assert load_knowledge_chunks(tmp_path / "missing") == []


def test_long_section_is_split(tmp_path) -> None:
    body = "这是一段用于测试分块的医院介绍文字。" * 40
    (tmp_path / "long.md").write_text(f"# 长文\n\n## 介绍\n\n{body}", encoding="utf-8")

    chunks = load_knowledge_chunks(tmp_path, chunk_size=80, chunk_overlap=10)

    assert len(chunks) > 1
    assert all(len(chunk.content) < 200 for chunk in chunks)
    assert all(chunk.heading == "介绍" for chunk in chunks)

