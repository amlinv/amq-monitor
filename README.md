# amq-monitor
Live monitor of ActiveMQ Key Metrics


## URL Syntax

|| Type || Syntax ||
| *Full JMX URL* | service:<jmx-url> |
| *Jolokia* | jolokia:<url> |
| *JMX by pid* | pid=<pid> or jvmId=<pid> |
| *JMX by host + port* | <host>:<port> |


## Examples

    jolokia://admin:admin@localhost:8161/api/jolokia
    service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi
    jvmId=1514
    pid=1514
    localhost:1099

