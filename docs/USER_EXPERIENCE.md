# User Experience Flow (Ethernet River)

このドキュメントでは、ユーザーが体験する一連のフローと、それに伴うシステムの挙動を記述します。

---

## Interaction

ユーザーが意図的に通信を行うことで、作品に干渉するフェーズです。

* **User Action:**
  * Webサイトを見る、SNSを更新するなど、通信を行う。
* **System Behavior:**
  * Analyzerがパケット量やプロトコル（HTTP/DNS/TLS）を解析。
  * AnalyzerからVisualizerにOSCメッセージが逐次送信される。
* **Visual Output:**
  * ノードから、通信の種類に応じた色（HTTP=青、 DNS=緑など）のパーティクルが放出される。
  * 自分の操作に合わせて光が溢れ出し、物理空間のインスタレーションの一部となる。
