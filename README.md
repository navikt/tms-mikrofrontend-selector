# tms-mikrofrontend-selector
Tjeneste som holder oversikt over hvilke mikrofrontends som skal vises på min side

## Dokumentasjon for produsenter
Dokumentasjon for produsenter finnes i [how-to](/howto.md) og i dokumnetasjonsidene til min side

### Oppdatere produsent-dokumentasjon
Oppdater howto.md. **NB!** Overskriftshierarkiet skal starte på `h1`/`# ` og beholde riktig sekvensiell struktur i 
hele dokumentet (`##`,deretter `###` osv).

## Dokumentasjon for min-side utvklere

### API - eksempel

`/microfrontends`

```json
{
   "microfrontends": [
      {
         "microfrontend_id": "mk1",
         "url": "https://cdn.eks/mk1jsurl8888.997.js"
      },
      {
         "microfrontend_id": "mk2",
         "url": "https://cdn.eks/mk2jsurl99998.9888.js"
      }
   ],
   "offerStepup": false,
   "__offerStepup_desc__": "Boolean, hvorvidt bruker har mikrofrontender som ikke kan vises ved gjeldende sensitivitetsnivå"
}
```

### Gi produsent-repo tilgang til å trigge actions i selector
1. Oppdater [oversikten over microfrontends](https://navno.sharepoint.com/:x:/r/sites/Teampersonbruker/_layouts/15/Doc.aspx?sourcedoc=%7B566CB64A-D4E2-4672-A740-8C9B7CC9D460%7D&file=Mikrofrontends.xlsx&action=default&mobileredirect=true)
2. Be om admin-tilgang til produsent-repoet
3. Gå til [repouthenticator på github](https://github.com/organizations/navikt/settings/apps/min-side-repo-authenticator)
4. Install App (i menyen til venstre) -> Settings (tannhjul til høyre)
5. Velg produsent-repoet i dropdown-lista og trykk på den grønne "Update access"-knappen
6. Legg inn secrets i produsent-repoet; Settings -> Secrets and variables -> Actions -> New repository secret
   1. `PRIVATE_KEY` fra `github-auth-app-key` i secrets i prod-gcp
   2. `APP_ID` fra "App ID" under "About" i repoauthenticator
