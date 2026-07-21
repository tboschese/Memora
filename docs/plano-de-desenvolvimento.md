# Plano de Desenvolvimento — Memora

Plano de engenharia para construir o app a partir do zero. Complementa (não substitui) o
`product-spec-memora.md` (o quê e por quê) e o `CLAUDE.md` (regras e stack). Aqui está o **como**:
estrutura de código, ordem de construção, contratos, testes e gates de build.

O protótipo navegável de UI está em [`index.html`](../index.html) — serve de referência visual
para as telas das Fases 1–3 (Hoje, Digest, Buscar, Ajustes, Calendário, Glossário, Enrollment,
notificação/shade). Ele é maquete: a fonte da verdade de comportamento continua sendo a spec.

---

## 0. Princípios de engenharia (guia de toda decisão)

Filosofia: **local-first, privacidade por padrão**. Tudo funciona 100% offline; a nuvem é
**opcional e opt-in** (ex.: backup de anotações no Drive, modelos pagos), nunca requisito.

1. **Local-first, rede opt-in.** Todas as capacidades essenciais rodam offline. Acesso à rede é
   permitido, mas só via features explícitas de sync/modelos remotos — nunca telemetria/analytics.
   Um relatório de build lista permissões de rede (informativo, ver §9) para nada entrar às cegas.
2. **Áudio é efêmero (default).** Não guardamos PCM cru: buffer só em memória/arquivo temporário
   entre captura e confirmação de transcrição; destruição imediata depois.
3. **Só texto persiste localmente**, em SQLCipher com chave derivada da credencial via Android Keystore.
   Se houver sync, o que sai do device é escolha explícita do usuário.
4. **Captura ≠ leitura.** Gravação e fila rodam com o app bloqueado; a senha protege apenas a leitura.
5. **Nunca chutar speaker.** Baixa confiança ⇒ `UNKNOWN`.
6. **Providers atrás de interface**, com fake em memória — tudo testável sem device/modelos/rede.

Convenções: Kotlin + Compose + MVVM + Coroutines/Flow; commits pequenos por capacidade, mensagens em
inglês no imperativo; nenhum log com transcrição, coordenadas ou conteúdo de anotação (só IDs e métricas).

---

## 1. Setup do projeto (pré-Fase 0, ~1–2 dias)

Objetivo: esqueleto multi-módulo que compila e roda uma tela vazia, com os gates já ligados.

### 1.1 Estrutura de módulos (Gradle, version catalogs)

```
:app                      # host: navegação, DI (Hilt), Application, MainActivity
:core:common              # tipos base, Result, Clock, dispatchers, logging seguro
:core:db                  # Room + SQLCipher, entidades, DAOs, migrations
:core:audio               # AudioRecord + Foreground Service + buffer efêmero
:core:transcription       # TranscriptionProvider + fila (WorkManager)
:core:speaker             # ECAPA-TDNN (ONNX) + enrollment + classificação
:core:location            # FusedLocation + lugares nomeados + GeocodingProvider
:core:digest              # DigestProvider (LLM local) + prompts versionados
:core:glossary            # GlossaryEntry + injeção nos 3 pontos do pipeline
:core:security            # Keystore, derivação de chave, BiometricPrompt, auto-lock
:core:models              # gestão de modelos sideload (presença + checksum)
:feature:today            # tela "Hoje" (timeline)
:feature:digest           # tela Digest + Calendário + Resumo do dia
:feature:notes            # sheet de anotação + anotação por voz + blind note
:feature:search           # busca full-text (FTS)
:feature:settings         # ajustes, glossário, enrollment, parâmetros
:feature:onboarding       # setup de PIN, permissões, sideload inicial
```

Regra de dependência: `:feature:*` → `:core:*` → `:core:common`. Features nunca dependem de features.
Cada `:core:*` externo (transcription, speaker, location, digest, models) expõe **interface** +
implementação real + **fake** (source set de teste compartilhado).

### 1.2 Toolchain e config base
- Kotlin, AGP atuais; `compileSdk`/`targetSdk` 35 (Android 15), `minSdk` 31 (device dedicado).
- Hilt para DI. Compose BOM. Detekt + ktlint. JUnit5 + Turbine (Flow) + Robolectric onde couber.
- `build-logic/` (convention plugins) para não repetir config entre módulos.
- **Baseline dos gates ativados já aqui** (§9), mesmo com o app vazio, para nunca regredir.

