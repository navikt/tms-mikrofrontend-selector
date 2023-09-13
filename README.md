# tms-mikrofrontend-selector
Tjeneste som holder oversikt over hvilke mikrofrontends som skal vises på min side

## Dokumentasjon

Dokumentasjon for produsenter finnes i [how-to](/howto.md) og i dokumnetasjonsidene til min side

### Oppdatere produsent-dokumentasjon

1. Oppdater howto.md. **NB!** Overskriftshierarkiet skal starte på `h1`/`# ` og beholde riktig sekvensiell struktur i
   hele dokumentet (`##`,deretter `###` osv).
2. Bygg og deloy tms-dokumentasjonen på nytt.

## API - eksempel

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