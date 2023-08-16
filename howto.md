# mikrofrontender på min side

Mikrofrontends gir teamene mulighet til å presentere den viktigste informasjonen til brukerne sine på Min side, som ett lite butikkvindu inn til teamenes egne løsninger. Dette gjøres gjennom at mikrofrontenden blir bygget til ESM og 
deretter hentes inn som en remote ES-Modul.

## Nice! Hvordan kan mitt team få en mikrofrontend på min side?

1. Lag en mikrofrontend! Om du vil bruke ett template finnes
   det [ett for typescript](https://github.com/navikt/tms-mikrofrontend-template-vitets) og ett for
   [ett for javascrip](https://github.com/navikt/tms-mikrofrontend-template-vitejs)
2. Sett opp automatisk oppdatering av manifest-url i workflows-mappa til prosjektet, se eksempel i [worfklows-mappa til tms-mikrofrontend-selector](https://github.com/navikt/tms-mikrofrontend-selector/tree/main/.github/workflows/manifest-triggere)

2. Opprett ett issue i [tms-min-side repoet](https://github.com/navikt/tms-min-side), be om å få lagt inn
   mikrofrontenden i kode og avtal en `<microfrontendId>`
3. Koble på [min-side-microfrontend-topicet](https://github.com/navikt/min-side-microfrontend-topic-iac)



## Enable/disable 

Om en mikrofrontend vises avhenger av om den er enablet for en gitt bruker. Dette setter du ved å sende en melding på
microfrontend-topicet.

1. Enable-melding når en bruker skal se mikrofrontenden
   ```json
   {
      "@action": "enable",
      "ident": <ident for bruker, vanligvis fnr>,
      "microfrontend_id": <microfrontendId>,
      "sensitivitet" : <nivå som kreves for å se innholdet i mikrofrontenden, gyldige verdier: substantial og high>,
      "@initiated_by":<ditt-team>
   }
   ```
2. Disable-melding når bruker ikke skal se mikrofrontenden lenger
   ```json
   {
      "@action":  "disable",
      "ident": <ident for bruker, vanligvis fnr>,
      "microfrontend_id": <microfrontendId>,
      "@initiated_by":<ditt-team>
   }
   ```

### Finnes det ett meldingsbygger bibliotek? 
Ofc gjør det det! Det er tilgjengelig på [jitpack](https://jitpack.io/#navikt/tms-mikrofrontend-selector) og [github packages](https://github.com/navikt/tms-mikrofrontend-selector/packages/1875650)


## Språk
Vi bruker språkvelgeren i Dekoratøren. For å vite hvilket språk man skal vise så sender vi et event via session storage.
Man kan enkelt sette opp en provider som vi har gjort [her](https://github.com/navikt/tms-utkast-mikrofrontend/blob/main/src/provider/LanguageProvider.tsx).

## Shared dependencies
For å unngå å ha for store bundles så deler vi noen dependencies på tvers av apper. Disse hentes ned en gang ved første
page load fra en CDN og caches i browseren. Foreløpig ligger react, react-dom og ds-css i CDNen.

CSSen fra designsystemet hentes i skallet til Min side. Denne skal deles på tvers og ikke bundles med i mikrofrontenden. 
Vi bør ligge på samme major versjon, veilendende versjon ligger [her](https://github.com/navikt/tms-min-side/blob/main/index.html).

## FAQ

### Hva bør jeg velge som mikrofrontend-id?
`<område>.<tjenste>` er ett bra utgangspunkt, prøv å holde det så generelt som mulig, men gjenkjenbart.

### Hvordan skal mikrofrontenden se ut?
Vi bruker en modifisert versjon av designsystemet. Ta kontakt med oss for å se hvordan en ny mikrofrontend kan passe inn
i Min side.

### Hvordan fungerer amplitude?

Amplitude fungerer som vanlig (se [AAP sin mikrofrontend](https://github.com/navikt/aap-min-side-microfrontend/blob/main/src/utils/amplitude.ts)). 
Dere kan fritt logge de eventene dere vil, men for å sette opp målinger på tvers i Amplitude har vi disse føringene 
på [navigasjonseventer](https://github.com/navikt/analytics-taxonomy/tree/main/events/navigere):
- komponent: tekstlig representasjon av komponenten det ble trykket på.

Videre anbefaler vi å følge [taksonominen](https://github.com/navikt/analytics-taxonomy) i NAV.

### Hva er egentlig sensitivitet?
Feltet `sensitivitet` i enablemeldingen  korresponderer direkte til de [nye acr-veridene](https://docs.digdir.no/docs/idporten/oidc/oidc_protocol_id_token#acr-values) i IDporten token. 
Altså; hvis det ligger informasjon i mikrofrontenden som kun personer som har logget inn med `idporten-loa-high` skal kunne se, skal `sensitivitet` settes til `HIGH` Om informasjonen
kan vises uavhengig av innloggingsnivå skal sensitivitet være `substantial`.
Om sensitivitet ikke er spesifisert i kafka-meldingen settes det alltid til `high` hos oss.
Om en person logger inn med `idporten-loa-substantial` og det finnes mikrofrontender som personen kan se på `idporten-loa-high` vil bruker få beskjed om dette og link til en "steup"
login. Se også [NAIS docs](https://docs.nais.io/security/auth/idporten/#security-levels) for mer info om acr-verdiene




