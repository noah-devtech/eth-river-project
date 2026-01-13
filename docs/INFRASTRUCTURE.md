# Infrastructure

## Topology

```mermaid
graph TD
    Internet((Internet)) --> Router[ホームルーター <br> or <br> モバイルルーター]
    Router -- DMZ --> FWX120[Router: Yamaha FWX120]
    FWX120 -- DHCP/NAT --> L2Switch[L2 Switch]
    L2Switch --> RPi_Master[RPi 4: Analyzer]
    L2Switch --> CiscoAP[AP: Cisco Aironet/Catalyst]
    CiscoAP -.-> Smartphones[Guest Devices]
```


## Hardware Selection

* Router (Yamaha FWX120)
  * コンシューマー機では耐えられないNATセッション数を捌くため。
  * ミラーポートを用いてパケットキャプチャーをするため。

* AP (Cisco Aironet/Catalyst)
  * 干渉対策と接続安定性のため。
  * Captive Portalを用いた情報収集と個人情報収集に同意してもらうため。

## Network Settings
