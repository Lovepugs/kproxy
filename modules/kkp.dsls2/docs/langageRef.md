#Language reference



##Backend
Backend(host = dnsNameOrIP, port = 80)

can be writen

 	- Backend(dnsNameOrIP) // port defaults to 80 
	- Backend(dnsNameOrIP:9000)

##Acl
Acl("localhost", 168.192.24.1, 168.192.24.0/24) 

Accept a list of allowed IPs or CDIRs and localhost loopback