# User Experience Flow (Ethernet River)

Ethernet Riverは、ネットワークパケットという「見えないデジタル信号」を可視化するインタラクティブ・インスタレーションです。
来場者は自身の端末を接続することで、この光の川の一部として参加することができます。

このドキュメントでは、ユーザーが体験する一連のフローと、それに伴うシステムの挙動を記述します。

---

## Phase 1: Connection

ユーザーが会場に足を踏み入れ、Wi-Fiに接続した直後の状態。

* **User Action:**
  * 会場のWi-Fi SSID `EthernetRiver-Guest` に接続する。
* **System Behavior:**
  * DHCPサーバーがIPアドレス（例: `10.0.20.50`）を割り当てる。
  * パケットキャプチャ (Analyzer) が通信を検知する。
  * AnalyzerからVisualizerにOSCメッセージが送信される。
* **Visual Output:**
  * **(Nothing)**
  * Visualizerにはまだ名前が紐付いていないため、「同意されてない通信」と判断しパーティクルやノードは一切表示しない。

## Phase 2: Identification

ユーザーが自身の存在をシステムに登録し、実体を持つフェーズ。

* **User Action:**
  * スマートフォンのブラウザを立ち上げる（または自動的にポップアップする）。
  * Captive Portal画面が表示される。
  * 自分のニックネーム（例: `NOAH`）を入力した上で、利用規約に同意し送信ボタンを押す。
* **System Behavior:**
  * Captive Portalが `IP: 10.0.20.50` と `Name: NOAH` を紐付ける。
  * VisualizerへOSCメッセージで通知を送る。
* **Visual Output:**
  * **`NOAH`** と表示されたノードが他のノードと通信（=光の粒/パーティクルの流れ）を行う。

## Phase 3: Interaction

ユーザーが意図的に通信を行うことで、作品に干渉するフェーズです。

* **User Action:**
  * Webサイトを見る、SNSを更新するなど、通信を行う。
* **System Behavior:**
  * Analyzerがパケット量やプロトコル（HTTP/DNS/TLS）を解析。
  * AnalyzerからVisualizerにOSCメッセージが逐次送信される。
* **Visual Output:**
  * **`NOAH`** のノードから、通信の種類に応じた色（HTTP=青, DNS=緑など）のパーティクルが放出される。
  * 自分の操作に合わせて光が溢れ出し、物理空間のアートの一部となる。
