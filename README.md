# 🎤 PocketMiku — AI Miku Companion (Android)

Личный AI-компаньон Хацунэ Мику на твоём телефоне. **VRM 3D-аватар с физикой**, отвечает голосом Miku, локальный LLM через Luna-proxy. Полностью offline на Poco X7 Pro.

## Стек

- **UI:** Kotlin + Jetpack Compose
- **Аватар:** WebView + three-vrm (Mali-G720 WebGL2, физика SpringBone)
- **LLM:** Luna proxy на `127.0.0.1:8765` (Termux крутит) → Qwen3-Max
- **TTS:** piper en_US-amy (v0.2+)
- **Miku-голос:** RVC v2 ONNX post-process поверх piper (v0.4+)

## Версии

| | Что |
|--|--|
| v0.1 ← **сейчас** | VRM viewer + Luna chat (без TTS) |
| v0.2 | piper TTS + audio playback + lip-sync через viseme |
| v0.3 | VRMExpression эмоции (sentiment → happy/sad/angry) |
| v0.4 | RVC Miku ONNX post-process → studio Miku quality |
| v0.5 | SQLite memory, persona finetuning |
| v0.6 | Voice input через whisper.cpp |
| v0.7 | Background service, push-уведомления от Miku |

## Запуск Luna-proxy (требуется для чата)

В Termux на том же устройстве:

```bash
cd ~/projects/luna-proxy
PORT=8765 PUPPETEER_SKIP_DOWNLOAD=true \
  ./node_modules/.bin/ts-node-dev --transpile-only src/dev.ts
```

PocketMiku ходит на `http://127.0.0.1:8765/v1/chat/completions` — поэтому Luna должна крутиться когда запускаешь приложение.

## Building

GitHub Actions собирает debug APK на каждый push в main:
- Repo → Actions → Build APK → последний run → MaidMic-debug artifact

Локально:
```bash
./gradlew assembleDebug
```

## Лицензии assets

- **Miku VRM:** outrine/HatsuneMiku_VRM_Model, free use with credit, **не для VRChat** (для нашего личного app — OK)
- **piper TTS:** MIT
- **three-vrm:** MIT
- **three.js:** MIT
- **RVC Miku v2:** HidekoHaruna/RVC_V2_Hatsune_Miku, **non-commercial**

## Структура

```
app/src/main/
├── java/com/korvus/pocketmiku/
│   ├── MainActivity.kt              # Scaffold + AvatarController bridge
│   ├── avatar/
│   │   └── VrmAvatarView.kt         # WebView + asset interceptor для miku.vrm
│   ├── llm/
│   │   └── LunaClient.kt            # OkHttp + multi-turn (providerSessionId)
│   ├── ui/
│   │   ├── chat/
│   │   │   ├── ChatScreen.kt        # Compose overlay снизу + sentiment→emotion
│   │   │   └── ChatViewModel.kt     # state + viewModelScope.launch
│   │   └── theme/Theme.kt           # Miku-teal Material3 dark
│   └── persona/
│       └── MikuPersona.kt           # SYSTEM_PROMPT
├── assets/
│   ├── vrm/miku4.vrm                # 44MB VRM модель
│   ├── tts/en_US-amy-medium.onnx    # 61MB piper TTS
│   └── webview/
│       ├── index.html               # three-vrm scene + Miku JS API
│       ├── three.min.js             # 637KB
│       └── three-vrm.min.js         # 145KB
└── res/...
```
