# Üldine
- Ehitustarkvara: [Gradle 2.9](http://gradle.org/gradle-download/) 
- Java versioon: [Java SE Development Kit 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- X-tee: [infosüsteemide andmevahetuskiht X-tee](https://www.ria.ee/ee/x-tee.html)
- X-tee v6: [üleminek X-tee versioonile 6](https://www.ria.ee/ee/uleminek-x-tee-versioonile-6.html)

X-tee WSDL konverteri ülesanne on transformeerida WSDL, mis vastab X-tee sõnumiprotokollile v2 või v3, uude v4 formaati ([v4 spec](https://www.ria.ee/public/x_tee/pr-mess_x-road_message_protocol_v4.0_4.0.8_Y-743-11.pdf)).

### Olulisemad konverteri tegevused
- Võtab kasutusele uued X-tee ja teenusepakkuja nimeruumid
- Lisab uued X-tee SOAP päiseelemendid
- Asendab rpc/endoded stiilis loodava SOAP sõnumi doc/literal stiiliga  
- Asendab _SOAP-ENC:Array_ stiilis schema elemendi _sequence_ elemendiga 

### Eeldused konverteri edukaks kasutamiseks
- Konverditav WSDL on korrektne v1.1 dokument ([v1.1 schema](http://schemas.xmlsoap.org/wsdl/))  
- X-tee v2 WSDL kasutab rpc/encoding stiili
- X-tee v3 WSDL kasutab doc/literal stiili
- Teenusepakkuja kõik schema objektid asuvad WSDL dokumendis importimata kujul. Imporditud võivad olla ainult X-tee ja SOAP encoding schemad
- Multipart/Related (MIME) teenuse esimene osa peab olema SOAP ümbrik. Täiendavalt peab olema vähemalt üks manuse osa

# Konverteri funktsioonid
Konverterit ning selle lisafunktsioone ajendas mind kirjutama X-tee sõnumiprotokolli uuendamise paratamatus. 
Kui sul on palju ürgvanu v2 rpc/encoded teenuseid, siis nüüd on käes aeg veebiteenuste kaasajastamiseks.

Veebiteenuste uuendamise töövoog võiks olla näiteks selline,

- Konverdi vanad v2 ja v3 WSDL-id v4-ks
- Ekstrakti v4 failist üks teenus ja implementeeri see. Ekstraktitud WSDL salvesta eraldi kataloogi (per-adapter)
- Kui oled ühe adapteri jagu teenuseid ekstraktinud ja implementeerinud, genereeri üksikutest WSDL failidest koond-WSDL. 
WSDL-i saab X-tee administraator importida turvaserverisse

Konverteri erinevate funktsioonide proovimiseks võib kasutada reaalseid emta (v2, 70 teenust) ja emtav5 (v3, 15 teenust) andmekogude WSDL-e.

**Konvertimine**. Konverdi wsdl/emtav3.wsdl v4 formaati. Kasuta gradle taski *convertV3ToV4*

```bash
gradlew convertV3ToV4
```

Tulemus salvestatakse faili wsdl/xrd-v4.wsdl

**Ekstraktimine**. Oletame, et soovid implementeerida kahte teenust nimedega _uploadMime_ ja _downloadMime_. 
Mõlemad teenused võiksid saada eraldatud ühte WSDL faili. Ekstrakti need teenused failist wsdl/xrd-v4.wsdl.
Kasuta gradle taski *extractServices*

```bash
gradlew extractServices
```

Tulemus salvestatakse faili wsdl/services.wsdl

**Liitmine**. Peale _uploadMime_ ja _downloadMime_ teenuse implementeerimist, liida need wsdl/emtav4.wsdl failis olevate teenustega. 
Kasuta gradle taski *assembleServices*
                  
```bash
gradlew assembleServices
```

Tulemus salvestatakse faili wsdl/emta-v6.wsdl. Nüüd on turvaserverisse importimiseks vajalik WSDL olemas. 
Peale esmast importi võib sama adapteri uusi teenuseid turvaserverisse lisada ükshaaval (koond-WSDL'i läheb vaja ainult esmasel importimisel).

# Näited
Kui kasutada wrapper skripte, gradlew (Linux) või gradlew.bat (Windows), siis ei pea gradlet paigaldama. Wrapper teeb seda sinu eest.
JDK8 on vajalik gradle taskide käivitamiseks.

### v2 -> v4 konvertimine
Häälestamiseks on järgmised parameetrid,

1. wsdlOutputDir - kataloog kuhu salvestatakse v4 konverditud WSDL
2. wsdlFileIn - v2 formaadis WSDL faili aadress. Failisüsteem: '/tmp/v2.wsdl', klassitee: 'classpath://v2.wsdl', http(s): 'http://host:port/v2.wsdl'
3. useWrapperElements - true|false, 'true' korral lisatakse 'keha/paring' wrapper elemendid
4. producerName - teenusepakkuja nimi. Kasutatakse nimeruumi koostamisel, näiteks emta-v6: 'http://emta-v6.x-road.eu'

Järgnev näide loeb v2 dokumendi aadressilt http://vinski.mta:9001/xtee-router/emta?WSDL ja salvestab konvertimise tulemuse
failisüsteemi /tmp/xrd-v4.wsdl. 'keha/paring' wrapper elemente ei kasutata. Teenusepakkuja nimeruum 'http://emta-v6.x-road.eu'

```java
task convertV2ToV4(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    main = "ee.rmit.xrd.XrdV2ToV4Converter"
    def wsdlOutputDir = "/tmp"
    def wsdlFileIn = "http://vinski.mta:9001/xtee-router/emta?WSDL"
    def useWrapperElements = "false"
    def producerName = "emta-v6"
    args = [wsdlOutputDir, wsdlFileIn, useWrapperElements, producerName].toList()
}
```

### v3 -> v4 konvertimine
Häälestamiseks on järgmised parameetrid,

1. wsdlOutputDir - kataloog kuhu salvestatakse v4 konverditud WSDL
2. wsdlFileIn - v3 formaadis WSDL faili aadress. Failisüsteem: '/tmp/v3.wsdl', klassitee: 'classpath://v3.wsdl', http(s): 'http://host:port/v3.wsdl'
3. producerName - teenusepakkuja nimi. Kasutatakse nimeruumi koostamisel, näiteks emta-v6: 'http://emta-v6.x-road.eu'

Järgnev näide loeb v3 dokumendi aadressilt /tmp/wsdl/emtav3.wsdl ja salvestab konvertimise tulemuse
samasse kohta failisüsteemis /tmp/wsdl/xrd-v4.wsdl. Teenusepakkuja nimeruum 'http://emta-v6.x-road.eu'

```java
task convertV3ToV4(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    main = "ee.rmit.xrd.XrdV3ToV4Converter"
    def wsdlOutputDir = "/tmp/wsdl"
    def wsdlFileIn = "$wsdlOutputDir/emtav3.wsdl"
    def producerName = "emta-v6"
    args = [wsdlOutputDir, wsdlFileIn, producerName].toList()
}
```

### Teenuste ekstraktimine
Häälestamiseks on järgmised parameetrid,

1. wsdlOutputDir - kataloog kuhu salvestatakse ekstraktitud teenuste WSDL-id
2. wsdlFileIn - koond-WSDL faili aadress. Failisüsteem: '/tmp/v4.wsdl', klassitee: 'classpath://v4.wsdl', http(s): 'http://host:port/v4.wsdl'
3. services - teenuste nimed mida soovitakse eraldada. Kõik teenused eraldi failidesse: services='', valitud teenused eraldi failidesse: services='uploadMime, downloadMime',
valitud teenused ühte faili: services='uploadMime\*, downloadMime\*'

Järgnev näide loeb v4 dokumendi aadressilt /tmp/wsdl/xrd-v4.wsdl ja ekstraktib sellest kaks teenust _uploadMime_ ja _downloadMime_
eraldi failidesse /tmp/wsdl/uploadMime.wsdl ja /tmp/wsdl/downloadMime.wsdl 

```java
task extractServices(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    main = "ee.rmit.xrd.WsdlServiceExtractor"
    def wsdlOutputDir = "/tmp/wsdl"
    def wsdlFileIn = "$wsdlOutputDir/xrd-v4.wsdl"
    def services = "uploadMime, downloadMime"
    args = [wsdlOutputDir, wsdlFileIn, services].toList()
}
```

### Teenuste liitmine
Häälestamiseks on järgmised parameetrid,

1. wsdlInputDir - kataloog kus asuvad liidetavate teenuste WSDL-id
2. wsdlTemplate - baas-WSDL, millele liidetakse kõik teenused. Vaikimisi template (teenusteta) kasutamiseks wsdlTemplate=''. 
Templateks võib olla ka teenustega WSDL. Failisüsteem: '/tmp/v4-template.wsdl', klassitee: 'classpath://v4-template.wsdl', http(s): 'http://host:port/v4-template.wsdl'  
3. wsdlFilesToAssemble - saab vajadusel täpsustada konkreetsete liidetavate failide nimed. Kõik failid kataloogis: wsdlFilesToAssemble='', 
ainult üks fail: wsdlFilesToAssemble='services.wsdl', kaks faili: wsdlFilesToAssemble='{service1,service2}.wsdl'
4. producerName - teenusepakkuja nimi. Kasutatakse nimeruumi koostamisel, näiteks emta-v6: 'http://emta-v6.x-road.eu'

Järgnev näide loeb v4 koond-WSDL'i aadressilt http://vinski.mta:9001/xtee-router/emtav4?WSDL ja liidab sellele uued teenused failist 
/tmp/wsdl/services.wsdl. Teenusepakkuja nimeruum 'http://emta-v6.x-road.eu'

```java
task assembleServices(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    main = "ee.rmit.xrd.XrdV4WsdlAssembler"
    def wsdlInputDir = "/tmp/wsdl"
    def wsdlTemplate = "http://vinski.mta:9001/xtee-router/emtav4?WSDL"
    def wsdlFilesToAssemble = "services.wsdl"
    def producerName = "emta-v6"
    args = [wsdlInputDir, wsdlTemplate, wsdlFilesToAssemble, producerName].toList()
}
```

Järgnev näide kasutab vaikimisi template faili ja liidab sellele kõik failid kataloogis /tmp/wsdl. 
Teenusepakkuja nimeruum 'http://emta-v6.x-road.eu'

```java
task assembleServices(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    main = "ee.rmit.xrd.XrdV4WsdlAssembler"
    def wsdlInputDir = "/tmp/wsdl"
    def wsdlTemplate = ""
    def wsdlFilesToAssemble = ""
    def producerName = "emta-v6"
    args = [wsdlInputDir, wsdlTemplate, wsdlFilesToAssemble, producerName].toList()
}
```