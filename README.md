# Ktor-PTV6-Proxy

Reimplementierung des [Tiger-PTV6-Proxy](https://github.com/gematik/tiger-ptv6-proxy) der [gematik GmbH](https://www.gematik.de/)
auf der Basis von Kotlin, Ktor und Jdom.

Das Ziel einer PTV6-Konnektor-Simulation wird erreicht, indem das XML der folgenden Operationen angepasst wird:
- CheckCertificateExpiration
- EncryptDocument
- ExternalAuthenticate
- ReadCardCertificate
- SignDocument

Einfach ausgedrückt, wird sichergestellt, dass diese Operationen die ECC-Verschlüsselung auswählen, selbst wenn im XML
zuerst die Verwendung von RSA konfiguriert war. Für ein genaues Verständnis schauen Sie sich bitte die Testfälle und
gegebenenfalls die Implementation Methoden-Modifikation an.

**Der Ktor-PTV6-Proxy funktioniert nur bei Verwendung von Karten der Generation 2.1 !!**

## Vergleich mit dem gematik tiger-ptv6-proxy
|                   Thema                   | tiger-ptv6-proxy                              | ktor-ptv6-proxy                                                                                                                                                |
|:-----------------------------------------:|:----------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------|
|                  Support                  | von der gematik                               | keiner. Die Lizenz des Projektes erlaubt es aber, dass Sie es forken und selbst anpassen können. MergeRequests an dieses Projekt werden vielleicht integriert. |
|                   Tests                   | Integrationstests zum Konnektor               | Unit- und Applicationstests gegen die Spezifikation (, aber nicht gegen einen echten Konnektor getestet)                                                       |
|        Wie wird das XML angepasst         | durch reguläre Ausdrücke                      | durch die XML-Lib [JDOM](http://www.jdom.org/)                                                                                                                 |
| Namespace-Version neu eingefügter Elemente | ist hardgekodet, was zu Problemen führen kann | wird von anderen Elementen aus dem XML übernommen und ist damit mit dem Rest des XML konsistent                                                                |

Die letzten beiden Punkte sind der Grund, warum dieses Projekt existiert.

## Deployment

### Konfiguration/Umgebungsvariablen

- PTV5PLUS_CONNECTOR_URL: muss gesetzt werden, die Url des PTV5+ Konnektors, der sich wie ein PTV6 Konnektors verhalten soll.
- PROXY_URL: optionale Url eines Proxies, über den die Verbindung laufen soll.
- TIMEOUT_IN_SEC: Wie lange die Anfrage zum Konnektor Zeit hat, bevor sie durch einen Timeout beendet wird. Defaultwert: 10(s) 
- HOST: Defaultwert: "0.0.0.0"
- PORT: Defaultwert: 8080

### Docker-Image

Mit dem Befehl `docker build -t ktor-ptv6-proxy:latest .` bauen Sie ein Docker-Image aus diesem Projekt.
Genaue Details können Sie sich in der Datei Dockerfile ansehen.