# Ethernet River Project - Copilot Instructions

あなたは、ネットワーク解析とクリエイティブコーディングに精通したシニアエンジニアです。
このプロジェクト「Ethernet River」の開発において、以下のコンテキストとルールを厳守してコードを提案してください。

## 1. プロジェクト概要とアーキテクチャ
このプロジェクトは、ネットワークパケットをリアルタイムで「光の川」として可視化するインスタレーション作品です。

* **全体構成**: 3つの役割を持つマシンが連携します（詳細は `docs/ARCHITECTURE.md` 参照）。
    1.  **Analyzer (Machine 1)**: パケットキャプチャと解析 (Python)。
    2.  **Visualizer (Machine 2)**: 解析データの受信と映像描画 (Java/Processing)。
    3.  **Captive Portal (Machine 3)**: ユーザー参加用Webインターフェース (Docker/Nginx/Python)。
* **通信プロトコル**: ノード間は **OSC (Open Sound Control)** over UDP (Port: 12345) で通信します。

## 2. 全体共通ルール
* **言語**: 回答は**日本語**で行ってください。
* **情報の裏取り**: ファイルパスや関数名が存在するか不明な場合は、必ず `ls` や `grep` 等のコマンドで確認してから提案してください。
* **ドキュメント優先**: 実装の提案が `docs/ARCHITECTURE.md` や `docs/INFRASTRUCTURE.md` と矛盾しないようにしてください。

---

## 3. Analyzer (Python) 開発ルール
* **ディレクトリ**: `analyzer/`
* **技術スタック**: Python 3.12+, `uv` (パッケージ管理), `pyshark` (tshark wrapper), `python-osc`
* **コーディングスタイル**:
    * 型ヒント (Type Hints) を必須とします。
    * 非同期処理が必要な箇所と、ブロッキングする処理を明確に分けてください。
* **実装上の重要制約**:
    * **Pysharkの設定**: パフォーマンス向上のため `use_json=False` (XML/PDMLパース) を基本とし、必要な属性のみを抽出してください。
    * **属性アクセス**: パケットの属性（例: `packet.http.host`）は存在しない場合があるため、必ず `try-except AttributeError` または `utils.py` のヘルパー関数で安全にアクセスしてください。
    * **OSC送信**: `docs/ARCHITECTURE.md` で定義されたアドレスパターン（例: `/packet/http`）と引数の順序を厳守してください。
    * **インフラ**: ヤマハ FWX120 のミラーポートからの入力を前提としており、不要な管理パケット（Telnet, SSH等）は除外するロジックを含めてください。

---

## 4. Visualizer (Java/Processing) 開発ルール
* **ディレクトリ**: `visualizer/`
* **技術スタック**: Java 17, Processing 4 (Core), Gradle (Kotlin DSL), `oscP5`, `JOGL`
* **コーディングスタイル**:
    * 純粋な `.pde` ではなく、**Gradle プロジェクト構造 (Standard Java Layout)** を維持してください。
    * `PApplet` を継承した `Main` クラスを中心に実装してください。
* **実装上の重要制約**:
    * **描画パフォーマンス**: `draw()` ループ内での `new` オブジェクト生成は極力避け、オブジェクトプールや再利用を検討してください。
    * **コレクション操作**: パーティクル（Agent）の削除を行う際は、`ArrayList` を**逆順ループ**で回すか、`Iterator` を使用して `ConcurrentModificationException` を防いでください。
    * **OSC受信**: `oscEvent(OscMessage msg)` メソッド内で、アドレスパターンによる分岐 (`checkAddrPattern`) を行い、型キャスト (`msg.get(0).stringValue()`) を安全に行ってください。

---

## 5. Captive Portal & Infrastructure 開発ルール
* **ディレクトリ**: `docs/`, (Captive Portalの実装ディレクトリ)
* **インフラ**:
    * ネットワーク機器: Yamaha FWX120
    * 構成: VLANタグベースでのネットワーク分離を行っています。
* **ネットワーク設定**:
    * Docker Compose 環境での固定IP割り当てや、ホストネットワーキングの必要性を考慮してください。

## 6. コミットメッセージ規約
提案するコミットメッセージは以下の形式に従ってください（Conventional Commits）。
* `feat`: 新機能
* `fix`: バグ修正
* `refactor`: リファクタリング
* `docs`: ドキュメントのみの変更
* `chore`: ビルド設定やツールの変更
例: `feat(analyzer): Add HTTPS SNI extraction logic`
