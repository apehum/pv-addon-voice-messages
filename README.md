![](.github/assets/icon.png)

<div>
    <a href="https://modrinth.com/mod/plasmo-voice">Plasmo Voice</a>
    <span> | </span>
    <a href="https://modrinth.com/plugin/pv-addon-voice-messages">Modrinth</a>
    <span> | </span>
    <a href="https://github.com/Apehum/pv-addon-voice-messages/">GitHub</a>
    <span> | </span>
    <a href="https://discord.com/invite/uueEqzwCJJ">Discord</a>
     <span> | </span>
    <a href="https://www.patreon.com/plasmomc">Patreon</a>
</div>


# pv-addon-voice-messages

Server-side [Plasmo Voice](https://github.com/plasmoapp/plasmo-voice) addon that adds voice messages.

![](.github/assets/output.gif)

## Commands

- `/vm` — Record and send a voice message to all players (same as `/vm default`)
- `/vm default` — Record and send a voice message to all players
- `/vm direct <player>` — Record and send a voice message to a specific player

## Configuration

The configuration file is located at `plugins/pv-addon-voicemessages/config.toml` (`/config/pv-addon-voicemessages/config.toml` for Fabric/Forge/NeoForge).

```toml
# Activation to use for recording
activation = "proximity"
max_duration_seconds = 5
actionbar_when_recording = true
# Source line weight controls sorting order in "Volume"
# Higher weights are placed at the bottom of the list
source_line_weight = 100
# Available storage types: [MEMORY, REDIS]
storage_type = "MEMORY"

[chat_format]
default = "<lang:chat.type.text:'<player_name>':'<voice_message>'>"
direct_incoming = "<italic><gray><lang:commands.message.display.incoming:'<source_player_name>':'<voice_message>'>"
direct_outgoing = "<italic><gray><lang:commands.message.display.outgoing:'<target_player_name>':'<voice_message>'>"

# Redis configuration (required if storageType is REDIS)
#[redis]
#host = "localhost"
#port = 6379
#user = ""
#password = ""
```
## API

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("dev.apehum.voicemessages:voicemessages:1.0.0-beta.1")
}
```
