# Mikrofrontends på Min side

## Kom i gang

Ta utgangspunkt i [template for ssr mikrofrontend](https://github.com/navikt/tms-microfrontend-template-ssr) og opprett et nytt repository basert på denne templaten.

### Konfigurer applikasjonen


1. **Bytt ut applikasjonsnavn i konfigurasjonsfiler**
   
   CMD + Shift + F og søk etter tms-microfrontend-template-ssr og erstatt dette med ditt applikasjonsnavn.
   
3. **Tilpass nais.yaml**
   
   Tilpass både nais/dev-gcp/nais.yaml og nais/prod-gcp/nais.yaml. Alle felter som må tilpasses har tilhørende kommentar med instruksjon i template.


3. **Tilpass innholdet i .github/workflows/deploy.yaml**

   Tilpass deploy.yaml slik kommentarene i koden instruerer. Applikasjonsnavn skal være likt navn på repository.

   Du kan vente med å kommentere inn og tilpasse stegene update-manifest-prod og deploy prod til applikasjonen er klar for prodsetting. 

5. **Etterspør tilganger**
   
   Be om tilgang til å oppdatere manifest og deploye applikasjonen til nais på slack kanalen #minside-microfrontends

6. **Deploy til produksjon**
   
   Når applikasjonen er klar for prodsetting, kan du kommentere inn update-manifest-prod og deploy-prod stegene i .github/workflows/deploy.yaml. Sørg for at de er fylt inn likt som i steg 3.


## Bruk av template

### Språk
   
   Bruk språkoppsett satt opp i template og legg språkvariablene inn i /language/text.ts

### Fallback

   Dersom microfrontenden har eksterne kall bør du tilby en fallback - se `src/pages/[locale]/fallback.astro`

### Produktanalyse

   Vi bruker dekoratøren sin analyticsfunksjon - se `src/pages/[locale]/index.astro`

### CSS

   For lokal css kan du bruke css moduler som vanlig. 

   For bruk av designsystemet må medfølgende css isoleres til mikrofrontenden. Importer derfor kun de delene av ds-css som er i bruk. Aksel har laget [et verktøy for dette](https://aksel.nav.no/grunnleggende/kode/kommandolinje#56838966b1fc) som genererer listen med imports du trenger. Legg listen med imports i /styles/aksel.css.

### Client side interaktivitet

   Ved behov for client side interaktivitet kan [Astros Client Islands](https://docs.astro.build/en/concepts/islands/#client-islands) tas i bruk. Et eksempel ligger i /components/ClientIsland.tsx og oppfører seg som en vanlig React komponent. 

   Merk at interaktivitet er tilgjengeliggjort via React. Dette medfører overhead da React major versjonen må samsvare i mikrofrontend og på Min side.


### Design

   Vi stiller visse [designkrav](https://aksel.nav.no/god-praksis/artikler/retningslinjer-for-design-av-mikrofrontends) til utformingen av mikrofrontends, for å sikre en helhetlig brukeropplevelse.
   
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


## Lurer du på noe?

Dersom du har spørsmål, kan disse stilles i [#minside-microfrontends](https://nav-it.slack.com/archives/C04V21LT27P) kanalen på slack.
