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
    uma timeline cronológica (`TodayTimeline`, junção pura), com o `DayRange` de `:core:common`
    traduzindo "hoje" no fuso do usuário para o intervalo que os DAOs consultam, e `CaptureController` como *seam* de
    start/stop. Leitura real sobre o Room (`RoomTodayRepository`) mora em `:app`, espelhando
    `RoomSegmentSink` — a UI não conhece o schema do banco. Tudo testado (fakes + Room in-memory).
  - ✅ `:feature:onboarding` (fluxo de PIN): `PinPolicy` (forma do PIN, pura), `OnboardingViewModel`
    (definir PIN em dois passos, escolher→confirmar) e `UnlockViewModel` (desbloqueio), atrás do
    `PinGate` — a UI nunca vê a chave. O `SecurityPinGate` real mora em `:app`: deriva a chave via
    `PinVault`, abre a sessão cifrada (`EncryptedSession`, seam de device) e destranca o auto-lock,
    zerando a chave em seguida. Testado com `PinVault` sobre store em memória (sem device/PBKDF2 caro).
  - ✅ `SessionCoordinator` (`:app`): o cérebro de navegação — combina "existe PIN?" com o estado do
    auto-lock e expõe a fase da sessão (`ONBOARDING`/`LOCKED`/`UNLOCKED`) como `StateFlow`. Máquina de
    estados pura (relógio por parâmetro), reagindo a autenticou/atividade/timeout/lock. Governa só a
    leitura — a captura segue em background (regra 4). Testado sem device.
  - ⏭️ Captura real (`AudioRecord` + Foreground Service) e as telas Compose (Hoje, PIN) ligadas aos
    ViewModels/`SessionCoordinator`; a `EncryptedSession` real sobre `buildEncryptedDatabase`;
    whisper.cpp (JNI).
