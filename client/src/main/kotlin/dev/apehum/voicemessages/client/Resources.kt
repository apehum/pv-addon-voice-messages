package dev.apehum.voicemessages.client

import net.minecraft.resources.ResourceLocation

const val NAMESPACE = "pv_addon_voice_messages"

fun modResource(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(NAMESPACE, path)
