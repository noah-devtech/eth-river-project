# Ethernet River Project

## ドキュメント一覧

### プロジェクト全体

-   **[docs/summary.md](docs/summary.md)**: プロジェクト全体の概要・アーキテクチャ・横断的な設計方針
-   **[docs/TODO.md](docs/TODO.md)**: プロジェクト全体の TODO リスト（全タスクの集約先）
-   **[docs/DECISIONS.md](docs/DECISIONS.md)**: 重要な設計決定の履歴
-   **[docs/CPU_thread.md](docs/CPU_thread.md)**: パフォーマンスとスレッドに関する考察

### サブプロジェクト別開発ガイド

-   **[docs/instructions_for_main.md](docs/instructions_for_main.md)**: マシン 1 (Python/pyshark) 専用開発ガイド
-   **[docs/instructions_for_vis.md](docs/instructions_for_vis.md)**: マシン 3 (Processing/Java) 専用開発ガイド

---

## 概要

**Ethernet River** は、ネットワークパケットの流れを「光の川」としてリアルタイムに可視化するインタラクティブ・インスタレーションです。Raspberry Pi 4B/5 を用いた分散システムで、ネットワークトラフィックのキャプチャ・解析・送信・描画・物理的介入（Kinect）までを一貫して実現します。

---

## システム構成

| マシン       | 役割                     | ディレクトリ                       | 主な技術                          |
| :----------- | :----------------------- | :--------------------------------- | :-------------------------------- |
| **マシン 1** | パケット解析・OSC 送信   | [`eth_river/`](eth_river/)         | Python 3.12+, pyshark, python-osc |
| **マシン 2** | Kinect 認識・OSC 送信    | （未着手）                         | Python, OpenCV, python-osc 予定   |
| **マシン 3** | 可視化・シミュレーション | [`eth_river_vis/`](eth_river_vis/) | Java 17, Processing 4, oscP5      |

---

## 体験の流れ

1. **キャプチャ**  
   マシン 1（Raspberry Pi 4B）がネットワークブリッジ上で全パケットをキャプチャ。
2. **解析**  
   パケットをプロトコル（HTTP, HTTPS/TLS, DNS, QUIC, TCP ハンドシェイク等）ごとに分類し、SNI や方向性（上り/下り）を特定。
3. **送信**  
   解析したメタデータ（プロトコル名、パケット長、方向、ドメイン名等）を OSC でマシン 3 へ即時送信。
4. **可視化**  
   マシン 3（Raspberry Pi 5）が Processing で OSC データを受信し、パケットを「光の川」として描画。プロトコルで色、パケット長でサイズ、方向で流れを変化。
5. **介入**  
   マシン 2（Kinect）が物理的な「石」や来場者を認識し、その座標を OSC で送信。マシン 3 で川の流れ（パーティクル挙動）に影響を与える。

---

## ディレクトリ構成

```
eth_river/         # マシン1: パケットキャプチャ・解析・OSC送信 (Python)
eth_river_vis/     # マシン3: 可視化・シミュレーション (Java/Processing)
docs/              # 技術仕様・設計・運用ドキュメント
.github/           # Copilot/CI用設定
```

---

## ビルド・実行方法

### マシン 1（パケット解析: Python）

```sh
cd eth_river
python -m pip install -r requirements.txt
python main.py
```

-   `main.py` の `TARGET_IP` は本番時にマシン 3 の IP へ変更必須。

### マシン 3（可視化: Java/Processing）

```sh
cd eth_river_vis
./gradlew build
# IDEで Main クラスを実行、または ./gradlew run (要 applicationプラグイン)
```

-   UDP ポート 12345 で OSC メッセージを受信し、パーティクルを描画。

---

## OSC 通信仕様

-   **マシン 1 → マシン 3**

    -   ポート: 12345
    -   アドレス: `/packet/{protocol_name}`
    -   引数:
        1. `String` プロトコル名
        2. `int` パケット長
        3. `String` 詳細（例: SNI）
        4. `String` 送信元 IP
        5. `String` 宛先 IP

-   **マシン 2 → マシン 3**（予定）
    -   ポート: 12346
    -   アドレス: `/kinect/object`
    -   引数: `int` (ID), `float` (x), `float` (y), `float` (angle)

---

## 開発・設計のポイント

-   **リアルタイム性重視**: パケットキャプチャから描画までの遅延を最小化。
-   **分散処理**: 3 台の Raspberry Pi が OSC で連携。
-   **パフォーマンス最適化**:
    -   Python 側は`pyshark`の XML パース負荷を軽減するため、`use_ek=True`（EK JSON モード）推奨。
    -   Processing 側はパーティクル削除を逆順ループで行い、描画負荷を抑制。
-   **属性アクセスの統一**: Python 側は`utils.get_nested_attr()`で None 安全なアクセスを徹底。
-   **未定義プロトコル**: DATA, STUN 等は現状`default_handler`で処理または無視。

---

## 現状の ToDo・課題

-   [ ] **TCP ハンドシェイクの OSC 送信**（`tcp_handler.py`で SYN/FIN/RST 検出）
-   [ ] **QUIC プロトコル対応**（SNI 取得・可視化）
-   [ ] **Kinect 連携の実装**（OSC 受信スタブ・介入ロジック）
-   [ ] **パフォーマンス検証・最適化**（CPU 負荷・フレームレート）
-   [ ] **コードの TODO/デバッグ残骸の整理**（保守性向上）

---

## 参考ドキュメント

-   [docs/summary.md](docs/summary.md): プロジェクト全体サマリー
-   [docs/instructions_for_main.md](docs/instructions_for_main.md): マシン 1（pyshark）開発ガイド
-   [docs/instructions_for_vis.md](docs/instructions_for_vis.md): マシン 3（Processing）開発ガイド
-   [docs/CPU_thread.md](docs/CPU_thread.md): パフォーマンス考察
-   [docs/DECISIONS.md](docs/DECISIONS.md): 設計決定履歴

---

## ライセンス

-   本リポジトリには LICENSE ファイルが存在しません。必要に応じて追加してください。

---

## 謝辞

-   Processing — The Processing Foundation
-   oscP5 — Andreas Schlegel
-   PyShark — Kimi Newt
