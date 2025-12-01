# マシン 1 (Python/pyshark) 開発ガイド

> このファイルは **マシン 1 (`eth_river/` ディレクトリ)** の開発・運用・設計指針を記載した、サブプロジェクト専用のドキュメントです。
> プロジェクト全体の概要や他のマシンの情報は [docs/summary.md](summary.md) を参照してください。

---

## 1. マシン 1 の役割と責務

**役割**: パケットキャプチャ・プロトコル解析・OSC 送信

**ハードウェア**: Raspberry Pi 4B

**主要な処理フロー**:

1. 透過型 L2 ブリッジ（`br0`）上で全パケットをキャプチャ
2. `pyshark`でプロトコル層ごとに解析（HTTP, HTTPS/TLS, DNS, QUIC, TCP 制御パケット等）
3. 宛先ドメイン（SNI）や通信方向を特定
4. 解析したメタデータを`python-osc`でマシン 3 へリアルタイム送信

---

## 2. 技術スタックと主要ライブラリ

-   **言語**: Python 3.12+
-   **主要ライブラリ**:
    -   `pyshark`: パケットキャプチャと解析（`tshark`のラッパー）
    -   `python-osc`: OSC メッセージの送信
-   **重要な設定**:
    -   `use_json=False` (デフォルトの XML/PDML パーサーを使用)
    -   `tcp.desegment_tcp_streams=TRUE` (TCP 再アセンブリを有効化)

---

## 3. 現状の ToDo リストと進捗

### 完了済み

-   **[マシン 1]** `pyshark`パイプライン構築（`link_layer` -> `network_layer` -> `transport_layer`）。
-   **[マシン 1]** `tls_handler` での SNI 取得（`get_nested_attr(packet.tls, "handshake_extensions_server_name")`）に成功。
-   **[マシン 1]** OSC 送信機能の実装（`utils.py` の `format_output`）。
-   **[マシン 3]** OSC 受信機能の実装（`oscEvent`）。
-   **[マシン 3]** パーティクルシステムの基本実装（レベル 1：粒子が流れる）。
-   **[マシン 3]** 通信方向（Upstream/Downstream）の判定と描画（上下両方から発生）の実装。

### 未完了（次のタスク）

1.  **(最優先) P2-1: TCP ハンドシェイク処理**

    -   **担当**: マシン 1 (`tcp_handler.py`)
    -   **内容**: アプリケーション層のチェックの**前**に、`tcp_layer.flags_syn`, `flags_fin`, `flags_rst` をチェックするロジックを追加する。
    -   **現状**: `test.txt` に `TCP has no higher layer` と出力されており、未実装。

2.  **P3-1: QUIC プロトコルの実装**

    -   **担当**: マシン 1 (`udp_handler.py`, `quic_handler.py` (新規))
    -   **内容**: QUIC の SNI (`packet.quic.sni` など) を取得するハンドラを作成し、`udp_handler` の辞書に登録する。

3.  **P3-Vis: TCP 制御パケットの可視化**

    -   **担当**: マシン 3 (`Main.java`)
    -   **内容**: P2-1 で実装される `/packet/tcp_syn` などの OSC メッセージを受信し、色（白など）や寿命（短い）を変えて描画するロジックを追加する。

4.  **P2-II / P2-III: Kinect による「介入」の実装**
    -   **担当**: マシン 2 (新規開発), マシン 3 (`Main.java`)
    -   **内容**: Kinect で物体座標を OSC 送信するプログラム（マシン 2）と、それを受信してパーティクルの `update()` に介入ロジックを追加する（マシン 3）。

---

## 4. これまでに決定した重要な仕様・設計

-   **属性アクセス**: `pyshark` の `packet` オブジェクトへのアクセスは、`utils.py` にある `get_nested_attr()` で統一する。これにより、フィールド/属性の違いを意識せず、`None` 安全なアクセスが可能。
-   **TCP 再アセンブリ**: `pyshark` で `tcp.desegment_tcp_streams=TRUE` を有効にする。`tcp_handler` は `DATA` レイヤー の存在を考慮する必要がある。
-   **ローカル IP 判定**: `Main.java` 側で、`localNetPrefix` 変数（ハードコーディング）と `ip.startsWith()` を使って方向（Upstream/Downstream）を判定する。
-   **リクエスト/レスポンスの待機**: 「待ち」は実装せず、キャプチャした瞬間に即時 OSC 送信する。

---

## 5. OSC 送信仕様（マシン 3 向け）

**送信先**: マシン 3（デフォルト `TARGET_IP = "127.0.0.1"`）  
**ポート**: `12345`  
**アドレス**: `/packet/{protocol_name}` (例: `/packet/dns`, `/packet/tls_hello`)

**引数（順序固定）**:

1. `String`: プロトコル名 (例: "DNS", "TLS-Hello")
2. `int`: パケット長 (例: 120)
3. `String`: 詳細 (例: "SNI: google.com")
4. `String`: 送信元 IP (例: "192.168.11.50")
5. `String`: 宛先 IP (例: "1.1.1.1")

**重要**: `main.py`の`TARGET_IP`は開発・テスト用の設定。**本番環境ではマシン 3 の IP アドレスに書き換え必須**。

---

## 6. 開発時の注意点とベストプラクティス

### 6.1 パフォーマンス重視の実装

-   `pyshark`の`sniff_continuously()`は**シングルスレッド**で動作
-   高トラフィック下では CPU がボトルネックとなり**パケットロスのリスク**
-   `main.py`のメインループ内や、パイプライン処理は**極めて軽量**に保つ必要がある
-   重い処理（ログ出力、複雑な文字列操作等）は最小限に

### 6.2 属性アクセスの統一

-   `pyshark`の`packet`オブジェクトへのアクセスは**必ず**`utils.get_nested_attr()`を使用
-   フィールド/属性の違いを意識せず、None 安全なアクセスが可能

```python
# 例
sni = get_nested_attr(packet.tls, "handshake_extensions_server_name")
```

### 6.3 TCP 再アセンブリの考慮

-   `tcp.desegment_tcp_streams=TRUE`により、`DATA`レイヤーが存在する場合がある
-   `tcp_handler.py`では`DATA`レイヤーの存在を考慮した処理が必要

### 6.4 未定義プロトコルの扱い

-   `DATA`, `STUN`等の未定義プロトコルは現状`default_handler`で処理または無視
-   必要に応じて専用ハンドラを追加

---

## 7. 現在の課題と今後の方針

### 最優先課題

-   **TCP 制御パケット処理（P2-1）**: `tcp_handler.py`で SYN/FIN/RST フラグの検出が未実装
-   **パフォーマンス最適化**: CPU 使用率の削減、パケットロス対策

### 今後の拡張

-   **QUIC 対応（P3-1）**: `quic_handler.py`の新規作成と SNI 取得
-   **コード保守性向上**: 残存する TODO コメントの解消、リファクタリング

詳細な TODO リストは [docs/TODO.md](TODO.md) を参照してください。

---

## 8. ビルド・実行方法

```bash
cd eth_river
python -m pip install -r requirements.txt
python main.py
```

---

## 9. 関連ドキュメント

-   [docs/summary.md](summary.md): プロジェクト全体サマリー
-   [docs/instructions_for_vis.md](instructions_for_vis.md): マシン 3 開発ガイド
-   [docs/TODO.md](TODO.md): 全体 TODO リスト
-   [docs/DECISIONS.md](DECISIONS.md): 設計決定履歴
-   [docs/CPU_thread.md](CPU_thread.md): パフォーマンス考察
