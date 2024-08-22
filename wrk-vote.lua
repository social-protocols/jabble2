-- for the http benchmark tool wrk
-- use just prod-benchmark to benchmark
wrk.scheme  = "https"
wrk.host    = "jabble.fly.dev"
wrk.method  = "POST"
wrk.path    = "/RpcApi/vote"
wrk.body    = '[6,6,"Down"]'
wrk.headers = {
  ["Authorization"] =
  "Bearer " .. os.getenv("WRK_BEARER"),

}
