# pipeline/network_layer.py
from typing import Any, Dict, List

from utils import get_nested_attr

from .application_layer import default_handler
from .transport_layer import tcp_handler, udp_handler

# プロトコル番号と担当ハンドラーの対応表
TRANSPORT_HANDLERS = {"17": udp_handler, "6": tcp_handler}


def process(packet: Any, layers: List[Any], context: Dict[str, Any]) -> None:
    """
    レイヤー3 (IP) の処理。
    """
    if not layers or layers[0].layer_name not in ["ip", "ipv6"]:
        print("network layer : unknown protocol")
        return

    ip_layer = layers.pop(0)

    context["source_ip"] = get_nested_attr(ip_layer, "src")
    context["dest_ip"] = get_nested_attr(ip_layer, "dst")

    protocol_number = get_nested_attr(ip_layer, "proto")

    # 対応表から担当ハンドラーを探す
    handler = TRANSPORT_HANDLERS.get(protocol_number, default_handler)

    handler.process(packet, layers, context)
