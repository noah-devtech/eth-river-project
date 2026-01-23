# pipeline/transport_layer/application_layer/default_handler.py
from typing import Any, Dict, List

from utils import format_output, get_nested_attr, has_nested_attr


def process(packet: Any, layers: List[Any], context: Dict[str, Any]) -> None:
    """
    未対応のプロトコルやポートの処理。ここでパイプラインは終了。
    """
    if not len(layers) == 0 and has_nested_attr(layers[0], "layer_name"):
        protocol = get_nested_attr(layers[0], "layer_name")
        format_output(context, protocol)
    protocol = "Other"
    format_output(context, protocol)
