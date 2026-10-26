import re
from dataclasses import dataclass
from pathlib import Path
from typing import List, Tuple


@dataclass(frozen=True)
class KnowledgeChunk:
    """A small, self-contained slice of a hospital knowledge document."""

    id: str
    source: str
    title: str
    heading: str
    content: str


def load_knowledge_chunks(
    directory: Path,
    chunk_size: int = 480,
    chunk_overlap: int = 80,
) -> List[KnowledgeChunk]:
    """Load every Markdown file in *directory* and split it into chunks.

    Documents are plain Markdown. ``#`` is the document title and ``##`` starts a
    section. Long sections are split further so a single chunk stays small enough
    to be embedded and shown to the model.
    """

    if not directory.exists() or not directory.is_dir():
        return []

    chunks: List[KnowledgeChunk] = []
    for path in sorted(directory.glob("*.md")):
        text = path.read_text(encoding="utf-8")
        title = _extract_title(text, path.stem)
        for section_index, (heading, body) in enumerate(_split_sections(text)):
            for chunk_index, piece in enumerate(
                _split_body(body, chunk_size, chunk_overlap)
            ):
                chunks.append(
                    KnowledgeChunk(
                        id=f"{path.stem}-{section_index}-{chunk_index}",
                        source=path.name,
                        title=title,
                        heading=heading,
                        content=f"{title} / {heading}\n{piece}".strip(),
                    )
                )
    return chunks


def _extract_title(text: str, fallback: str) -> str:
    match = re.search(r"^\s*#\s+(.+?)\s*$", text, re.MULTILINE)
    return match.group(1).strip() if match else fallback


def _split_sections(text: str) -> List[Tuple[str, str]]:
    sections: List[Tuple[str, List[str]]] = []
    current_heading = "简介"
    current_lines: List[str] = []

    for line in text.splitlines():
        if re.match(r"^\s*#\s+", line):
            # Document title, already captured separately.
            continue
        heading_match = re.match(r"^\s*##\s+(.+?)\s*$", line)
        if heading_match:
            body = "\n".join(current_lines).strip()
            if body:
                sections.append((current_heading, current_lines))
            current_heading = heading_match.group(1).strip()
            current_lines = []
            continue
        current_lines.append(line)

    body = "\n".join(current_lines).strip()
    if body:
        sections.append((current_heading, current_lines))

    return [(heading, "\n".join(lines).strip()) for heading, lines in sections]


def _split_body(body: str, chunk_size: int, chunk_overlap: int) -> List[str]:
    body = body.strip()
    if not body:
        return []

    paragraphs = [part.strip() for part in re.split(r"\n\s*\n", body) if part.strip()]
    chunks: List[str] = []
    buffer = ""

    for paragraph in paragraphs:
        if len(paragraph) > chunk_size:
            if buffer:
                chunks.append(buffer)
                buffer = ""
            chunks.extend(_split_long_paragraph(paragraph, chunk_size, chunk_overlap))
            continue

        candidate = f"{buffer}\n\n{paragraph}" if buffer else paragraph
        if len(candidate) <= chunk_size:
            buffer = candidate
        else:
            if buffer:
                chunks.append(buffer)
            buffer = paragraph

    if buffer:
        chunks.append(buffer)
    return chunks


def _split_long_paragraph(text: str, chunk_size: int, chunk_overlap: int) -> List[str]:
    chunk_size = max(1, chunk_size)
    step = max(1, chunk_size - max(0, chunk_overlap))
    pieces: List[str] = []
    start = 0
    while start < len(text):
        piece = text[start : start + chunk_size].strip()
        if piece:
            pieces.append(piece)
        if start + chunk_size >= len(text):
            break
        start += step
    return pieces

