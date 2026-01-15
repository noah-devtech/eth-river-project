import os
import sys

import pyshark
from dotenv import load_dotenv
from pythonosc import udp_client

from pipeline import link_layer
from utils import (
    get_lan_network,
    get_nested_attr,
)  # 共通関数はutils.pyから読み込む

load_dotenv()
display_filter = os.getenv("DISPLAY_FILTER", "ip.version == 4")
is_read_mode = os.getenv("IS_READ_MODE", "False").lower() == "true"
read_file = "test-data\\pcap\\test.pcapng"
NETWORK_INTERFACE = os.getenv("NETWORK_INTERFACE", "Wi-Fi")
TARGET_IP = os.getenv("TARGET_IP", "127.0.0.1")
TARGET_PORT = int(os.getenv("TARGET_PORT", "12345"))

OSC_CLIENT = udp_client.SimpleUDPClient(TARGET_IP, TARGET_PORT)
print(f"[*] OSC sending to {TARGET_IP}:{TARGET_PORT}")


def main():
    lan_network = get_lan_network()
    if not lan_network:
        print("[!] Could not determine local network. Exiting.")
        sys.exit(1)

    print(f"[*] Your LAN Network is: {lan_network}")
    print(f"[*] Capturing on interface: {NETWORK_INTERFACE}")
    print("[*] Press Ctrl+C to stop capturing.")
    if is_read_mode:
        capture_session = pyshark.FileCapture(
            read_file, override_prefs={"tcp.desegment_tcp_streams": "TRUE"}
        )
    else:
        capture_session = pyshark.LiveCapture(
            interface=NETWORK_INTERFACE,
            display_filter=display_filter,
            override_prefs={"tcp.desegment_tcp_streams": "TRUE"},
        )
    try:
        if is_read_mode:
            for packet in capture_session:
                time_epoch = float(get_nested_attr(packet, "frame_info.time_epoch"))
                context = {
                    "lan_network": lan_network,
                    "packet_number": packet.number,
                    "timestamp": time_epoch,  # floatのまま保持
                    "length": packet.length,
                    "osc_client": OSC_CLIENT,
                }
                link_layer.process(packet, packet.layers.copy(), context)
        else:
            for packet in capture_session.sniff_continuously():
                time_epoch = float(get_nested_attr(packet, "frame_info.time_epoch"))
                context = {
                    "lan_network": lan_network,
                    "packet_number": packet.number,
                    "timestamp": time_epoch,  # floatのまま保持
                    "length": packet.length,
                    "osc_client": OSC_CLIENT,
                }
                link_layer.process(packet, packet.layers.copy(), context)

    except KeyboardInterrupt:
        print("\n[*] Capture stopped by user.")
        sys.exit(0)
    except Exception as e:
        print(f"[!] An error occurred: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
