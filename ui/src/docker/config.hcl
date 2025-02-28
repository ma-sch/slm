consul {
  address = "consul:8500"
}

template {
  source =        "/consul-template/slm.ctmpl"
  destination =   "/etc/nginx/conf.d/slm.conf"
  exec {
    command = ["nginx", "-s", "reload"]
  }
}