# pipeline/transport_layer/application_layer/dns_handler.py
from utils import format_output, get_nested_attr , no_higher_layer
# TODO パケットnumber 23,30,35を確認

def process(packet, layers, context):
    """
    レイヤー7 (DNS) の処理。
    """
    if not layers or layers[0].layer_name != "dns":
        no_higher_layer(layers, "dns")
        return

    dns_layer = layers.pop(0)
    query_name = get_nested_attr(dns_layer, "qry_name")
    transaction_id = get_nested_attr(dns_layer, "id")
    # 複数個レコードがあってもとりあえず一個だけしか返さない
    if dns_layer.flags_response == "True":
        record = dns_layer.resp_type
        if record == "1":
            # Aレコード
            answers = get_nested_attr(dns_layer, "a")
        elif record == "5":
            # CNAMEレコード
            answers = get_nested_attr(dns_layer, "a")
        elif record == "28":
            # AAAAレコード
            answers = get_nested_attr(dns_layer, "aaaa")
        elif record == "12":
            # PTRレコード=逆引き
            answers = get_nested_attr(dns_layer, "ptr_domain_name")
        elif record == "6":
            # TODO res_amountの数字おかしいので確認
            answers = " MNAME: "+get_nested_attr(dns_layer, "soa_mname")+"RNAME: "+get_nested_attr(dns_layer, "soa_rname_name")
        else:
            answers = "unknown record"
        details = f"Query: {query_name}, Answer: {answers}, id: {transaction_id}, res_amount: {int(get_nested_attr(dns_layer,"count_answers"))}"
    else:
        details = f"Query: {query_name}, id: {transaction_id}"

    format_output(context, "DNS", details)
