# AVO

Automatisch Vertragings-Orakel (AVO) is een demonstratiesysteem voor testautomatisering. Het systeem bestaat uit twee deelsystemen: AVOServer en AVOClient. AVOServer is het hoofdsysteem met daarin de meeste functionaliteit. AVOClient is simpelweg een grafische client die de output van de server kan ontvangen. AVO is belast met het bepalen van de vertraging van treinen die op bepaalde dienstregelpunten worden waargenomen. Wanneer een trein wordt waargenomen dan wordt er onmiddelijk en instantaan een bericht verstuurd naar AVOServer. Vervolgens bepaalt AVOServer de vertraging van de trein en publiceert dit bericht weer op uitgaande queues. Dit backend-systeem werkt volledig met JMS-verkeer.

AVO is een demonstratiesysteem, er wordt voor het gemak gebruik gemaakt van een Apache ActiveMQ setup die lokaal op je machine draait. Ook wordt er gebruik gemaakt van JMeter om handmatig berichten in te schieten (d.w.z. de waargenomen treinen simuleren). Voor beide systemen is een installer bijgevoegd in de map `installers`. Installeer deze software indien je hier nog niet over beschikt op je systeem.

## AVOServer

AVOServer is een supersnelle server die van waargenomen treinen de vertraging bepaalt en doorstuurt naar afnemers. Deze afnemers bestaan uit de AVOClient en eventueel andere afnemende systemen. AVOServer luistert op het volgende topic op je lokale ActiveMQ instantie:

* AVOSignaleringIn

De bepaalde vertragingen worden verstuurd naar de volgende queues op je lokale ActiveMQ instantie:

* AVOClientUit
* AVOServerUit

De signaleringsberichten die worden ontvangen door AVO zijn relatief eenvoudig:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Signalering>
  <Trein>4084</Trein>
  <Drglpunt>Asd</Drglpunt>
</Signalering>
```

De uitgestuurde vertragingen zijn ook relatief eenvoudig en bevatten de volgende elementen:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Vertraging>
  <Trein>4084</Trein>
  <Drglpunt>Asd</Drglpunt>
  <Vertraagd>+23</Vertraagd>
  <Tijd>2018/03/23 16:47:01</Tijd>
</Vertraging>
```

### Starten en testen

AVOServer is als jar te vinden in de map `bin`. Mocht je het project opnieuw willen compileren, het is ook bijgevoegd als IntelliJ project in de map `AVOServer`. In deze readme gaan we ervan uit dat je gewoon gebruik maakt van de geleverder jar.

Allereerst moeten we ActiveMQ starten. Ga hiervoor naar de installatiedirectory van ActiveMQ en start een lokale instantie met `activemq start`. Het grote voordeel is dat AVOServer dynamisch de benodige topics en queues aanmaakt bij de ActiveMQ broker. Om de topics en queues te controleren kun je gebruik maken van de webconsole. Ga hiervoor naar `http://localhost:8161/admin` en log in met credentials admin/admin.

Nadat ActiveMQ is gestart kun je AVOServer starten: `java -jar AVOServer.jar`. In de console-output van AVOServer zie je de ontvangen berichten voorbij komen. Je kunt natuurlijk ook in de ActiveMQ webconsole kijken!

Om het systeem in actie te zien kun je gebruik maken van het meegeleverde jmeter testplan `AVOServerTest.jmx`. Dit testplan bevat een threadgroup om berichten aan te bieden en een threadgroup om de output van AVOServer op te vangen. Probeer het eens uit!

## AVOClient

TODO: In ontwikkeling
