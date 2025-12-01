import pyshark
import sys
import socket
import ipaddress

# netifacesがあれば、より正確なサブネットマスクを取得できる
try:
    import netifaces

    has_netifaces = True
except ImportError:
    has_netifaces = False

def get_lan_network():
    """
    実行中マシンのIPアドレスとサブネットマスクから、
    所属するLANのネットワークオブジェクトを取得する。
    """
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 1))
        local_ip = s.getsockname()[0]
    except Exception:
        return None
    finally:
        s.close()

    netmask = None
    if has_netifaces:
        try:
            iface = netifaces.gateways()["default"][netifaces.AF_INET][1]
            netmask = netifaces.ifaddresses(iface)[netifaces.AF_INET][0]["netmask"]
        except Exception:
            pass

    if not netmask:
        netmask = "255.255.255.0"
        print(
            f"[Warning] Could not determine netmask automatically. Assuming {netmask}."
        )

    lan_network = ipaddress.ip_network(f"{local_ip}/{netmask}", strict=False)
    return lan_network


def get_traffic_direction(source_ip, dst_ip, lan_network):
    """
    通信の方向を Inbound, Outbound, Internal で判定する。
    """
    try:
        src_ip_obj = ipaddress.ip_address(source_ip)
        dst_ip_obj = ipaddress.ip_address(dst_ip)

        src_is_local = src_ip_obj in lan_network
        dst_is_local = dst_ip_obj in lan_network

        if src_is_local and not dst_is_local:
            return "Outbound"
        elif not src_is_local and dst_is_local:
            return "Inbound"
        elif src_is_local and dst_is_local:
            return "Internal"
        else:
            return "Unknown"
    except ValueError:
        return "Invalid"


def get_nested_attr(obj, attr_string, default=None):
    """
    ネストした属性を安全に取得するユーティリティ関数。
    """
    attrs = attr_string.split(".")
    current_obj = obj
    for attr in attrs:
        if hasattr(current_obj, attr):
            current_obj = getattr(current_obj, attr)
        else:
            return default
    return current_obj

def has_nested_attr(obj, attr_string, default=None):
    """
    ネストした属性があるかどうかを安全に確認し返す。

    Args:
        obj (_type_): _description_
        attr_string (_type_): _description_
        default (_type_, optional): _description_. Defaults to None.
    """
    if get_nested_attr(obj, attr_string) is None:
        return False
    return True

def format_output(context, protocol, details):
    """
    最終的な出力形式を統一する関数。
    """
    length_str = f"{context.get("length","?")}"
    number_str = f"{context.get('packet_number', '?')}"
    time = context["timestamp"]  # floatのまま表示
    direction = get_traffic_direction(
        context.get("source_ip", "?"),
        context.get("dest_ip", "?"),
        context["lan_network"],
    )
    source_str = f"{context.get('source_ip', '?')}:{context.get('source_port', '?')}"
    dest_str = f"{context.get('dest_ip', '?')}:{context.get('dest_port', '?')}"
    if 'segments' in context:
        segments_str = f"| segments:{context.get('segments')}"
    else:
        segments_str = ""

    print(
        f"number:{number_str:<5} | "
        f"time:{time:<17} | "
        f"{direction:<8} | "
        f"proto:{protocol:<5} | "
        f"{source_str:<21} -> {dest_str:<21} | "
        f"length:{length_str:<4} | "
        f"{details}"
        f"{segments_str}"
    )
    osc_client = context.get("osc_client")
    if osc_client:
        try:
            # OSCアドレス（ラベル）を決定
            # (例: "/packet/dns"など)
            osc_address = f"/packet/{protocol.lower().replace(' ', '_')}"

            # 送信するデータをリストにまとめる
            data_to_send = [
                protocol.lower().replace(" ", "_"),  # 1. プロトコル名 (String)
                int(context.get("length", 0)),  # 2. パケット長 (Int)
                details,  # 3. 詳細 (String)
                int(context.get("packet_number", None)),  # 4. パケットナンバー(Int)
                context.get("source_ip", "?"),  # 5. 送信元IP (String)
                context.get("dest_ip", "?"),  # 6. 宛先IP (String)
            ]

            # OSCメッセージを送信
            osc_client.send_message(osc_address, data_to_send)

        except Exception as e:
            print(f"[!] OSC send error: {e}")


def no_higher_layer(layers,layer_name):
    """
    担当するレイヤーじゃないレイヤーが来たときに呼び出される

    Args:
        layers (_type_): _description_
        layer_name (_type_): _description_
    """

    if not len(layers) == 0 and has_nested_attr(layers[0],"layer_name"):
        print(f"higher layer is {get_nested_attr(layers[0],"layer_name")}")
    else:
        print(f"This is not {layer_name} data")
    return
