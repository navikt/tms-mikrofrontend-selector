# Mikrofrontender på min side

Mikrofrontends gir teamene mulighet til å presentere den viktigste informasjonen til brukerne sine på Min side, som et
lite butikkvindu inn til teamenes egne løsninger. Dette gjøres gjennom at mikrofrontenden blir bygget til ESM og
deretter hentes inn som en remote ES-Modul.

## Nice! Hvordan kan mitt team få en mikrofrontend på min side?

### 1. Lag en mikrofrontend!
   Om du vil bruke et template finnes det [ett for typescript](https://github.com/navikt/tms-mikrofrontend-template-vitets) og 
   [ett for javascript](https://github.com/navikt/tms-mikrofrontend-template-vitejs)

### 2. Sett opp oppdatering av url til den gjeldene javascript-koden
   1. Be om å få installert min-side-repo-authenticator
   2. Legg inn action-secrets PRIVATE_KEY og APP_ID (disse får du fra min-side teamet)
   3. Legg inn oppdateringsworkflow i github workflows mappa til prosjektet
```yaml
name: Update microfrontend-manifest
on: <ønsket hook>

jobs:
  trigger_manifest_update:
    uses: navikt/tms-deploy/.github/workflows/oppdater-mikrofrontend-manifest.yaml@main
    with:
      cluster: "<cluster: dev-gcp eller prod-gcp>"
      id: "<microfrontend_id>"
      url: "<url til js-kode i cdn>"
    secrets: inherit
 ```

#### Hvordan vet jeg hva urlen til javascriptet er?
Om du bruker `navikt/frontend/actions/cdn-upload/v1@main`:

1. Hent path fra cdn-upload outputs(`${{ <steps/jobs>.<id til cdn-upload step eller job>.outputs.uploaded }}`)
2. Legg til host (`https://cdn.nav.no`)
```bash
files="${{ <steps/jobs>.<id til cdn-upload step eller job>.outputs.uploaded }}"
cdn_path=$(echo $files|cut -d "," -f <index til js-url i liste>)
cdn_url="https://cdn.nav.no/$cdn_path"
```

Eksempel finnes i
[workflows-mappa til tms-mikrofrontend-selector](https://github.com/navikt/tms-mikrofrontend-selector/tree/main/.github/workflows/manifest-triggere)
NB: Urlen skal være til **js-koden**, ikke json-manifest.

### 3. Koble til løsningen
1. Opprett et issue i [tms-min-side repoet](https://github.com/navikt/tms-min-side), be om å få lagt inn
  mikrofrontenden i kode og avtal en `<microfrontendId>`
2. Koble på [min-side-microfrontend-topicet](https://github.com/navikt/min-side-microfrontend-topic-iac)



  ## Enable/disable 

  Om en mikrofrontend vises avhenger av om den er enablet for en gitt bruker. Dette setter du ved å sende en melding på
  microfrontend-topicet.

  ### Enable-melding når en bruker skal se mikrofrontenden
```json
{
    "@action": "enable",
    "ident": <ident for bruker: fnr/dnr>,
    "microfrontend_id": <microfrontendId>,
    "sensitivitet": <nivå som kreves for å se innholdet i mikrofrontenden, gyldige verdier: substantial og high>,
    "@initiated_by": <ditt-team>
}
```
 ### Disable-melding når bruker ikke skal se mikrofrontenden lenger

```json
{
    "@action":  "disable",
    "ident": <ident for bruker: fnr/dnr>,
    "microfrontend_id": <microfrontendId>,
    "@initiated_by":<ditt-team>
}
```

### Meldingsbygger-bibliotek

Det er tilgjengelig på [github packages](https://github.com/navikt/tms-mikrofrontend-selector/packages/1875650)

## Språk

Vi bruker språkvelgeren i Dekoratøren. For å vite hvilket språk man skal vise så sender vi et event via session storage.
Man kan enkelt sette opp en provider som vi har
gjort [her](https://github.com/navikt/tms-utkast-mikrofrontend/blob/main/src/provider/LanguageProvider.tsx).

## Shared dependencies

For å unngå å ha for store bundles så deler vi noen dependencies på tvers av apper. Disse hentes ned en gang ved første
page load fra en CDN og caches i browseren. Foreløpig ligger react, react-dom og ds-css i CDNen.

CSSen fra designsystemet hentes i skallet til Min side. Denne skal deles på tvers og ikke bundles med i mikrofrontenden.
Vi bør ligge på samme major versjon, veilendende versjon
ligger [her](https://github.com/navikt/tms-min-side/blob/main/index.html).

## FAQ

### Hva bør jeg velge som mikrofrontend-id?

`<område>.<tjenste>` er et bra utgangspunkt, prøv å holde det så generelt som mulig, men gjenkjenbart.

### Hvordan skal mikrofrontenden se ut?

Vi bruker en modifisert versjon av designsystemet. Ta kontakt med oss for å se hvordan en ny mikrofrontend kan passe inn
i Min side.

### Hvordan fungerer amplitude?

Amplitude fungerer som vanlig (
se [AAP sin mikrofrontend](https://github.com/navikt/aap-min-side-microfrontend/blob/main/src/utils/amplitude.ts)).
Dere kan fritt logge de eventene dere vil, men for å sette opp målinger på tvers i Amplitude har vi disse føringene
på [navigasjonseventer](https://github.com/navikt/analytics-taxonomy/tree/main/events/navigere):

- komponent: tekstlig representasjon av komponenten det ble trykket på.

Videre anbefaler vi å følge [taksonominen](https://github.com/navikt/analytics-taxonomy) i NAV.

### Hva er sensitivitet?

Feltet `sensitivitet` i enablemeldingen korresponderer direkte til
de [nye acr-veridene](https://docs.digdir.no/docs/idporten/oidc/oidc_protocol_id_token#acr-values) i IDporten token.

* Om informasjonen som vises krever `idporten-loa-high` innlogging skal `sensitivitet` settes til `high`. 
* Om informasjonen kan vises uavhengig av innloggingsnivå skal `sensitivitet` settes til `substantial`.

Om sensitivitet ikke er spesifisert i kafka-meldingen settes det alltid til `high` hos oss.
Om en person logger inn med `idporten-loa-substantial` og det finnes mikrofrontender som personen kan se
på `idporten-loa-high` vil bruker få beskjed om dette og link til en "stepup"
login. Se også [NAIS docs](https://docs.nais.io/security/auth/idporten/#security-levels) for mer info om acr-verdiene


## Observability
Hendelser blir logget til kibana med customfelter for filtrering \
Alle microfrontends: `x_contenttype:"microfrontend` \
Spesifik microfrontend: `x_minside_id :"<microfrontendid>"` 