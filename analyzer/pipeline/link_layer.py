# pipeline/link_layer.py
from typing import Any, Dict, List

from utils import get_nested_attr

from . import network_layer


def process(packet: Any, layers: List[Any], context: Dict[str, Any]) -> None:
    """
    レイヤー2 (イーサネット) の処理。
    """
    if not layers or layers[0].layer_name != "eth":
        print("link layer : unknown protocol")
        return

    eth_layer = layers.pop(0)  # 先頭のレイヤーを処理してリストから削除

    context["source_mac"] = get_nested_attr(eth_layer, "src")
    context["dest_mac"] = get_nested_attr(eth_layer, "dst")

    # 次のレイヤーの処理を呼び出す
    network_layer.process(packet, layers, context)
