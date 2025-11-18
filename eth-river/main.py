import pyshark
import sys
import socket
import ipaddress
from pipeline import link_layer

from utils import (
    get_lan_network,
    get_traffic_direction,
    get_nested_attr,
)  # 共通関数はutils.pyから読み込む


from pythonosc import udp_client
TARGET_IP = "127.0.0.1"
TARGET_PORT = 12345
OSC_CLIENT = udp_client.SimpleUDPClient(TARGET_IP, TARGET_PORT)
print(f"[*] OSC sending to {TARGET_IP}:{TARGET_PORT}")

display_filter = "ip.version == 4"

network_interface = "Wi-Fi"

is_read_mode = True
read_file = "test-data\\pcap\\test.pcapng"

def main():
    lan_network = get_lan_network()
    if not lan_network:
        print("[!] Could not determine local network. Exiting.")
        sys.exit(1)

    print(f"[*] Your LAN Network is: {lan_network}")
    print(f"[*] Capturing on interface: {network_interface}")
    print(f"[*] Press Ctrl+C to stop capturing.")
    if is_read_mode:
        capture_session = pyshark.FileCapture(
            read_file,
            override_prefs={"tcp.desegment_tcp_streams": "TRUE"}
        )
    else:
        capture_session = pyshark.LiveCapture(interface=network_interface,display_filter=display_filter, override_prefs={'tcp.desegment_tcp_streams': 'TRUE'})
    try:
        if is_read_mode:
            for packet in capture_session:
                time_epoch = float(get_nested_attr(packet, "frame_info.time_epoch"))
                context = {
                    "lan_network": lan_network,
                    "packet_number": packet.number,
                    "timestamp": time_epoch,  # floatのまま保持
                    "length": packet.length,
                    "osc_client": OSC_CLIENT
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
                    "osc_client": OSC_CLIENT
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
