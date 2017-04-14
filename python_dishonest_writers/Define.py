# plataform description
plataform = "python"

timeout = 5


'''
    REQUEST (client sends to server)

    READ - {type: 'read', request_code: int}
    READ TIMESTAMP - {type: 'read_timestamp', request_code: int}
    WRITE - {type: 'write', request_code: int, client_id: int, variable: dict, timestamp: int, data_signature: string}
    CLOSE SOCKET - {type: 'bye'}
'''
type = 'type'
read = 'read'
read_timestamp = 'read_timestamp'
write = 'write'
variable = 'variable'
timestamp = 'timestamp'
data_signature = 'data_signature'
client_id = 'client_id'
bye = 'bye'
request_code = 'request_code'


'''
    RESPONSE (server sends to client)
    BASIC STRUCTURE - {server_id: int, plataform: string, request_code: int, status: string, msg = string, data = dictionary or array}
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

'''
    DIGITAL SIGN
    OBS: The signature is in BASE64 format (to reduce the size)
    It is signed: Varaible+str(timestamp) -> data_signature
'''