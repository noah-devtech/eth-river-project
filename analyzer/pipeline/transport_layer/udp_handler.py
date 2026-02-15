# pipeline/transport_layer/udp_handler.py
from typing import Any, Dict, List

from utils import format_output, get_nested_attr

from ..application_layer import dns_handler

# 宛先ポート番号と担当ハンドラーの対応表
APPLICATION_HANDLERS = {
    "dns": dns_handler,
}
NOISE_PROTOCOLS={"mdns","ssdp","lmnr","dhcp","nbus","netbios_ns","igmp"}


def process(packet: Any, layers: List[Any], context: Dict[str, Any]) -> None:
    """
    レイヤー4 (UDP) の処理。
    """
    if not layers or layers[0].layer_name != "udp":
        print("This is not UDP packet")
        return

    udp_layer = layers.pop(0)

    context["source_port"] = get_nested_attr(udp_layer, "srcport")
    context["dest_port"] = get_nested_attr(udp_layer, "dstport")

    # QUIC 判定
    is_quic_layer = (
        len(layers) > 0 and get_nested_attr(layers[0], "layer_name") == "quic"
    )

    if (
        is_quic_layer
        or context["dest_port"] == "443"
        or context["source_port"] == "443"
    ):
        format_output(context, "QUIC")
        return

    if layers:
        app_layer_name = get_nested_attr(layers[0], "layer_name")
        if app_layer_name in APPLICATION_HANDLERS:
            APPLICATION_HANDLERS[app_layer_name].process(packet, layers, context)
            return

        if app_layer_name in NOISE_PROTOCOLS:
            format_output(context, "NOISE")
            return

    length_str = get_nested_attr(udp_layer, "length")
    length = int(length_str) if length_str else 0

    if length > 8:
        # ヘッダー以上のサイズがあればデータあり
        format_output(context, "DATA")
    else:
        format_output(context, "UDP")