### 1.3 Entregável de saída
App instala e abre uma tela placeholder; `./gradlew check` roda os gates de manifest/áudio/log verdes.

---

## 2. Contratos a definir primeiro (antes de qualquer implementação de fase)

Definir as fronteiras estáveis. Assinaturas ilustrativas (Kotlin):

```kotlin
// :core:transcription
interface TranscriptionProvider {
    suspend fun transcribe(chunk: AudioChunk, opts: WhisperOptions): TranscriptResult
}

// :core:speaker
interface SpeakerClassifier {
    suspend fun embed(chunk: AudioChunk): FloatArray          // roda ANTES do descarte
    fun classify(emb: FloatArray, profile: VoiceProfile): SpeakerLabel // SELF/OTHER/UNKNOWN
}

// :core:digest
interface DigestProvider {
    suspend fun generate(input: DigestInput): DigestJson       // saída JSON validada por schema
}

// :core:location
interface GeocodingProvider { suspend fun resolve(p: LatLng): PlaceLabel } // offline / lugares nomeados

// :core:models
interface ModelRegistry {
    fun status(): List<ModelStatus>                            // presença + checksum
    suspend fun verify(): List<ChecksumResult>
}
```

Toda interface ganha um **Fake** determinístico (fixtures de embedding, transcrições canned, digest
mock) para permitir Fases 1–4 sem device/modelos reais. Isso é o que torna a fila, o pipeline de
destruição e o digest testáveis em CI.

---

## 3. Fase 0 — Spike técnico (~1 semana)

**Objetivo:** derrubar as duas maiores incertezas antes de escrever produto. Código descartável/isolado
em `:spike` (não entra no app final), medindo no **device alvo**, não no emulador.

Tarefas:
- [ ] Foreground service (`foregroundServiceType=microphone`) gravando **8h** contínuas sem ser morto.
- [ ] Silero VAD (ONNX Runtime) on-device; medir custo e precisão de "há fala?".
- [ ] ECAPA-TDNN (ONNX): gerar embedding de um chunk e classificar "minha voz vs outra" com perfil de teste.
- [ ] whisper.cpp (JNI): velocidade real (× realtime) e WER pt-BR nos modelos `base`/`small`/`medium` q5.
- [ ] LLM local (Qwen 2.5 3B / Gemma 3 4B via llama.cpp): gerar um digest de teste e avaliar qualidade/tempo.
- [ ] Pipeline cru ponta-a-ponta: chunk → VAD → whisper.cpp → texto, **em modo avião** (prova de zero rede).
- [ ] Medir bateria e throttling térmico em captura + transcrição.

**Gate (obrigatório para seguir):** 8h de captura estável com **< 5%/h** de bateria **E** transcrição
local acompanhando o volume do dia sem backlog crescente. Documentar números em `docs/spike-results.md`.
Se não passar, o resto é maquete — reavaliar modelo/estratégia antes da Fase 1.

---

## 4. Fase 1 — MVP: Gravador + Transcrição (~2–3 semanas)

**Objetivo:** o dia vira texto, com confiança de que nada se perde.

### 4.1 Segurança e persistência (base)
- [ ] `:core:security`: derivação de chave a partir de PIN + Keystore; setup de PIN no onboarding.
- [ ] `:core:db`: Room sobre SQLCipher; entidades `Session`, `Segment` (+ FTS depois); migrations.
- [ ] Auto-lock por timeout + BiometricPrompt como atalho; **captura segue em background** ao bloquear.

### 4.2 Captura (`:core:audio`)
- [ ] `AudioRecord` em Foreground Service com notificação persistente (RF-01, RF-02).
- [ ] Chunking 30–60s; **buffer efêmero**; flush periódico (perda máx. em crash: 60s — RNF-03).
- [ ] VAD descarta silêncio (RF-03); auto-pausa/retomada configurável (RF-04).
- [ ] Detectar mic tomado por outro app → registrar **gap** na timeline (risco conhecido).
- [ ] Sobrevivência a reboot opcional (RF-05); wakelock parcial + watchdog (metas OEM).

