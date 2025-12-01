# プロジェクト「Ethernet リバー」AI 向け引き継ぎサマリー

## 1. プロジェクトの目的とゴール

-   **目的**: ネットワークトラフィック（パケット）をリアルタイムで可視化するインタラクティブ・インスタレーションの制作。
-   **ゴール**:
    1.  **キャプチャ**: Raspberry Pi でネットワークブリッジを構築し、`pyshark` を使って全パケットをキャプチャする。
    2.  **解析**: キャプチャしたパケットをプロトコル（HTTP, HTTPS/TLS, DNS, QUIC, TCP Handshake）ごとに分類し、宛先ドメイン(SNI)や方向性（上り/下り）を特定する。
    3.  **送信**: 解析したメタデータ（プロトコル名、パケット長、方向、ドメイン名）を OSC で描画用マシンにリアルタイム送信する。
    4.  **可視化**: Processing で OSC データを受信し、パケットを「光の川」として描画する。プロトコルで色、パケット長でサイズ、方向で流れ（上り/下り）を変える。
    5.  **介入**: Kinect（マシン 2）で物理的な「石」や「来場者」を認識し、その座標を OSC で送信。Processing 側でその情報を受け取り、川の流れ（パーティクルの挙動）に影響を与える。

---

## 2. 主要な技術スタック

-   **アーキテクチャ**: 3 台の Raspberry Pi による分散処理
    -   **マシン 1 (pyshark)**: RPi 4B - 透過型 L2 ブリッジ（`br0`） + Wi-Fi（`wlan0`）管理 I/F
    -   **マシン 2 (Kinect)**: RPi 4B - （未着手）
    -   **マシン 3 (Processing)**: RPi 5 - 描画・シミュレーション担当
-   **マシン 1 (pyshark / `eth-river` フォルダ)**
    -   **言語**: Python 3
    -   **ライブラリ**: `pyshark`, `python-osc`
    -   **設定**: `use_json=False`（デフォルトの XML/PDML パーサーを使用）, `tcp.desegment_tcp_streams=TRUE`
-   **マシン 3 (Processing / `eth_river_vis` フォルダ)**
    -   **言語**: Java (Processing 4)
    -   **ライブラリ**: `oscP5`
    -   **ビルド**: Gradle
-   **通信**: **OSC** (マシン 1→3, マシン 2→3)

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
-   **パーティクル削除**: Processing の `ArrayList` からパーティクルを削除する際は、インデックスのずれを防ぐため、`for` ループで**逆順**（`i = list.size() - 1` から）に処理する。
-   **リクエスト/レスポンス**: 「待ち」は実装せず、キャプチャした瞬間に即時粒子を発生させる。

---

## 5. 現在抱えている課題や懸念点

-   **`tcp_handler.py` の実装**: `tcp_handler.py` が P2（ハンドシェイク）未実装のままであり、最優先で修正が必要。
-   **Kinect 開発**: マシン 2（Kinect）の開発が未着手。RPi 4B と Kinect の連携（ドライバ、CV 処理）がスムーズに進むか未検証。
-   **Processing のパフォーマンス**: RPi 5 で、多数のパーティクル（P1）＋高度な物理演算（P3-Sim）＋ Kinect からの OSC データ（P2-III）を同時に処理した際のフレームレートが未知数。
-   **未処理プロトコル**: `test.txt` にある `DATA`, `STUN` などの未定義プロトコルをどう扱うか（無視するか、`default_handler` で処理するか）の方針が未定。
-   **TODO コメント**: `dns_handler.py` と `tcp_handler.py` に、過去のデバッグ用 TODO やリファクタリング TODO が残っている。
