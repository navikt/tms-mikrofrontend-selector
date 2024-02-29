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

### Legge til min-side-repo-authenticator
#### Gi tilgang til det nye repoet fra authenticator.
1. be om admin-tilgang til microfrontend-repoet
2. Gå til [repouthenticator på github](https://github.com/organizations/navikt/settings/apps/min-side-repo-authenticator)
3. Install App (i menyen til venstre) -> Settings (tannhjul til høyre)
4. Velg microfrontend-repoet i dropdown-lista og trykk på den grønne "Update access"-knappen 
5. Kopier secrets fra prod-gcp og legg det inn i repoet som skal bruke authenticator.