### 4.3 Pipeline de transcrição (`:core:transcription`)
- [ ] Fila assíncrona via WorkManager; política contínuo vs. lote (carregando/ocioso) — RF-10, RF-11.
- [ ] `TranscriptionProvider` real (whisper.cpp) + Fake; idioma pt-BR + auto (RF-12).
- [ ] **Destruição do chunk** imediatamente após texto persistido; política de descarte por limite
      de fila (200 MB) com gap registrado — RF-13, RNF-02.

### 4.4 UI (`:feature:today`)
- [ ] Tela "Hoje": transcrição do dia em ordem cronológica + card de gravação (ref. protótipo).
- [ ] Start/stop pela tela e pela notificação.

**Critério de saída:** uso real por 5 dias seguidos com confiança de que nada se perdeu.
**DoD:** compila/roda no device; gate de áudio efêmero + ausência de INTERNET verdes; bateria medida.

---

## 5. Fase 2 — Anotações + Digest diário (~2 semanas)

**Objetivo:** o texto vira conhecimento.

### 5.1 Anotações (`:feature:notes`)
- [ ] Sheet de anotação (app) + anotação pela notificação, com timestamp exato ancorado ao segmento
      (RF-06, RF-07); tags rápidas `#reunião/#ideia/#tarefa/#pessoal` customizáveis (RF-08).
- [ ] **Blind note**: escrita às cegas pela notificação/shade sem desbloquear a leitura (regra 4).

### 5.2 Reconhecimento de voz (`:core:speaker`)
- [ ] Enrollment: fluxo de ~2 min em 2 ambientes → `VoiceProfile` (embedding) criptografado (RF-18).
- [ ] Classificação SELF/OTHER/UNKNOWN por cosseno, **antes do descarte** do áudio (RF-19, regra 5).
- [ ] Exibir atribuição na timeline; alimentar digest (compromissos meus vs. contexto) — RF-20.
- [ ] Threshold configurável; re-treino sob demanda (RF-21, RF-22).

### 5.3 Localização (`:core:location`)
- [ ] Amostra a cada 2h + início/fim de sessão + passive listener (RF-26, RF-27).
- [ ] Lugares nomeáveis reconhecidos por proximidade, **sem geocoding online** (RF-28).
- [ ] Segmentos/anotações herdam o lugar vigente; digest usa lugar como eixo (RF-29, RF-30).

### 5.4 Glossário (`:core:glossary`)
- [ ] CRUD de `GlossaryEntry` (grafia canônica, variantes de erro, descrição).
- [ ] Injeção nos 3 pontos: `initial_prompt` do Whisper (respeitar ~224 tokens, priorizar frequentes),
      correção determinística pós-transcrição, system prompt do digest (RF-31, RF-32).
- [ ] "Adicionar como correção automática" ao corrigir manualmente (RF-33).

### 5.5 Digest (`:core:digest` + `:feature:digest`)
- [ ] `DigestProvider` (LLM local) com **saída JSON validada por schema**; prompts versionados em `assets/prompts/`.
- [ ] Digest automático (21h ou ao plugar carregador) e sob demanda (RF-14, RF-15, RF-16); latência < 60s (RNF-06).
- [ ] Timeline unificada + tela Digest (blocos, decisões, meus/terceiros, temas) — ref. protótipo.
- [ ] Parâmetros avançados de Whisper/LLM/VAD/speaker em Ajustes com reset (RF-34).

**Critério de saída:** o digest de um dia real é útil sem edição manual.

---

## 6. Fase 3 — Consulta e refinamento (~2 semanas)

