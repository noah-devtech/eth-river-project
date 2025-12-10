# マシン 3 (Processing/Java) 開発ガイド

> このファイルは **マシン 3 (`visualizer/` ディレクトリ)** の開発・運用・設計指針を記載した、サブプロジェクト専用のドキュメントです。
> プロジェクト全体の概要や他のマシンの情報は [docs/summary.md](summary.md) を参照してください。

---

## 1. マシン 3 の役割と責務

**役割**: 可視化・シミュレーション・物理的介入の統合

**ハードウェア**: Raspberry Pi 5

**主要な処理フロー**:

1. **OSC 受信**: マシン 1（パケット情報）とマシン 2（Kinect 情報）から 2 系統の OSC データを受信
2. **データ解釈**: パケットメタデータ（プロトコル、IP、長さ）に基づき、粒子の属性（色、サイズ、方向）を決定
3. **シミュレーション**: パーティクルシステムを管理し、物理演算を実行（現在はレベル 1: 等速度運動）
4. **介入処理**: Kinect 座標に基づき、パーティクルの挙動に「介入」するロジックを適用（未実装）
5. **描画**: シミュレーション結果を Processing ウィンドウに描画

---

## 2. 技術スタックと主要ライブラリ

-   **言語**: Java 17
-   **フレームワーク**: Processing 4 (PApplet クラスを継承)
-   **ビルドシステム**: Gradle
-   **主要ライブラリ**:
    -   `oscP5`: OSC 通信の受信用ライブラリ
-   **設定ファイル**:
    -   `src/main/resources/config.properties`
    -   `src/main/resources/secrets.properties`
    -   （注: 現在は一時的にハードコーディングで対応中）

---

## 3. ディレクトリ構成とファイル概要

```
visualizer/
├── build.gradle.kts           # Gradleビルド設定
├── settings.gradle.kts        # Gradleプロジェクト設定
├── gradlew / gradlew.bat      # Gradleラッパー
└── src/main/
    ├── java/
    │   └── Main.java          # メインクラス（setup, draw, oscEvent等）
    └── resources/
        └── config.properties  # 設定ファイル
```

---

## 4. 現状の実装と完了済み機能

-   **P0: OSC 受信 (pyshark)**: `oscP5` を使用し、ポート `12345` でマシン 1 (pyshark) からの OSC メッセージ受信に成功。
-   **P1: パーティクルシステム (レベル 1)**:
    -   `Particle` クラス（位置, 速度, 色, サイズ）を実装。
    -   `ArrayList<Particle>` でパーティクルを管理。
    -   `oscEvent` で受信したデータ（プロトコル, パケット長）に基づき、色とサイズを変えてパーティクルを生成。
-   **P1: 方向性（上下）の実装**:
    -   `localNetPrefix` 変数（ハードコーディング）に基づき、`srcIp` と `dstIp` を比較。
    -   「Upstream」（下から上へ）と「Downstream」（上から下へ）で、パーティクルの発生位置と初期速度を変更するロジックを実装済み。
-   **P1: 安全なパーティクル削除**: `draw()` ループ内で `ArrayList` を**逆順（後ろから）**処理し、`isDead()`（画面外判定）で安全に要素を削除するロジックを実装済み。

### 未完了の主要タスク

詳細な TODO リストは **[docs/TODO.md](TODO.md)** を参照してください。

マシン 3 関連の主要タスク:

-   Kinect OSC 受信スタブ作成（P2-III）
-   介入ロジック実装（P2-IV）
-   TCP 制御パケットの可視化（P3-Vis）
-   シミュレーション高度化（P3-Sim）
-   ノードベース可視化システム（P4-NodeVis）

---

## 5. OSC 受信仕様

-   **OSC 仕様 (pyshark から)**: ポート `12345` で受信。
    -   **アドレス**: `/packet/{protocol_name}` (例: `/packet/dns`, `/packet/tls_hello`)。
    -   **引数 (順序固定)**:
        1.  `String`: プロトコル名 (例: "DNS", "TLS-Hello")
        2.  `int`: パケット長 (例: 120)
        3.  `String`: 詳細 (例: "SNI: google.com")
        4.  `String`: 送信元 IP (例: "192.168.11.50")
        5.  `String`: 宛先 IP (例: "1.1.1.1")
