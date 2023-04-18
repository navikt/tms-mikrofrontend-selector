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
      "microfrontend_id": <microfrontendId>,
      "sikkerhetsnivå" : <innloggingsnivå som er påkrevd for å se innholdet i mikrofrontenden, gyldige verdier: 3 eller 4>
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

## FAQ

### Hva bør jeg velge som mikrofrontend-id?
`<område>.<tjenste>` er ett bra utgangspunkt, prøv å holde det så generelt som mulig, men gjenkjenbart.

### Hva er egentlig innloggngsnivå og sikkerhetsnivå?

Når en person logger inn på NAV.no kan hen ha gått igjennom forskjelige tjenester, der noen anses som 
mer sikker (nivå 4)  og andre anses som mindre sikker (nivå 3). Feltet `sikkerhetsnivå` i enablemeldingen
korresponderer direkte til disse innloggingsnivåene. Altså; hvis det ligger informasjon i mikrofrontenden 
som kun personer som har logger inn på nivå 4 skal kunne se, skal `sikkerhetsnivå` settes 4. Om informasjonen
kan vises uavhengig av innloggingsnivå skal sikkerhetsnivå sette til 3. Om en person logger inn på nivå 3
og det finnes mikrofrontender som personen kan se på nivå 4 vil bruker få beskjed om dette og link til en "steup"
loging.

Om sikkerhetsnivå ikke er spesifisert settes det alltid til 4 hos oss.