# plataform description
plataform = "python"

timeout = 5


'''
    REQUEST (client sends to server)

    READ - {type: 'read', request_code: int}
    GET_ECHOES - {type: 'get_echoes', request_code: int, variable: dict, timestamp: int}
    READ TIMESTAMP - {type: 'read_timestamp', request_code: int}
    WRITE - {type: 'write', request_code: int, client_id: int, variable: dict, timestamp: int, echoes: array(server_id, data_signature)}
    CLOSE SOCKET - {type: 'bye'}
'''
type = 'type'
read = 'read'
read_timestamp = 'read_timestamp'
write = 'write'
get_echoe = "get_echoe"
variable = 'variable'
timestamp = 'timestamp'
data_signature = 'data_signature'
echoes = 'echoes'
client_id = 'client_id'
bye = 'bye'
request_code = 'request_code'


'''
    RESPONSE (server sends to client)
    BASIC STRUCTURE - {server_id: int, plataform: string, request_code: int, status: string, msg: string, data: dictionary or array}
'''
server_id = 'server_id'
server_plataform = 'plataform'
status = 'status'
success = 'success'
error = 'error'
msg = 'msg'
data = 'data'
variable_updated = 'variable_updated'
#errors
undefined_type = 'undefined_type'
unknown_error = 'unknown_error'
outdated_timestamp = 'outdated_timestamp'
invalid_signature = 'invalid_signature'
timestamp_already_echoed = 'timestamp_already_echoed'
invalid_echoes = 'invalid_echoes'

'''
    DIGITAL SIGN
    OBS: The signature is in BASE64 format (to reduce the size)
    It is signed: Variable+str(timestamp) -> data_signature
'''