-   **OSC 仕様 (Kinect から)**: （仮仕様）
    -   **ポート**: `12346` （`12345` とは別）
    -   **アドレス**: `/kinect/object`
    -   **引数**: `int` (ID), `float` (x), `float` (y), `float` (angle)
-   **方向判定ロジック**:
    -   `localNetPrefix` 変数（ハードコーディング）と `ip.startsWith()` で `srcIsLocal` と `dstIsLocal` を判定。
    -   `srcIsLocal && !dstIsLocal` → **Upstream**（下から上へ）
    -   `!srcIsLocal && dstIsLocal` → **Downstream**（上から下へ）
    -   その他（Local, Unknown） → **Downstream**（上から下へ）
-   **パーティクルクラス (`Particle`)**:
    -   以下の属性を持つ: `PVector pos`, `PVector vel`, `color c`, `float size`。
    -   `update()`: `pos.add(vel)` というレベル 1 の物理演算を実装。
    -   `isDead()`: `pos.y` が画面外（上下）に出たか判定する。

---

## 6. Particle クラスの実装仕様

**属性**:

-   `PVector pos`: 位置
-   `PVector vel`: 速度
-   `color c`: 色
-   `float size`: サイズ

**メソッド**:

-   `update()`: 現在はレベル 1（等速度運動: `pos.add(vel)`）
-   `display()`: 楕円として描画
-   `isDead()`: 画面外判定（`pos.y < -50` または `pos.y > height + 50`）

---

## 7. 開発時の注意点とベストプラクティス

### 7.1 パフォーマンス重視の実装

-   **RPi 5 のパフォーマンス限界を意識**
    -   パーティクル数が数千を超えた場合のフレームレート低下に注意
    -   高度な物理演算（P3-Sim）と Kinect 介入（P2-IV）の同時処理で負荷増大のリスク
-   **効率的なコーディング**
    -   不要な計算を削減
    -   オブジェクトプールの検討（頻繁な生成・削除を避ける）

### 7.2 安全な ArrayList 操作

-   パーティクル削除時は**必ず逆順ループ**で処理
    ```java
    for (int i = particles.size() - 1; i >= 0; i--) {
        Particle p = particles.get(i);
        if (p.isDead()) {
            particles.remove(i);
        }
    }
    ```
-   インデックスのずれを防ぐため、順方向ループでの削除は厳禁

### 7.3 通信方向判定

-   `localNetPrefix`変数（ハードコーディング）と`ip.startsWith()`で判定
-   **Upstream**: `srcIsLocal && !dstIsLocal` → 下から上へ
-   **Downstream**: `!srcIsLocal && dstIsLocal` → 上から下へ
-   その他（Local, Unknown） → Downstream 扱い

---

## 8. 現在の課題と今後の方針

### 最優先課題

-   **Kinect 連携の実装**: マシン 2 が未開発のため、P2-III（受信スタブ）から着手
-   **パフォーマンス検証**: 大量パーティクル＋物理演算時のフレームレート測定

### 今後の拡張

-   **TCP 制御パケット可視化**: pyshark 側の P2-1 実装後に対応
-   **シミュレーション高度化**: 加速度ベースの物理演算（レベル 3）への移行
-   **ノードベース可視化**: IP 間の通信を川の流れとして表現

詳細な TODO リストは [docs/TODO.md](TODO.md) を参照してください。

---

## 9. ビルド・実行方法

```bash
cd visualizer
./gradlew build
# IDEでMainクラスを実行、または ./gradlew run (要applicationプラグイン)
```

---

## 10. 関連ドキュメント

-   [docs/summary.md](summary.md): プロジェクト全体サマリー
-   [docs/instructions_for_analyzer.md](instructions_for_analyzer.md): マシン 1 開発ガイド
-   [docs/TODO.md](TODO.md): 全体 TODO リスト
-   [docs/DECISIONS.md](DECISIONS.md): 設計決定履歴