- 🚧 **Fase 2 (Anotações + Digest)** — iniciada pela leitura, sem device:
  - ✅ `:feature:digest` (§5.5): `DigestViewModel` gera, sob demanda, o resumo estruturado do dia —
    busca as fontes (`DigestSources`, *seam*), delega a síntese ao `DigestProvider` (fake nos testes,
    LLM local depois) e expõe `Idle/Generating/Ready/Empty/Failed`. Dia sem falas não chama o modelo;
    falha do provider degrada para `Failed` sem derrubar a tela. `RoomDigestSources` (`:app`) lê o
    mesmo dia que a timeline. Testado (fakes + Room in-memory).
  - ✅ `:core:glossary` (§5.4, ponto 1 da injeção): `GlossaryPrompt` monta o `initial_prompt` do
    Whisper a partir das grafias canônicas, dentro de um orçamento de ~224 tokens e priorizando os
    mais frequentes (pula termo que não cabe, dedup case-insensitive). A contagem de tokens é um
    *seam* (heurística → BPE real do Whisper). Puro e testado. Soma-se à correção pós-transcrição
    (`GlossaryCorrector`) já existente.
  - ✅ `:core:speaker` (§5.2, enrollment): `VoiceEnrollment.buildProfile` funde as amostras de
    embedding (o fluxo real: ~2 min em 2 ambientes) no `VoiceProfile` do dono — centroide normalizado
    (L2) — e devolve a `cohesion` das amostras como proxy de qualidade (gancho de re-treino, RF-22).
    Puro (o embedding em si é do modelo ECAPA-TDNN); soma-se ao `decideSpeaker` já testado.
    `SpeakerAttribution.attribute` classifica vários segmentos de uma vez centralizando a **regra 5**:
    sem perfil (enrollment não feito), tudo vira `UNKNOWN` — nunca chutar. Testado.
  - ✅ `:core:location` (§5.3, lugar vigente): `PlaceTimeline` colapsa as amostras de localização em
    intervalos de "lugar vigente" com histerese (uma leitura espúria na borda de um raio não troca
    o lugar) e resolve o lugar de um instante — como segmentos/anotações herdam o lugar (RF-29/30).
    Puro; a coordenada→lugar continua no `GeocodingProvider` offline (Haversine, zero rede).
  - ✅ `:feature:notes` (§5.1, anotações): `NotesViewModel` observa as notas do dia e salva um
    `NoteDraft` (id/timestamp atribuídos por *seam*, rascunho vazio ignorado), atrás do
    `NotesRepository`. Nova entidade `NoteEntity` + `NoteDao` (banco na v2), com tags rápidas
    (#reunião/#ideia/…) e âncora opcional a um segmento (RF-07/08). `RoomNotesRepository` (`:app`)
    faz o round-trip real. Testado (fakes + Room in-memory).
  - ✅ `:core:models` (§8, sideload): `FileModelRegistry` verifica presença e integridade dos modelos
    sideloaded (`.gguf`/`.onnx`) numa pasta — `statuses()` barato (só existência), `verify()`
    recalcula o SHA-256 fora da main thread. Modelo ausente degrada a feature; presente com hash
    divergente nunca é usado. Sem rede; testado com arquivos reais (JVM puro).
  - ✅ `:core:digest` (§5.5, agendamento): `DigestScheduler` decide, como política pura, quando gerar
    o digest do dia — prioriza o carregador entre 21h e 23h, gera de qualquer forma após 23h, e é
    idempotente por dia (RF-14/15/16). Sem relógio interno; tudo entra por parâmetro. Testado.
  - ✅ `:core:digest` (§5.5, saída validada por schema): `DigestJson.parse` valida e sanitiza o JSON
    do LLM local num `Digest` confiável — JSON malformado ou sem `summary` vira `null` (nada de
    digest-lixo na tela), listas ausentes viram vazias, itens não-string descartados, `epochDay` vem
    do chamador (nunca do modelo). Testado (org.json real sob Robolectric).
  - ✅ `:core:glossary` (§5.4, RF-33): `GlossaryEditor.learnCorrection` aprende com edições manuais —
    registra a grafia errada como variante da entrada canônica (cria a entrada se nova, sem duplicar,
    case-insensitive), e o `GlossaryCorrector` passa a corrigi-la sozinho. Puro e testado.
  - ✅ `:core:glossary` (§5.4, CRUD): `GlossaryRepository` (*seam*) + `GlossaryEntity`/`GlossaryDao`
    (banco na v3) persistem o glossário que alimenta os 3 pontos de injeção. `RoomGlossaryRepository`
    (`:app`) faz o round-trip real (variantes newline-separated). Testado (Room in-memory).
  - ✅ Glossário no pipeline (§5.4, ponto 2 ligado): `TranscriptionQueue` ganhou um `postProcess`
    (default identidade) aplicado **antes** de persistir — logo, antes do descarte do áudio. Em `:app`,
    `TranscriptResult.correctedBy(corrector)` liga o `GlossaryCorrector` ao pipeline sem que
    `:core:transcription` conheça o `:core:glossary`. Testado (queue + correção).
  - ✅ `:feature:settings` (§5.5, RF-34): `MemoraSettings` reúne os parâmetros avançados
    (Whisper/VAD/speaker/auto-lock/digest) com defaults sensatos e `normalized()` que clampa cada
    campo ao intervalo válido (idempotente) — o pipeline nunca recebe ajuste fora de faixa; reset =
    `DEFAULT`. Puro e testado. Em `:app`, `toWhisperOptions`/`toVoiceProfile` derivam os parâmetros
    concretos do pipeline a partir dos ajustes (sempre normalizados antes). Testado.
- 🚧 **Fase 3 (Consulta)** — iniciada pela lógica pura, sem device:
  - ✅ `:feature:search` (§6, busca): `SearchQueryParser` interpreta a caixa (`#tag`, `@speaker`,
    termos livres) e `SearchMatcher` é o matcher de referência — casa todos os termos (substring
    case-insensitive) + todas as tags + o speaker, em ordem cronológica decrescente; query vazia não
    casa nada. Puro e testado; o FTS do Room é aceleração posterior sobre este mesmo contrato.
  - ✅ `:core:common` (§6, export — RF-25): `MarkdownExporter` gera o Markdown de um dia com
    frontmatter YAML (Obsidian/PARA) — falas com speaker (menos `UNKNOWN`, regra 5), notas com tags,
    `places`/tags agregados no cabeçalho. Puro e determinístico (fuso por parâmetro). Testado.
  - ✅ `:core:common` (timeline unificada): `DayItem` (fala/nota/gap) + `DayTimeline.merge` intercalam
    as três fontes numa timeline cronológica única (ordenação estável, desempate determinístico).
    Modelo comum para a tela unificada intercalar sem um feature depender de outro. Puro e testado.
    Em `:app`, `RoomUnifiedTimeline` compõe a timeline reativa das três tabelas (falas + notas + gaps)
    — onde as fontes de features distintas se encontram. Testado (Room in-memory).
- 🚧 **Fase 4 (Inteligência avançada)** — primeira peça pura:
  - ✅ `:core:digest` (§7, weekly review): `WeeklyReview.aggregate` cruza os digests de vários dias
    num `WeeklyDigest` — temas por frequência (empate alfabético, top 10), decisões em ordem
    cronológica, action items sem repetir. Puro e testado.

O que hoje é substituível por implementações reais sem tocar no resto: o `TranscriptionProvider`
(fake → whisper.cpp), o `VoiceActivityDetector` (energia → Silero/ONNX), a fonte de PCM
(`CapturePipeline.onAudio` ← `AudioRecord`), o `CaptureController` (fake → Foreground Service) e a
`EncryptedSession` (fake → `buildEncryptedDatabase`). O miolo — fila, efemeridade, persistência,
segurança, o fluxo de PIN e a leitura da timeline — já está testado.

Plano de engenharia completo em [`docs/plano-de-desenvolvimento.md`](docs/plano-de-desenvolvimento.md).
O protótipo navegável de UI está em [`index.html`](index.html).

---

## Privacidade

Memora não coleta telemetria e não faz analytics. Hoje o app não declara **nenhuma** permissão de
rede — o relatório de build confirma isso a cada `check`. Quando/se um recurso de sync (ex.: backup
no Drive) for adicionado, ele fica atrás de um provider **desligado por padrão**, é ativado
explicitamente nos Ajustes e é a única porta pela qual dado sai do device — e nunca áudio.
