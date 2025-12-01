# プロジェクト「Ethernet リバー」AI インストラクション

## 1. プロジェクトのゴールと最優先事項

**目的**: ネットワークパケットをリアルタイムで可視化するインタラクティブ・インスタレーションの制作。
**最重要視点**:

1.  **リアルタイム性**: パケットキャプチャから描画までの遅延を最小限に抑えること。
2.  **分散処理の連携**: 3 台のマシン（Python/pyshark, Kinect, Processing/Java）間での OSC 通信仕様を厳守すること。
3.  **パフォーマンス**: マシン 1 (pyshark) とマシン 3 (Processing) の両方で、パフォーマンスのボトルネックを常に意識し、軽量なロジックを優先すること。

開発に関する詳細や補足事項は、docs ディレクトリ内の md ファイルに随時記載していますので、必要に応じてご確認ください。

## 2. 主要な技術スタックと設計上の制約

### 2.1. マシン 1 (パケット解析: Python / eth-river)

-   **ライブラリ**: `pyshark`、`python-osc` を使用。
-   **pyshark 設定**:
    -   `use_json=False` (デフォルトの XML/PDML パーサーを使用)。
    -   **TCP**: `tcp.desegment_tcp_streams=TRUE` を必ず有効にすること。`tcp_handler`はこれに基づき `DATA` レイヤーの存在を考慮する必要がある。
-   **属性アクセス**: `pyshark` の `packet` オブジェクトへのアクセスは、`utils.py` にある `get_nested_attr()` で**常に**統一し、None 安全性を確保すること。
-   **通信 (OSC)**:
    -   解析したメタデータは `python-osc` クライアントを使用し、リアルタイムで即時送信すること。
    -   **重要**: `main.py` の `TARGET_IP = "127.0.0.1"` は**開発・テスト用の設定**である。本番環境（マシン 3 へのデプロイ時）には、この IP アドレスを**マシン 3 の IP アドレスに書き換える必要がある**ことを常に意識すること。

### 2.2. マシン 3 (可視化: Java / Processing)

-   **連携**: Processing 側は `oscP5` ライブラリで OSC データを受信する。
-   **通信方向判定**: 通信の方向 (Upstream/Downstream) の判定は、Processing 側でハードコーディングされた `localNetPrefix` 変数と `ip.startsWith()` を使って行われる。
-   **パーティクル削除**: Processing 側でのパーティクル削除ロジックの提案は、インデックスのずれを防ぐため、`for` ループで**逆順** (`i = list.size() - 1` から) に処理することを推奨すること。

## 3. 現在の最優先 ToDo と懸念点（開発方針）

Copilot によるコード提案は、以下の**未完了タスク**と**懸念点**の解決に貢献するようにしてください。

### 3.1. 最優先 ToDo (P2-1)

-   `tcp_handler.py` に、アプリケーション層のチェック**前**に `tcp_layer.flags_syn`, `flags_fin`, `flags_rst` をチェックするロジックを追加すること。

### 3.2. 次期 ToDo

-   **QUIC (P3-1)**: QUIC プロトコルの実装と SNI (`packet.quic.sni`など) 取得のためのハンドラ作成。
-   **可視化 (P3-Vis)**: TCP 制御パケット (SYN/FIN/RST など) 受信時の描画ロジック（色、寿命）を Processing 側に追加することを提案すること。
-   **Kinect (P2-II/P2-III)**: マシン 2 の Kinect 連携（物体座標を OSC でマシン 3 に送信）の実装。

### 3.3. 懸念点への配慮

-   **パフォーマンス (マシン 1 - Python)**:
    -   `pyshark` の `sniff_continuously()` ループは、`tshark` プロセスからの出力をポーリングする**シングルスレッド**で動作していると想定される。
    -   高トラフィック下では、この Python 側の処理ループか、`tshark` プロセス自体が CPU ボトルネックとなり、**パケットロスが発生する危険性がある**。
    -   したがって、`main.py` のメインループ内や、そこから呼び出されるパイプライン処理 (`link_layer.process` 以下) は、**極めて軽量**に保つ必要がある。
-   **パフォーマンス (マシン 3 - Processing)**:
    -   マシン 3 (RPi 5) の描画パフォーマンス（多数のパーティクル、物理演算、Kinect からの OSC 受信）が未知数である。
    -   Processing 側（Java）のコード提案は、効率的かつ軽量な実装（例：逆順ループでの削除）を優先すること。
-   **未処理プロトコル**: `DATA`, `STUN` などの未定義プロトコルは、現状の方針が未定であるため、一時的に `default_handler` で処理するか、無視するロジックを提案すること。

## 4. コーディングスタイルとメンテナンス

-   **言語**: Python 3.12 以降の環境で動作するコードを提案すること。
-   **リファクタリング**: `dns_handler.py` や `tcp_handler.py` などに残っている過去のデバッグ用 TODO やリファクタリング TODO の解消を促す、保守性の高いコードを提案すること。

## 5. 参考ドキュメント

-   [`docs/summary.md`](summary.md): プロジェクト全体の技術スタックと設計概要
-   [`docs/instructions_for_main.md`](instructions_for_main.md): マシン 1 (pyshark) の詳細な開発ガイド
-   [`docs/instructions_for_vis.md`](instructions_for_vis.md): マシン 3 (Processing) の詳細な開発ガイド
-   [`docs/DECISIONS.md`](DECISIONS.md): アーキテクチャ設計の経緯と決定事項
-   [`docs/CPU_thread.md`](CPU_thread.md): パフォーマンスとスレッドに関する考察
-   [`docs/TEST_DATA.md`](TEST_DATA.md): テストデータの仕様
