# Setup & Build — baseline M0

Scaffold multi-módulo do Memora com os gates das regras invioláveis já ligados. Este documento
explica como colocar pra rodar e o que cada gate faz. O plano completo está em
[`plano-de-desenvolvimento.md`](plano-de-desenvolvimento.md).

## Pré-requisitos

- **JDK 17** (o build usa Java 17). Confirme com `java -version`.
- **Android Studio** (Ladybug ou mais novo) **ou** o Android SDK via `cmdline-tools`.
- `local.properties` com o caminho do SDK (o Android Studio cria automático):
  ```
  sdk.dir=/Users/<voce>/Library/Android/sdk
  ```

## Materializar o Gradle wrapper (passo único)

Este repo já traz `gradlew`, `gradle/wrapper/gradle-wrapper.properties` e o `settings.gradle.kts`,
mas **não** o binário `gradle-wrapper.jar` (não versionável a partir daqui). Gere-o uma vez:

- **Via Android Studio:** abra a pasta do projeto → "Sync Project with Gradle Files". A IDE baixa a
  distribuição do Gradle (8.11.1) e materializa o wrapper.
- **Via CLI (se tiver Gradle instalado):**
  ```
  gradle wrapper --gradle-version 8.11.1
  ```

Depois disso, `./gradlew` funciona normalmente.

## Comandos

```bash
./gradlew help            # sanity check do wrapper
./gradlew check           # roda TODOS os gates + testes unitários (é o gate de CI)
./gradlew :app:assembleDebug
./gradlew test            # só unit tests
```

## Gates & relatórios (local-first)

Filosofia: **local-first, privacidade por padrão**. Tudo funciona offline; rede é opcional e opt-in
(backup no Drive, modelos pagos — futuro). Por isso rede é **relatório**, não bloqueio; os defaults de
privacidade (áudio efêmero, log seguro) é que quebram o build.

| # | Item | Onde | Efeito no build |
|---|---|---|---|
| 1 | **Relatório de rede** | `build-logic` → `reportNetwork<Variant>` | Lista permissões de rede do **manifest mergeado** (todas as deps). **Não falha** — só dá visibilidade contra telemetria acidental. |
| 1b | Rede (runtime) | [`NetworkPermissionsTest`](../app/src/test/kotlin/com/memora/app/NetworkPermissionsTest.kt) | Robolectric imprime permissões de rede não declaradas como intencionais. Informativo. |
| 2 | **Áudio efêmero** ⛔ | [`EphemeralAudioContractTest`](../core/audio/src/test/kotlin/com/memora/core/audio/EphemeralAudioContractTest.kt) | **Quebra o build** se algum chunk sobreviver a `destroy()`. Fase 1: impl real + prova de ausência de resíduo em disco. |
| 3 | **Log seguro** ⛔ | [`SafeLog`](../core/common/src/main/kotlin/com/memora/core/common/log/SafeLog.kt) + `SafeLogTest` | **Quebra o build** se o log expuser texto livre — o tipo `Field` (selado) só aceita id/métrica/estágio. |

> **Ao adicionar uma feature de rede** (sync no Drive, modelo pago): adicione `INTERNET` ao manifest,
> isole o acesso atrás de um provider opt-in (desligado por padrão) e registre a permissão como
> intencional em `NetworkPermissionsTest`. Rode `./gradlew check` — o relatório confirma o que entrou.

## Estrutura

```
build-logic/            # convention plugins (memora.android.*) + gate de rede
gradle/libs.versions.toml
app/                    # host: Application (Hilt), MainActivity, Compose placeholder
core/                   # common, db, security, audio, transcription, speaker,
                        # location, digest, glossary, models  (stubs + contratos)
feature/                # today, digest, notes, search, settings, onboarding (stubs)
index.html              # protótipo visual das telas (Fases 1–3)
```

Regra de dependência: `feature/* → core/* → core/common`. Features nunca dependem de features.

## O que já existe (M0) vs. o que vem a seguir

- ✅ App compila e abre uma tela placeholder.
- ✅ Defaults de privacidade no `check`: áudio efêmero + log seguro (quebram o build) e relatório de rede (informativo).
- ✅ Contrato de áudio efêmero + fake em memória (base do pipeline da Fase 1).
- ✅ Logging seguro estruturado.
- ✅ Interfaces + fakes dos providers (§2): `TranscriptionProvider`, `SpeakerClassifier`,
  `DigestProvider`, `GeocodingProvider`, `ModelRegistry` — cada um com fake determinístico. Lógica
  pura já testada: `decideSpeaker`/`cosineSimilarity` (SELF/OTHER/UNKNOWN) e `GlossaryCorrector`.
- ⏭️ **Próximo:** o `:spike` (Gate M1) no device alvo — 8h de captura estável + transcrição local
  acompanhando o volume do dia (§3 do plano). Em paralelo, a Fase 1 pode começar pela base testável
  em JVM (entidades/DAOs Room, política da fila de transcrição, derivação de chave).
