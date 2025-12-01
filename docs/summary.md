# プロジェクト「Ethernet リバー」全体サマリー

## 1. プロジェクトの目的とゴール

**目的**: ネットワークトラフィック（パケット）をリアルタイムで可視化するインタラクティブ・インスタレーションの制作。

**ゴール**:

1. **キャプチャ**: Raspberry Pi でネットワークブリッジを構築し、`pyshark` を使って全パケットをキャプチャする。
2. **解析**: キャプチャしたパケットをプロトコル（HTTP, HTTPS/TLS, DNS, QUIC, TCP Handshake）ごとに分類し、宛先ドメイン(SNI)や方向性（上り/下り）を特定する。
3. **送信**: 解析したメタデータ（プロトコル名、パケット長、方向、ドメイン名）を OSC で描画用マシンにリアルタイム送信する。
4. **可視化**: Processing で OSC データを受信し、パケットを「光の川」として描画する。プロトコルで色、パケット長でサイズ、方向で流れ（上り/下り）を変える。
5. **介入**: Kinect（マシン 2）で物理的な「石」や「来場者」を認識し、その座標を OSC で送信。Processing 側でその情報を受け取り、川の流れ（パーティクルの挙動）に影響を与える。

**体験のコンセプト**: 来場者が専用 Wi-Fi に接続すると、その通信が「光の川」として床や机に投影される。物理的な「石」（タンジブル・オブジェクト）を置くと、川の流れに「介入」（せき止める、引き寄せるなど）できる。通信の「方向性」（上り/下り）や種類（プロトコル）、接続の開始と終了を視覚化することで、インターネットの裏側の仕組みを直感的に体験・理解してもらう。

---

## 2. アーキテクチャと技術スタック

### 2.1. システム構成（3 台の Raspberry Pi）

| マシン       | ハードウェア    | 役割                      | 開発ディレクトリ                      |
| :----------- | :-------------- | :------------------------ | :------------------------------------ |
| **マシン 1** | Raspberry Pi 4B | パケット解析・OSC 送信    | [`eth_river/`](../eth_river/)         |
| **マシン 2** | Raspberry Pi 4B | Kinect 物体認識・OSC 送信 | （未着手）                            |
| **マシン 3** | Raspberry Pi 5  | 可視化・シミュレーション  | [`eth_river_vis/`](../eth_river_vis/) |

**通信方式**: **OSC (Open Sound Control)**

-   マシン 1 → マシン 3: パケット情報（ポート `12345`）
-   マシン 2 → マシン 3: Kinect 情報（ポート `12346`、仮仕様）

### 2.2. マシン 1: パケット解析 ([`eth_river/`](../eth_river/))

**構成**: 透過型 L2 ブリッジ（`br0`）として動作。IP アドレスは持たない。管理 I/F として内蔵 Wi-Fi (`wlan0`) 経由で SSH 接続および OSC 送信を行う。

**技術スタック**:

-   **言語**: Python 3.12+
-   **主要ライブラリ**:
    -   **`pyshark`**: パケットキャプチャと解析（`tshark` のラッパー）
    -   **`python-osc`**: OSC メッセージの送信

**重要な設定**:

-   `use_json=False`（デフォルトの XML/PDML パーサーを使用）
-   `tcp.desegment_tcp_streams=TRUE`（TCP 再アセンブリを有効化）

**パイプライン構造**:

```
link_layer.py → network_layer.py → transport_layer/
                                    ├── tcp_handler.py
                                    └── udp_handler.py
                                        ├── application_layer/dns_handler.py
                                        ├── application_layer/tls_handler.py
                                        └── （今後: quic_handler.py）
```

**主要ファイル**:

-   [`main.py`](../eth_river/main.py): メインループ（`sniff_continuously`）
-   [`utils.py`](../eth_river/utils.py): `get_nested_attr()`（None 安全な属性アクセス）、`format_output()`（OSC 送信）
-   [`pipeline/`](../eth_river/pipeline/): プロトコル層ごとのハンドラ群

**開発時の注意点**:

-   **`TARGET_IP = "127.0.0.1"`** は開発・テスト用。**本番環境ではマシン 3 の IP アドレスに書き換える必要がある**（[`main.py`](../eth_river/main.py) 内）。

### 2.3. マシン 3: 可視化 ([`eth_river_vis/`](../eth_river_vis/))

**技術スタック**:

-   **言語**: Java 17
-   **フレームワーク**: Processing 4（`PApplet` クラスを継承）
-   **ビルドシステム**: Gradle
-   **主要ライブラリ**:
    -   **`oscP5`**: OSC 受信

**主要ファイル**:

-   `src/main/java/net/noahbutler/eth_river_vis/Main.java`: メインクラス（`setup()`, `draw()`, `oscEvent()`）
-   `src/main/java/net/noahbutler/eth_river_vis/Particle.java`: パーティクルクラス

**現在の実装レベル**:

-   **レベル 1**: 単純な等速度運動（`pos.add(vel)`）でパーティクルが流れる。

---

## 3. 現状の ToDo リストと進捗

### 完了済み（P0/P1）

