wrk.method  = "POST"
wrk.path    = "/RpcApi/vote"
wrk.body    = '[1,1,"Up"]'
wrk.headers = {
  ["Authorization"] =
  "Bearer " .. os.getenv("WRK_BEARER"),
}
