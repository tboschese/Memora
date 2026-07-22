# Memora

App Android **local-first** que transforma o seu dia em texto e conhecimento — captura de áudio
contínua, transcrição on-device e um digest diário, **100% offline por padrão**. Nenhum áudio cru
é guardado; só texto persiste, cifrado no device. A nuvem é **opcional e opt-in** (backup de
anotações, modelos pagos), nunca requisito.

> Status: em desenvolvimento. **M0 (esqueleto) e os contratos (§2) completos**; **Fase 1 (MVP:
> gravador + transcrição)** em andamento. Ver [Status atual](#status-atual).

---

## Princípios (guia de toda decisão)

1. **Local-first, rede opt-in.** Todas as capacidades essenciais rodam offline. Acesso à rede só
   via features explícitas de sync/modelos remotos — nunca telemetria/analytics.
2. **Áudio é efêmero.** Não guardamos PCM cru: o buffer vive só entre a captura e a confirmação da
   transcrição; destruição imediata depois.
3. **Só texto persiste**, em SQLCipher com chave derivada da credencial via Android Keystore.
4. **Captura ≠ leitura.** Gravação roda com o app bloqueado; a senha protege apenas a leitura.
5. **Nunca chutar speaker.** Baixa confiança ⇒ `UNKNOWN`.
6. **Providers atrás de interface**, com fake em memória — tudo testável sem device/modelos/rede.

Esses defaults viram **gates de build** (ver abaixo): áudio efêmero e log seguro **quebram o
build**; rede é relatório informativo (é permitida, mas só opt-in).

---

## Arquitetura

Multi-módulo Gradle (Kotlin + Compose + MVVM + Coroutines/Flow). Regra de dependência:
`:feature:* → :core:* → :core:common`. Features nunca dependem de features.

```
:app                  # host: navegação, DI (Hilt), Application, adaptadores de composição
:core:common          # tipos base, logging seguro (SafeLog)
:core:db              # Room + SQLCipher: entidades, DAOs, database
:core:audio           # captura + buffer efêmero (EphemeralAudioStore)
:core:transcription   # TranscriptionProvider + fila (TranscriptionQueue)
:core:speaker         # classificação SELF/OTHER/UNKNOWN (ECAPA-TDNN via ONNX)
:core:location        # lugares nomeados, offline (sem geocoding online)
:core:digest          # DigestProvider (LLM local), saída JSON validada por schema
:core:glossary        # grafias canônicas injetadas no pipeline
:core:security        # derivação de chave (PIN→Keystore), auto-lock
:core:models          # gestão de modelos sideload (presença + checksum)
:feature:*            # today, digest, notes, search, settings, onboarding
```

Cada `:core:*` externo expõe **interface + impl real + fake** determinístico, o que permite
desenvolver e testar todo o pipeline sem device, modelos ou rede.

---

## Build

**Pré-requisitos:** JDK 17 (AGP 8.7.3 exige 17) e o Android SDK (platform 35).

```bash
# JDK 17 via Homebrew (macOS):
brew install openjdk@17
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"   # ou o caminho do seu JDK 17
export PATH="$JAVA_HOME/bin:$PATH"

# local.properties com o caminho do SDK:
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

./gradlew check              # roda TODOS os gates + testes unitários (gate de CI)
./gradlew :app:assembleDebug
./gradlew test               # só unit tests
```

O `gradle-wrapper.jar` já está versionado, então `./gradlew` funciona direto após clonar.
Detalhes em [`docs/setup-e-build.md`](docs/setup-e-build.md).

### Gates & relatórios (local-first)

| # | Item | Efeito no build |
|---|---|---|
| 1 | **Relatório de rede** (`reportNetwork<Variant>`) | Lista permissões de rede do manifest mergeado. **Não falha** — visibilidade contra telemetria acidental. |
| 2 | **Áudio efêmero** ⛔ (`EphemeralAudioContractTest`) | **Quebra o build** se algum chunk sobreviver a `destroy()`. |
| 3 | **Log seguro** ⛔ (`SafeLog` + `SafeLogTest`) | **Quebra o build** se o log expuser texto livre — o tipo `Field` (selado) só aceita id/métrica/estágio. |

---

## Status atual

- ✅ **M0** — esqueleto multi-módulo, convention plugins (`build-logic/`), os 3 gates ligados.
- ✅ **§2 Contratos** — `TranscriptionProvider`, `SpeakerClassifier`, `DigestProvider`,
  `GeocodingProvider`, `ModelRegistry`, cada um com fake. Lógica pura testada: classificação de
  speaker (SELF/OTHER/UNKNOWN), correção por glossário.
- 🚧 **Fase 1 (MVP)** — em andamento:
  - ✅ `TranscriptionQueue`: pipeline `chunk → transcreve → persiste texto → destrói áudio`, com
    política contínuo vs. lote, descarte por limite (200 MB) com registro de gap, e destruição do
    áudio **só após** o texto persistir. Coberta por testes unitários.
  - ✅ `:core:db`: entidades `Session`/`Segment`/`TimelineGap`, DAOs e `MemoraDatabase` (Room).
  - ✅ Adaptadores fila↔banco (`RoomSegmentSink`/`RoomGapSink`) com teste de integração real
    (Room in-memory).
  - ✅ `:core:security`: derivação de chave PIN→chave (PBKDF2, sem persistir a chave), verificação
    de PIN, store cifrado por Keystore e auto-lock — testados; DI via `SecurityModule` (Hilt).
  - ✅ Seam DB cifrado (`buildEncryptedDatabase`, SQLCipher) ligando a chave derivada ao banco.
  - ✅ `:core:audio`: `PcmChunker` (chunking 30–60s), `EnergyVad` (baseline até o Silero/ONNX), o
    `EphemeralAudioStore` real em arquivo (cifrado com chave efêmera por processo, sem resíduo em
    disco após `destroy`) e o `CapturePipeline` que liga `chunking → VAD → store` e emite os chunks
    prontos para a fila. Todos testados.
  - ✅ **Pipeline ponta-a-ponta provado** por teste de integração: PCM cru → `CapturePipeline` →
    store efêmero → `TranscriptionQueue` → `RoomSegmentSink` → banco, com o áudio destruído só após
    o texto persistir.
  - ✅ `:feature:today` (leitura da tela "Hoje"): `TodayViewModel` combina falas + gaps do dia em
    uma timeline cronológica (`TodayTimeline`, junção pura), com `DayRange` traduzindo "hoje" no
    fuso do usuário para o intervalo que os DAOs consultam, e `CaptureController` como *seam* de
    start/stop. Leitura real sobre o Room (`RoomTodayRepository`) mora em `:app`, espelhando
    `RoomSegmentSink` — a UI não conhece o schema do banco. Tudo testado (fakes + Room in-memory).
  - ⏭️ Captura real (`AudioRecord` + Foreground Service) e a tela Compose de "Hoje" ligada ao
    `TodayViewModel`; whisper.cpp (JNI); fluxo de unlock/onboarding.

O que hoje é substituível por implementações reais sem tocar no resto: o `TranscriptionProvider`
(fake → whisper.cpp), o `VoiceActivityDetector` (energia → Silero/ONNX), a fonte de PCM
(`CapturePipeline.onAudio` ← `AudioRecord`) e o `CaptureController` (fake → Foreground Service). O
miolo — fila, efemeridade, persistência, segurança e a leitura da timeline — já está testado.

Plano de engenharia completo em [`docs/plano-de-desenvolvimento.md`](docs/plano-de-desenvolvimento.md).
O protótipo navegável de UI está em [`index.html`](index.html).

---

## Privacidade

Memora não coleta telemetria e não faz analytics. Hoje o app não declara **nenhuma** permissão de
rede — o relatório de build confirma isso a cada `check`. Quando/se um recurso de sync (ex.: backup
no Drive) for adicionado, ele fica atrás de um provider **desligado por padrão**, é ativado
explicitamente nos Ajustes e é a única porta pela qual dado sai do device — e nunca áudio.
