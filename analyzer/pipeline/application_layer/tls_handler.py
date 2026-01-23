# pipeline/application_layer/tls_handler.py
from typing import Any, Dict, List

from utils import format_output, get_nested_attr, no_higher_layer


def process(packet: Any, layers: List[Any], context: Dict[str, Any]) -> None:
    """
    レイヤー7 (TLS/SSL) の処理。
    """

    # まず 'tls' レイヤー自体を取得
    tls_layer = get_nested_attr(packet, "tls")
    if not tls_layer:
        no_higher_layer(layers, "tls")
        return
    if context.get("dest_port") == "443" or context.get("source_port") == "443":
        format_output(context, "HTTPS")
        return
    format_output(context, "TLS")