- [ ] Busca full-text (Room FTS) em transcrições e anotações, com filtro por data e tag (RF-24) — `:feature:search`.
- [ ] Q&A sobre o histórico via LLM local (RF-17).
- [ ] Export Markdown por dia com frontmatter (Obsidian/PARA) (RF-25).
- [ ] Anotação por voz: segurar e ditar, transcrita na hora e marcada como nota (RF-09).
- [ ] Quick Settings tile para gravar/pausar (RF-01, backlog #1).
- [ ] Ajustes de bateria/estabilidade com base em 2+ semanas de uso real.

---

## 7. Fase 4 — Inteligência avançada (contínuo)

- [ ] Weekly review (digest semanal cruzando 7 dias).
- [ ] Follow-up de action items ("isso foi feito?").
- [ ] Diarização de speakers (Pessoa 1/2, renomeação) — avaliar custo.
- [ ] Detecção automática de reuniões (múltiplas vozes, > 15 min).
- [ ] Grafo de tópicos; wake word para anotar; modo "reunião presencial".

---

## 8. Modelos sideload (`:core:models`)

- [ ] Pasta de app para `.gguf`/`.onnx`; tela em Ajustes lista presença + checksum (ref. protótipo).
- [ ] Verificação de checksum e "Instalar modelo…" (seleção de arquivo local, sem download).
- [ ] Bloquear features que dependem de modelo ausente com mensagem clara (degradação graciosa).

---

## 9. Estratégia de testes e gates de build

Defaults de privacidade viram CI (dois quebram o build; rede é só relatório, pois é opt-in):

1. **Relatório de rede** — task `reportNetwork<Variant>` lista as permissões de rede do manifest
   mergeado (todas as deps). **Não** quebra o build (rede é permitida para sync/modelos), mas dá
   visibilidade para telemetria acidental não entrar despercebida. Roda em `:app` no `check`.
2. **Áudio efêmero (quebra o build)** — teste do pipeline provando que, após confirmação de
   transcrição, nenhum arquivo de áudio sobrevive; fuzz de caminhos de erro (crash no meio, fila
   cheia) sem vazar PCM em disco.
3. **Log seguro (quebra o build)** — a API `SafeLog` (tipo `Field` selado) impede logar texto livre;
   teste garante que só id/métrica/estágio saem no log.

Testes unitários de áreas críticas (com Fakes): derivação de chave; classificação de speaker com
fixtures de embedding (inclui casos `UNKNOWN`); política da fila (descarte por 200 MB + gap);
validação do schema JSON do digest; injeção do glossário nos 3 pontos; herança de lugar.

Camadas: unit (JVM, rápido) → Robolectric (Android sem device) → instrumentado no **device alvo** para
áudio/serviço/bateria. CI roda unit + gates em toda PR; instrumentado no device antes de fechar fase.

---

## 10. Riscos e mitigações (resumo — detalhe na §9 da spec)

| Risco | Mitigação |
|---|---|
| OEM mata o foreground service | device dedicado + battery optimization off + wakelock + watchdog |
| Compute local (bateria/calor) | VAD agressivo, transcrição em lote ao carregar, digest à noite, monitorar throttling |
| Mic compartilhado interrompe captura | detectar e registrar gap na timeline |
| Digest fraco com modelo pequeno | saída JSON com schema rígido + iterações de prompt |
| Speaker ID impreciso | threshold + `UNKNOWN` em baixa confiança; nunca atribuir errado |
| WER de áudio ambiente | testar posicionamento; avaliar mic USB-C no device fixo |

---

## 11. Sequenciamento e marcos

Ordem **obrigatória** (spec): captura confiável antes de qualquer IA.

- **M0 — Setup** (~1–2 d): esqueleto multi-módulo + gates verdes.
- **M1 — Spike aprovado** (~1 sem): gate de 8h/bateria/backlog batido e documentado.
- **M2 — MVP** (~2–3 sem): o dia vira texto; 5 dias de uso sem perda.
- **M3 — Conhecimento** (~2 sem): anotações + speaker + localização + glossário + digest útil.
- **M4 — Consulta** (~2 sem): busca, Q&A, export, anotação por voz, tile.
- **M5+ — Avançado** (contínuo): weekly review, diarização, detecção de reunião.

Regra de ouro: nenhum marco fecha sem os gates da §9 verdes e (se tocou captura/VAD/localização)
consumo de bateria medido no device alvo.

---

## 12. Próximos passos imediatos

1. Criar o esqueleto multi-módulo (§1) e ligar os 3 gates (§9) com o app ainda vazio.
2. Definir as interfaces + Fakes (§2) — destrava o desenvolvimento paralelo das features.
3. Montar `:spike` e atacar o Gate M1 no device alvo (§3).

> Quando quiser, eu começo pelo passo 1: gero o `settings.gradle.kts`, os convention plugins e os
> módulos vazios já com os testes-gate falhando de propósito (INTERNET/áudio/log) para travar o baseline.