-   ✅ **[マシン 1]** `pyshark`パイプライン構築（`link_layer` → `network_layer` → `transport_layer`）
-   ✅ **[マシン 1]** `tls_handler` での SNI 取得（`get_nested_attr(packet.tls, "handshake_extensions_server_name")`）に成功
-   ✅ **[マシン 1]** OSC 送信機能の実装（[`utils.py`](../eth_river/utils.py) の `format_output`）
-   ✅ **[マシン 3]** OSC 受信機能の実装（`oscEvent`）
-   ✅ **[マシン 3]** パーティクルシステムの基本実装（レベル 1：粒子が流れる）
-   ✅ **[マシン 3]** 通信方向（Upstream/Downstream）の判定と描画（上下両方から発生）の実装
-   ✅ **[マシン 3]** 安全なパーティクル削除（逆順ループで `ArrayList` から削除）

### 未完了（次のタスク）

-   **P2-1: TCP ハンドシェイク処理**

    -   担当: マシン 1 [`tcp_handler.py`](../eth_river/pipeline/transport_layer/tcp_handler.py)
    -   アプリケーション層のチェックの**前**に、`tcp_layer.flags_syn`, `flags_fin`, `flags_rst` をチェックするロジックを追加する。専用の OSC メッセージ（例: `/packet/tcp_syn`）を送信する。

-   **P3-1: QUIC プロトコルの実装**

    -   担当: マシン 1 [`udp_handler.py`](../eth_river/pipeline/transport_layer/udp_handler.py), `quic_handler.py`（新規）
    -   QUIC の SNI (`packet.quic.sni` など) を取得するハンドラーを作成し、`udp_handler` の辞書に登録する。

-   **P3-Vis: TCP 制御パケットの可視化**

    -   担当: マシン 3 `Main.java`
    -   P2-1 で実装される `/packet/tcp_syn` などの OSC メッセージを受信し、色（白など）や寿命（短い）を変えて描画するロジックを追加する。

-   **P2-II: Kinect 環境構築**

    -   担当: マシン 2（新規開発）
    -   Kinect を Pi 4B に接続し、物体（石）や人物を認識して OSC を送信するプログラムの開発を開始する。

-   **P2-III: Kinect OSC 受信スタブ作成**

    -   担当: マシン 3 `Main.java`
    -   マシン 2 からの OSC（仮仕様: `/kinect/object` @ `12346`）を受信する `oscP5.plug()` と、コールバック関数（例: `kinectObjectEvent()`）を実装する。

-   **P2-IV: 「介入」ロジック実装**

    -   担当: マシン 3 `Main.java` (`Particle` クラス)
    -   P2-III で受信した「石」の座標（X, Y）を `Particle` クラスの `update()` に渡し、粒子がその座標を避ける、または引き寄せられる（「ダム」「磁石」）ロジックを実装する。

-   **P3-Sim: シミュレーション高度化**

    -   担当: マシン 3 `Particle` クラス
    -   `Particle` クラスに「加速度 (`PVector acc`)」と `applyForce(PVector force)` メソッドを追加し、粒子同士が反発する力や、Kinect の座標に反応する力を計算するロジック（レベル 3）を実装する。

-   [ ] ノードベース可視化システムの実装

    -   ノード間の通信を粒子で表現し、川の流れのようなビジュアライゼーションを目指す。
    -   ローカルネットワーク（右側）とインターネット（左側）を分けて表示。
    -   参考: [docs/ideas/node-flow-idea.md](docs/ideas/node-flow-idea.md)

-   [ ] 粒子のビジュアル表現・群知能アルゴリズムの実装
    -   粒子を有機的に動かし、加算合成やトレイル、パケットサイズに応じたスケーリング・輝度調整を行う。
    -   Boids アルゴリズムやベジェ曲線、力場による移動を検討。
    -   参考: [docs/ideas/particle-idea.md](docs/ideas/particle-idea.md)

---

## 4. 重要な仕様・設計決定事項

### 4.1. マシン 1（Python / pyshark）関連

-   **属性アクセスの統一**: `pyshark` の `packet` オブジェクトへのアクセスは、[`utils.py`](../eth_river/utils.py) にある `get_nested_attr()` で**常に**統一する。これにより、フィールド/属性の違いを意識せず、`None` 安全なアクセスが可能。

    ```python
    # 例
    sni = get_nested_attr(packet.tls, "handshake_extensions_server_name")
    ```

-   **TCP 再アセンブリ**: `pyshark` で `tcp.desegment_tcp_streams=TRUE` を有効にする。[`tcp_handler`](../eth_river/pipeline/transport_layer/tcp_handler.py) は `DATA` レイヤー の存在を考慮する必要がある。

-   **プロトコル振り分け**: `pyshark` が解析したレイヤー名 (`layer_name`) をキーとして、[`tcp_handler.py`](../eth_river/pipeline/transport_layer/tcp_handler.py) と [`udp_handler.py`](../eth_river/pipeline/transport_layer/udp_handler.py) の `APPLICATION_HANDLERS` 辞書で振り分ける。

