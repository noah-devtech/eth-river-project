# System Context

## Analyzer (Python)

### Pipeline Design

* pysharkの`LiveCapture()`を用い、リングバッファー構造ではなくストリーム処理`sniff_continuously`でパケットを取得。

* Layer Strategy
    1. Link Layer(Ethernet)
    2. Network Layer(IP)
    3. Transport Layer(TCP/UDP)
    4. Application Layer(DNS/TLS/HTTP)
    上記の順にOSI参照モデルに基づき責任連鎖（Chain of Responsibility）パターンで処理を委譲。

* Optimization
    `AttributeError`を防ぐため`get_nested_attr()` で安全に属性へアクセス。

## Visualizer (Java/Processing)

### Particle System

* Design Pattern
    Boidsアルゴリズム（群れシミュレーション）を応用している。

* Optimization
    描画負荷軽減のため、画面外に出たパーティクルは逆順ループで即時削除。
    パーティクル内の`PVector`などを使い回すことなどでGC発生を抑制。

* ArrayListによる動的なオブジェクト管理。

* Rendering
    加算合成 (ADD blend mode) を使用し、トラフィックの集中を「光の強さ」として表現。

## Communication

### OSC 通信仕様

* ポート: 12345

#### プログラム 1 → プログラム 2

* アドレス: `/packet/{protocol_name}`
* 引数:
    1. `string` 送信元IP
    2. `string` 宛先IP
    3. `int` パケットナンバー
    4. `int` パケットサイズ
    5. `float` タイムスタンプ

例としてAnalyzerからVisualizerへは以下のフォーマットで送信されます

* `/packet/http`: `[src_ip, dst_ip, number, size, timestamp]`
* `/packet/tls`: `[src_ip, dst_ip, number, size, timestamp]`
