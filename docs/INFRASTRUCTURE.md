# Infrastructure

## Topology

```mermaid
graph LR

  subgraph core_network [Core Network]
    subgraph FWX120 [Router: Yamaha FWX120]
      LAN2[LAN2:WAN ports]
    subgraph LAN1[LAN1:LAN ports]
      direction TB
        LAN1_1[Ports:1]
        LAN1_2[Ports:2]
        LAN1_3[Ports:3]
        LAN1_4[Ports:4]
    end
  end

  subgraph BSW [Buffalo BS-GS2008P PoE+ Smart Switch]
    subgraph L2Switch [L2 Switch]
      direction TB
        BSW_1[Ports:1]
        BSW_2[Ports:2]
        BSW_3[Ports:3]
        BSW_4[Ports:4]
        BSW_5[Ports:5]
        BSW_6[Ports:6]
        BSW_7[Ports:7]
        BSW_8[Ports:8]
      end
    end
  end

  subgraph Main_PC [メインPC]
    subgraph OS_Internal [OS]
      direction TB
        Analyzer -- "OSC (127.0.0.1)" --> Visual
    end
    subgraph NICs [Network Interface Cards]
      direction TB
        NIC_Onboard[Onboard NIC]
        NIC_331T_1[331T Port 1]
        NIC_331T_2[331T Port 2]
        NIC_331T_3[331T Port 3]
        NIC_331T_4[331T Port 4]
    end
  end

  Internet((Internet)) --> Router[ホームルーター <br> or <br> モバイルルーター]
  Router -- DMZ --> LAN2
  LAN2 -- NAPT --> LAN1
  BSW_6 --> NIC_331T_2
  %% trunk connection
  LAN1_1 --|VLAN 10,20|--> BSW_8


  %% Packet Sniffing
  NIC_331T_1 --> Analyzer
  BSW_7 --|VLAN 20|--> NIC_331T_1
  BSW_8 -- "Mirroring(VLAN20のみ)" --> BSW_7

  subgraph Guest_Access
    BSW_1 -- "Port 1 (PoE+)" --- AP1[Cisco 1832I]
    BSW_2 -- "Port 2 (PoE+)" --- AP2[Cisco 1832I]
    BSW_3 -- "Port 3 (PoE+)" --- AP3[Cisco 1832I]
    AP1 -.-> WiFi_Guest((Visitor WiFi <br> VLAN 20))
    AP2 -.-> WiFi_Guest((Visitor WiFi <br> VLAN 20))
    AP3 -.-> WiFi_Guest((Visitor WiFi <br> VLAN 20))
    AP1 -.-> WiFi_Management((Management WiFi <br> VLAN 10))
    AP2 -.-> WiFi_Management((Management WiFi <br> VLAN 10))
    AP3 -.-> WiFi_Management((Management WiFi <br> VLAN 10))
  end
  BSW_8 --|VLAN 10,20|-->BSW_1 & BSW_2 & BSW_3
```

## Hardware Selection

* Router (Yamaha FWX120)
  * コンシューマー機では耐えられないNATセッション数を捌くため。
  * ステートフルインスペクションとポリシーフィルターで柔軟な通信制御を行うため。

* AP (Cisco Aironet/Catalyst)
  * 干渉対策と接続安定性のため。

* Switch (Buffalo BS-GS2008P PoE+ Smart Switch)
  * PoE+給電対応でAPを動作させるため。
  * VLAN対応でセグメント分離を行うため。
  * ミラーポート機能を用いてパケットキャプチャーをするため。
  * Web GUIで簡単に設定できるため。

## Network Settings

### VLANの割当

| ゾーン      | VLAN ID | ネットワーク     | 役割                           |
| ----------- | ------- | ---------------- | ------------------------------ |
| DMZ（WAN）  | -       | DHCP             | ホームルーター<-->FWX120       |
| Native VLAN | VLAN1   | 192.168.xxx.0/24 | FWX120やBS-GS2008Pなどの管理用 |
| Trust       | VLAN10  | 10.0.10.0/24     | 管理用・CiscoAP                |
| Guest       | VLAN20  | 10.0.20.0/24     | CiscoAP・ゲスト端末            |

### 通信制御の全体コンセプト（マトリクス図）

セグメント間の通信を「デフォルトは遮断、必要なものだけ許可」というコンセプトで設計。
フィルターではなくポリシーフィルターで設定。

| 送信元 / 宛先      | Trust (10) | Guest (20) | Internet (WAN) |
| :----------------- | :--------- | :--------- | :------------- |
| **Trust (管理)**   | 許可       | 許可       | 許可           |
| **Guest (ゲスト)** | 遮断       | 許可       | 許可           |

### 通信ポリシー (L3/L4 Filter)

1. Guest(VLAN20) -> DNS(TCP/53,UDP/53), Internet(TCP/80,443), Mail： 許可
2. Trust(VLAN10) -> All： 許可

### 監視設計

* ミラーポート設定
  * Buffalo BS-GS2008PのPort8のトラフィックをPort7にミラーリング
  * メインPCのNICをPort7に接続
* パケットキャプチャ設定
  * PCのNICはIPアドレスを設定しない（パケット受信用のみ）
  * ミラーされたパケットはVLAN20タグ付きで届くため、OS側でVLANタグを剥がす設定を行う
  * Analyzerは剥がされたパケットを受信
  * 接続されたPCのNICを「プロミスキャスモード（`promisc`）」に設定

### Wi-Fiについて

#### SSID構成

| SSID名              | 紐付けVLAN | 認証方式   | 備考             |
| ------------------- | ---------- | ---------- | ---------------- |
| EthernetRiver-Admin | VLAN 10    | WPA2/3-PSK | 管理者・設備用。 |
| EthernetRiver-Guest | VLAN 20    | WPA2/3-PSK | 展示端末用。     |

#### 詳細仕様

* アイソレーション: 有効（P2P Blocking）。ゲスト向けWi-Fi内クライアント間通信を禁止。
* VLAN20のDHCP: DNSサーバーとして8.8.8.8を明示的に指定する。