-   **SNI の取得パス**:
    -   **TLS (HTTPS)**: `get_nested_attr(packet.tls, "handshake_extensions_server_name")`
    -   **QUIC**: （`quic_handler.py` 未実装だが）`get_nested_attr(packet.quic, "sni")` などを試行する。

### 4.2. マシン 3（Java / Processing）関連

-   **OSC 仕様（pyshark から）**: ポート `12345` で受信。

    -   **アドレス**: `/packet/{protocol_name}` (例: `/packet/dns`, `/packet/tls_hello`)
    -   **引数（順序固定）**:
        1. `String`: プロトコル名 (例: "DNS", "TLS-Hello")
        2. `int`: パケット長 (例: 120)
        3. `String`: 詳細 (例: "SNI: google.com")
        4. `String`: 送信元 IP (例: "192.168.11.50")
        5. `String`: 宛先 IP (例: "1.1.1.1")

-   **OSC 仕様（Kinect から）**: （仮仕様）

    -   **ポート**: `12346`（`12345` とは別）
    -   **アドレス**: `/kinect/object`
    -   **引数**: `int` (ID), `float` (x), `float` (y), `float` (angle)

-   **通信方向判定ロジック**: `Main.java` 側で、`localNetPrefix` 変数（ハードコーディング）と `ip.startsWith()` を使って方向を判定する。

    -   `srcIsLocal && !dstIsLocal` → **Upstream**（下から上へ）
    -   `!srcIsLocal && dstIsLocal` → **Downstream**（上から下へ）
    -   その他（Local, Unknown） → **Downstream**（上から下へ）

-   **パーティクル削除**: Processing の `ArrayList` からパーティクルを削除する際は、インデックスのずれを防ぐため、`for` ループで**逆順**（`i = list.size() - 1` から）に処理する。

-   **即時表示**: リクエスト/レスポンスの「待ち」は実装せず、キャプチャした瞬間に即時粒子を発生させる。

-   **パーティクルクラス (`Particle`)**:
    -   属性: `PVector pos`, `PVector vel`, `color c`, `float size`
    -   `update()`: `pos.add(vel)` というレベル 1 の物理演算を実装
    -   `isDead()`: `pos.y` が画面外（上下）に出たか判定する

---

## 5. 現在抱えている課題・懸念点

### 5.1. パフォーマンス関連

-   **マシン 1（Python）のボトルネック**:

    -   `pyshark` の `sniff_continuously()` ループは、`tshark` プロセスからの出力をポーリングする**シングルスレッド**で動作していると想定される。
    -   高トラフィック下では、この Python 側の処理ループか、`tshark` プロセス自体が CPU ボトルネックとなり、**パケットロスが発生する危険性がある**。
    -   したがって、[`main.py`](../eth_river/main.py) のメインループ内や、そこから呼び出されるパイプライン処理は、**極めて軽量**に保つ必要がある。

-   **マシン 3（Processing）のパフォーマンス**:
    -   RPi 5 で、多数のパーティクル（P1）＋高度な物理演算（P3-Sim）＋ Kinect からの OSC データ（P2-III）を同時に処理した際のフレームレートが未知数。
    -   Processing 側（Java）のコード実装は、効率的かつ軽量な実装（例：逆順ループでの削除、不要な計算の削減）を優先する必要がある。

### 5.2. 開発未着手・未完了の機能

-   **[`tcp_handler.py`](../eth_river/pipeline/transport_layer/tcp_handler.py) の実装**: TCP ハンドシェイク（P2-1）が未実装のままであり、最優先で修正が必要。

-   **Kinect 開発**: マシン 2（Kinect）の開発が未着手。RPi 4B と Kinect の連携（ドライバ、CV 処理）がスムーズに進むか未検証。

-   **未処理プロトコル**: `DATA`, `STUN` などの未定義プロトコルをどう扱うか（無視するか、`default_handler` で処理するか）の方針が未定。

### 5.3. コード保守性

-   **TODO コメント**: [`dns_handler.py`](../eth_river/pipeline/transport_layer/application_layer/dns_handler.py) と [`tcp_handler.py`](../eth_river/pipeline/transport_layer/tcp_handler.py) に、過去のデバッグ用 TODO やリファクタリング TODO が残っている。これらの解消を促す、保守性の高いコードへのリファクタリングが望ましい。

---

## 6. 開発環境とビルド

### マシン 1（Python）

```bash
cd eth_river
python -m pip install -r requirements.txt
python main.py
```

### マシン 3（Processing / Java）

```bash
cd eth_river_vis
./gradlew run
```

---

## 7. 参考ドキュメント

-   [`docs/instructions_for_main.md`](instructions_for_main.md): マシン 1 (pyshark) の詳細な開発ガイド
-   [`docs/instructions_for_vis.md`](instructions_for_vis.md): マシン 3 (Processing) の詳細な開発ガイド
-   [`docs/DECISIONS.md`](DECISIONS.md): アーキテクチャ設計の経緯と決定事項
-   [`docs/CPU_thread.md`](CPU_thread.md): パフォーマンスとスレッドに関する考察
-   [`docs/TEST_DATA.md`](TEST_DATA.md): テストデータの仕様
