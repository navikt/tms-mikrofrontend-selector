# Mikrofrontends på Min side

## Kom i gang

Ta utgangspunkt i [template for ssr mikrofrontend](https://github.com/navikt/tms-microfrontend-template-ssr) og opprett et nytt repository basert på denne templaten.

### Konfigurer applikasjonen


1. **Bytt ut applikasjonsnavn i konfigurasjonsfiler**
   CMD + Shift + F og søk etter tms-microfrontend-template-ssr og erstatt dette med ditt applikasjonsnavn.
   
1. **Tilpass nais.yaml**
   Tilpass følgende felter i både nais/dev-gcp/nais.yaml og nais/prod-gcp/nais.yaml

```json
   metadata:
      name: <ditt applikasjonsnavn>
      namespace: <ditt teams namespace>
      labels:
         team: <ditt teams namespace>
```

1. **Tilpass innholdet i .github/workflows/deploy.yaml**

   Tilpass følgende steg i deploy.yaml. Applikasjonsnavn skal være likt navnet på repository. 

   **Deploy to CDN**

```json
      - name: "Upload to cdn"
      uses: nais/deploy/actions/cdn-upload/v2@master
      with:
       team: <ditt teams namespace>
       source: ./dist/client/_astro
       destination: <ditt applikasjonsnavn>
```

   **Build and push**

```json
   - name: "Build and push"
   uses: nais/docker-build-push@v0
   id: docker-build-push
   with:
    team: <ditt applikasjonsnavn>
```

   **Update manifest**

   Tilpass både update-manifest-dev og update-manifest-prod.

```json
   update-manifest-dev:
      uses: navikt/tms-deploy/.github/workflows/oppdater-mikrofrontend-manifest-v3.yaml@main
      needs: build
      with:
         id: <ditt applikasjonsnavn>
         url: <http://<ditt-applikasjonsnavn> // eksempelvis "http://tms-microfrontend-template-ssr"
         appname: <ditt applikasjonsnavn>
         namespace: <ditt teams namespace>
         cluster: dev-gcp
         commitmsg: ${{ github.event.head_commit.message}}
         fallback: Path til fallbackvisning hvis eksterne kall feiler // eksempelvis "http://tms-microfrontend-test.dev.nav.no/fallback"
         ssr: true
      secrets: inherit
```

1. **Etterspør tilganger**
   Be om tilgang til å oppdatere manifest og deploye applikasjonen til nais på slack kanalen #minside-microfrontends

1. **Deploy til produksjon**
   Når applikasjonen er klar for prodsetting, kan du kommentere inn update-manifest-prod og deploy-prod stegene i .github/workflows/deploy.yaml. Sørg for at de er fylt inn likt som i steg 3.
   
---

### Aktivere og deaktivere microfrontends

1. **Koble til kafka topicet**
   Abonner på [min-side-microfrontend-topicet](https://github.com/navikt/min-side-microfrontend-topic-iac). **NOTE:** `microfrontendId` skal være identisk med navnet på Github-repoet du opprettet basert på templatet tidligere i guiden.

1. **Send meldinger**
   Du kan nå sende oss Enable/Disable meldinger via Kafka for å skru aktivere/deaktivere microfrontenden for spesifikke brukere.

#### Meldingsformat

```json
// Enable
{
    "@action": "enable",
    "ident": <ident for bruker: fnr/dnr>,
    "microfrontend_id": <microfrontendId>,
    "sensitivitet": <nivå som kreves for å se innholdet i mikrofrontenden, gyldige verdier: substantial og high>,
    "@initiated_by": <ditt-team>
}
```

```json
// Disable
{
    "@action":  "disable",
    "ident": <ident for bruker: fnr/dnr>,
    "microfrontend_id": <microfrontendId>,
    "@initiated_by":<ditt-team>
}
```

---

**Tips** Et meldingsbygger-bibliotek finnes på [Github packages](https://github.com/navikt/tms-mikrofrontend-selector/packages/1875650)

---

#### Hva er sensitivitet?

`sensitivitet` samsvarer med [ID-portens ACR-verdier](https://docs.digdir.no/docs/idporten/oidc/oidc_protocol_id_token#acr-values).

|     Verdi     | Når brukes den?                                  |
| :-----------: | :----------------------------------------------- |
|    `high`     | Innhold krever innlogging med idporten-loa-high. |
| `substantial` | Innhold kan vises uavhengig av innloggingsnivå.  |

Hvis feltet utelates, antar systemet `high`. Logger brukeren inn med `idporten-loa-substantial` og det finnes microfrontends som krever `idporten-loa-high`, får brukeren tilbud om «step-up»-innlogging. Se også [NAIS-dokumentasjonen om security levels](https://docs.nais.io/security/auth/idporten/#security-levels).

## Rettningslinjer

For å sikre en helhetlig brukeropplevelse på tvers av ulike type innhold på Min side, stiller vi visse krav til både innhold i- og utforming av microfrontends. Tabellen under viser en oversikt over disse rettningslinjene.

|     Tema     | Krav og resurser                                                                                                                                                                                   |
| :----------: | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
|    Språk     | Alt innhold skal finnes på bokmål, nynorsk og engelsk. Språkhåndtering er allerede rigget i templaten – se `src/language/text.ts`.                                                                 |
|    Design    | Vi stiller visse [designkrav](https://aksel.nav.no/god-praksis/artikler/retningslinjer-for-design-av-mikrofrontends) til utformingen av microfrontends, for å sikre en helhetlig brukeropplevelse. |
| Dependencies | Dersom du benytter deg av client-side React komponenter bør du være på samme Major versjon som [tms- min-side](https://github.com/navikt/tms-min-side).                                                                                                                                                                                                |
|  Analytics   | Vi bruker dekoratøren sin analyticsfunksjon - se `src/pages/[locale]/index.astro`.                                                                                                                                                                                         |
|  Fallback    | Dersom microfrontenden har eksterne kall bør du tilby en fallback - se `src/pages/[locale]/fallback.astro` |

## Plassering på Min side

Min side består av tre soner der team kan plassere innhold:

|         Seksjon         | Formål                                                                                                                                                                                                                                                                                                 | Teknisk støtte          |
| :---------------------: | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :---------------------- |
|      Din oversikt       | Personlig status og løpende saker relatert til brukerens nåværende forhold til Nav.                                                                                                                                                                                                                    | `kafka`                 |
|       Produktkort       | Produktkort er strengt talt ikke microfrontender, men regelbaserte lenker som peker til innloggede produktsider for ett område. Vi anbefaler heller å bruke kafka, siden dette er mer treffsikkert i forhold til brukers situasjon, men hvis kafka ikke er en mulighet kan dette være ett alternativ.  | `Kafka` & `Regelbasert` |
| Kanskje aktuelt for deg | Under kanskje aktuelt for deg skal bruker få forslag til annet innhold som kan være relevant for hen, for eksempel andre stønader eller støttetjenester en bruker kan ha rett på gitt at hen har en spesifikk ytelse. Foreløbig er det kun regelbaserte mikrofrontender som vises i den her seksjonen. | `Kafka` & `Regelbasert` |

## Lurer du på noe?

Dersom du har spørsmål, kan disse stilles i [#minside-microfrontends](https://nav-it.slack.com/archives/C04V21LT27P) kanalen på slack.
