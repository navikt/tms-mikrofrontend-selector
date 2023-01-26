# tms-mikrofrontend-selector
Tjeneste som holder oversikt over hvilke mikrofrontends som skal vises på min side

## Dokumentasjon

Dokumentasjon for produsenter finnes i [how-to](/howto.md) og i dokumnetasjonsidene til min side

### Oppdatere produsent-dokumentasjon

1. Oppdater howto.md. **NB!** Overskriftshierarkiet skal starte på `h1`/`# ` og beholde riktig sekvensiell struktur i
   hele dokumentet (`##`,deretter `###` osv).
2. Bygg og deloy tms-dokumentasjonen på nytt.

##Meldingsformater

1. Enable-melding når en bruker skal se mikrofrontenden
   ```json
   {
      "@action": "enable",
      "ident": <ident for bruker, vanligvis fnr>,
      "microfrontend_id": <microfrontendId>
   }
   ```
2. Disable-melding når bruker ikke skal se mikrofrontenden lenger
   ```json
   {
      "@action": "disable",
      "ident": <ident for bruker, vanligvis fnr>,
      "microfrontend_id": <microfrontendId>
   }
   ```

Se [min-side-microfrontend-topic-iac](https://github.com/navikt/min-side-microfrontend-topic-iac) for mer info