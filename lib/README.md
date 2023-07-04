# Messagebuilder for microfronten-topic

Bibliotek som bygger meldinger
for [min-side-microfrontend-topicet](https://github.com/navikt/min-side-microfrontend-topic-iac)
Dokumentasjon om microfrontends finner du på [våre dokumentasjonsider](https://tms-dokumentasjon.intern.nav.no/mikrofrontend)

## Enable meldinger

```kotlin


MessageBuilder.enable {
    ident = "12345678910" // ident til bruker som skal ha mikrofrontent: req 11 siffer
    initiatedBy = "ditt-team"
    microfrontendId = "din-mikrofrontend-id"
    sensitivitet = SENSITIVITET.SUBSTANTIAL  //Valgfritt, default er HIGH. Se dokumentasjonen for mer info
}

MessageBuilder.enable(
    ident = expectedIdent, //11 siffer
    initiatedBy = expectedInitiatedBy,
    microfrontendId = expectedMicrofrontendId,
    sensitivitet = SENSITIVITET.SUBSTANTIAL  //Valgfritt, default er HIGH. Se dokumentasjonen for mer info

)
```

## Disable meldinger

```
MessageBuilder.disable {
    ident = expectedIdent
    initiatedBy = expectedInitiatedBy
    microfrontendId = expectedMicrofrontendId
}

MessageBuilder.disable(
    ident = expectedIdent,
    microfrontenId = expectedMicrofrontendId,
    initiatedBy = expectedInitiatedBy
)
```