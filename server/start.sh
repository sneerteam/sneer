cd /root/sneer-live

# Append date to previous log filename
mv logs/log "logs/log-`date +%Y-%m-%d_%H-%M`"

echo START NEW SERVER PROCESS
SERVER_PORT=5555
java -jar ./sneer.server-0.1.0-SNAPSHOT-standalone.jar $SERVER_PORT  &> logs/log&
