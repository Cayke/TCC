# plataform description
plataform = "python"
ip = 'localhost'
port = 5000

timeout = 5
#   request
#   {client_id: int, request_code: int, type: 'read'} or
#   {client_id: int, request_code: int, type: 'write', variable: number, timestamp: number, data_sign: signature}
#   {client_id: int, request_code: int, type: 'bye'}

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

#   response
#   {request_code: int, serverID: 'string', status: 'string', msg = 'string', data = dictionary or array}
serverID = 'serverID'
status = 'status'
success = 'success'
error = 'error'
msg = 'msg'
undefined_type = 'undefined_type'
unknown_error = 'unknown_error'
variable_updated = 'variable_updated'
outdated_timestamp = 'outdated_timestamp'
invalid_signature = 'invalid_signature'
data = 'data'




#   DIGITAL SIGN
#   OBS: AS ASSINATURAS SAO EM BASE64 (para diminuir o tamanho)
#   É assinado o Value+str(timestamp) -> data_sign
#   É assinado tambem apenas o timestamp -> time_sign