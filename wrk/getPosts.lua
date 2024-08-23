wrk.method  = "POST"
wrk.path    = "/RpcApi/getPosts"
wrk.body    = "null"
wrk.headers = {
  ["Authorization"] =
  "Bearer " .. os.getenv("WRK_BEARER"),
}

