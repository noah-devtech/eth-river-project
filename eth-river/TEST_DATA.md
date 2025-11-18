TEST DATA (pcapng) - placement and usage

目的

-   開発・デバッグ用のパケットキャプチャ（pcap/pcapng）ファイルを、リポジトリ外に置いて運用するための手順をまとめる。

推奨ディレクトリ構成（リポジトリルートの外、ローカルマシンに配置）

-   test-data/
    -   pcap/
        -   example_full_capture.pcapng
        -   capture_2025-11-18.pcapng

運用ルール

-   `test-data/` は大きなファイルや機密データを含む可能性があるため、リポジトリでは管理しない（すでに `.gitignore` に追加済み）。
-   小さなサンプル（CI 用）を置く場合は `tests/fixtures/pcap/` に置き、リポジトリで管理する。

コードからの参照方法

-   デフォルトの参照パスはプロジェクトルートの `test-data/pcap` を想定するが、環境ごとにカスタマイズするために環境変数 `ETH_RIVER_TEST_DATA` を使うことを推奨する。

使用例（Python）

```python
from pathlib import Path
import os

def get_test_pcap_dir():
    env = os.getenv("ETH_RIVER_TEST_DATA")
    if env:
        return Path(env)
    return Path.cwd() / "test-data" / "pcap"

pcap_dir = get_test_pcap_dir()
pcap_file = pcap_dir / "example_full_capture.pcapng"
# ここで pcap_file を pyshark などに渡す
```

README への追記提案

-   `eth-river/README.md` に短い説明と `TEST_DATA.md` へのリンクを追加してください。

セキュリティ注意

-   実トラフィックは個人情報や機密を含む可能性があります。公開リポジトリにアップロードしないでください。
