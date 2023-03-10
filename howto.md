# mikrofrontender på min side

Mikrofrontends gir teamene mulighet til å presentere den viktigste informasjonen til brukerne sine på Min side. Kan ses
på som et butikkvindu inn til teamenes egne løsninger.

## Nice! Hvordan kan mitt team få en mikrofrontend på min side?

1. Lag en mikrofrontend! Om du vil bruke ett template finnes
   det [ett for typescript](https://github.com/navikt/tms-mikrofrontend-template-vitets) og ett for
   [ett for javascrip](https://github.com/navikt/tms-mikrofrontend-template-vitejs)
2. Opprett ett issue i [tms-min-side repoet](https://github.com/navikt/tms-min-side), be om å få lagt inn
   mikrofrontenden i kode og avtal en `<microfrontendId>`
3. Koble på [min-side-microfrontend-topicet](https://github.com/navikt/min-side-microfrontend-topic-iac)

Om en mikrofrontend vises avhenger av om den er enablet for en gitt bruker. Dette setter du ved å sende en melding på
microfrontend-topicet.

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
      "@action":  "disable",
      "ident": <ident for bruker, vanligvis fnr>,
      "microfrontend_id": <microfrontendId>
   }
   ```