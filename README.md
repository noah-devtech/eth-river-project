# Ethernet River: パケット可視化インスタレーション

## Overview

> **Visualizing network traffic flow using OSC protocol.**

**Ethernet River** は、ネットワークパケットの流れを「光の川」としてリアルタイムに可視化するインタラクティブ・インスタレーションです。

* **Purpose:** 目に見えないネットワーク通信の量と流れを、直感的に理解可能な形で物理空間に投影すること。
* **Architecture:**
  * **Capture:** Raspberry Pi上で `Pyshark` を用い、パケットをリアルタイム解析。
  * **Communication:** 解析データをOpenSound Control (OSC) プロトコルで転送。
  * **Visualization:** Java (Processing) 側で受信データに基づきパーティクルを生成・描画。

## Demo

![eth-river-demo](eth-river.gif)

## Tech Stack

<div style="white-space: nowrap;">
  <img src="https://img.shields.io/badge/python-3670A0?logo=python&logoColor=ffdd54" alt="Python" />
  <img src="https://img.shields.io/badge/uv-DE5FE6?logo=python&logoColor=white" alt="uv" />
  <img src="https://img.shields.io/badge/java-%23ED8B00.svg?logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Gradle-02303A?logo=Gradle&logoColor=white" alt="Gradle" />
  <img src="https://img.shields.io/badge/Processing-003366?&logoColor=white" alt="Processing">
  <img src="https://img.shields.io/badge/Linux-FCC624?logo=linux&logoColor=black" alt="Linux" />
  <img src="https://img.shields.io/badge/git-%23F05033.svg?logo=git&logoColor=white" alt="Git" />
  <img src="https://img.shields.io/badge/-GitHub-181717.svg?logo=github" alt="Github" />
  <img src="https://img.shields.io/badge/Wireshark-1679A7?logo=wireshark&logoColor=white" alt="Wireshark" />
  <img src="https://img.shields.io/badge/-Raspberry Pi-C51A4A?logo=Raspberry-Pi" alt="Raspberry-Pi" />
</div>

## Architecture

| プログラム       | 役割                       | ディレクトリ                         | 主な技術                          |
| :--------------- | :------------------------- | :----------------------------------- | :-------------------------------- |
| **プログラム 1** | パケット解析・OSC 送信     | [`analyzer/`](analyzer/)             | Python 3.12+, pyshark, python-osc |
| **プログラム 2** | 可視化・シミュレーション   | [`visualizer/`](visualizer/)         | Java 17, Processing 4, oscP5      |
| **プログラム 3** | 外部Captive Portalサーバー | [`captive-server/`](captive-server/) | docker, Nginx, FastAPI, SQLite    |

### Network Infrastructure

本システムはイベント会場での安定稼働を目的として、以下の機材で構築する予定です。

* **WAN:** Speed Wi-Fi HOME 5G L13
* **Router:** Yamaha FWX120 (NAPT/Port Mirroring)
* **AP:** Cisco Aironet AIR-AP1832I-Q-K9
* **Switch:** Buffalo BS-GS2008P (PoE+ Smart Switch)

## How to use?

### プログラム 1（パケット解析: Python）

```sh
cd analyzer
uv run main.py
```

### プログラム 2（可視化: Java/Processing）

```sh
cd visualizer
./gradlew build
./gradlew run
```

### プログラム 3（Captive Portalサーバー: Docker）

```bash
# Now developing...
```

## Documentation Links

  * [ソフトウェア的なアーキテクチャーについて](docs/ARCHITECTURE.md)
  * [ネットワーク周りのインフラについて](docs/INFRASTRUCTURE.md)
  * [TODOリスト](docs/TODO.md)

## Notes

### **About This Project**

このプロジェクトはインタラクティブなインスタレーション展示専用に設計されています。そのため、アーキテクチャは特定のネットワークトポロジー（Yamaha FWX120 + Cisco Aironet + Buffalo BS-GS2008P）に高度に最適化されています。

> This project is specifically designed for interactive installation use. Therefore, the architecture is highly optimized for a specific network topology (Yamaha FWX120 + Cisco Aironet + Buffalo BS-GS2008P).

### **Tested Environment**

* Analyzer (Program 1)
  * Hardware: Raspberry Pi 4B (8GB)/ dynabook RZ/MW(i7-1360P, 32GB)
  * OS: Raspberry Pi OS (Kernel 6.12, aarch64)/ Windows 11 (25H2, x64)
  * Python: 3.12.x or higher
  * Dependencies:
    * pyshark: 0.6

      To running pyshark, `tshark` (part of Wireshark) must be installed on the system.

    And other dependencies listed in `analyzer/uv.lock`
* Visualizer (Program 2)
  * Hardware: dynabook RZ/MW(i7-1360P, 32GB, Xe Graphics)
  * OS: Windows 11 (25H2, x64)
  * Java: OpenJDK 17.0.x

  And other dependencies listed in `visualizer/build.gradle.kts`
  * Processing: 4.4.10

* Network Hardware: Yamaha FWX120, Cisco Aironet 1832I, Buffalo BS-GS2008P
