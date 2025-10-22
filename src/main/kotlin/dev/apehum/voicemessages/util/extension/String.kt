package dev.apehum.voicemessages.util.extension

import su.plo.slib.libs.adventure.adventure.text.Component
import su.plo.slib.libs.adventure.adventure.text.minimessage.MiniMessage
import su.plo.slib.libs.adventure.adventure.text.minimessage.tag.resolver.TagResolver

fun String.miniMessage(vararg resolvers: TagResolver): Component = MiniMessage.miniMessage().deserialize(this, *resolvers)
