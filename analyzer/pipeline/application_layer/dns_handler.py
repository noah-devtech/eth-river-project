# pipeline/transport_layer/application_layer/dns_handler.py
from typing import Any, Dict, List

from utils import format_output, no_higher_layer

# TODO パケットnumber 23,30,35を確認


def process(packet: Any, layers: List[Any], context: Dict[str, Any]) -> None:
    """
    レイヤー7 (DNS) の処理。
    """
    if not layers or layers[0].layer_name != "dns":
        no_higher_layer(layers, "dns")
        return

    # dns_layer = layers.pop(0)

    format_output(context, "DNS")
