FROM python:3.12-slim-bookworm
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

COPY pyproject.toml uv.lock ./

RUN uv sync --frozen --no-dev


ENV PATH="/app/.venv/bin:$PATH"

COPY . .

# 実行コマンド
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]