# VulkanCompat

A client-side Fabric mod that makes Minecraft's Vulkan renderer run on older GPUs and drivers that do not support `VK_KHR_dynamic_rendering`.

## Why?

Minecraft's Vulkan renderer relies on the `VK_KHR_dynamic_rendering` extension for drawing. Many older GPUs and drivers (especially on Linux or older Windows drivers) do not support this extension.

Without this mod, the game still opens but the Vulkan renderer fails to start with an error like this:

```
[Render thread/WARN]: Device [NVIDIA GeForce GT 740] does not support required extensions, missing: [VK_KHR_dynamic_rendering]
[Render thread/WARN]: Device [NVIDIA GeForce GT 740] does not have required feature [dynamicRendering]
[Render thread/ERROR]: Failed to create backend Vulkan
com.mojang.blaze3d.systems.BackendCreationException: Device missing capabilities
	at knot//com.mojang.blaze3d.vulkan.VulkanBackend.throwForMissingRequrements(VulkanBackend.java:404)
	at knot//com.mojang.blaze3d.vulkan.VulkanBackend.findPhysicalDevice(VulkanBackend.java:261)
	at knot//com.mojang.blaze3d.vulkan.VulkanBackend.createDevice(VulkanBackend.java:149)
	at knot//net.minecraft.client.Minecraft.<init>(Minecraft.java:515)
	at knot//net.minecraft.client.main.Main.main(Main.java:278)
	at net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider.launch(MinecraftGameProvider.java:514)
	at net.fabricmc.loader.impl.launch.knot.Knot.launch(Knot.java:72)
	at net.fabricmc.loader.impl.launch.knot.KnotClient.main(KnotClient.java:23)
	at org.prismlauncher.launcher.impl.StandardLauncher.launch(StandardLauncher.java:115)
	at org.prismlauncher.EntryPoint.listen(EntryPoint.java:129)
	at org.prismlauncher.EntryPoint.main(EntryPoint.java:70)
```

**VulkanCompat** translates the renderer's dynamic rendering calls into the classic, universally supported render pass API (`vkCmdBeginRenderPass` / `vkCmdEndRenderPass`), so the Vulkan renderer works on hardware without the extension.

## Features

- Replaces `vkCmdBeginRenderingKHR` / `vkCmdEndRenderingKHR` with legacy render passes
- Replaces `vkCreateGraphicsPipelines` calls that use `VK_KHR_dynamic_rendering` pipelines with compatible render-pass-based pipelines
- Caches render passes and framebuffers to avoid performance loss
- Works automatically — **no configuration needed**, always active
- Uses the native `vkCmdBeginRenderPass` / `vkCmdEndRenderPass` when dynamic rendering is supported (no interference)
- Client-side only, lightweight

## How it works

When a driver does not advertise `VK_KHR_dynamic_rendering`:

- The renderer is allowed to initialize without the extension (`VK_KHR_dynamic_rendering` is stripped from the required device extensions and features)
- Every render pass drawn with `vkCmdBeginRenderingKHR` is converted into a legacy render pass + framebuffer and recorded with `vkCmdBeginRenderPass`
- Pipeline compilation that references dynamic rendering structures is rewritten to use the equivalent classic render pass layout

When the driver *does* support dynamic rendering, the mod does nothing and the game behaves exactly as vanilla.

## Compatibility

- Works with **any GPU** that supports Vulkan 1.0+ (which is practically all Vulkan-capable GPUs)
- GPU-specific drivers that lack `VK_KHR_dynamic_rendering` (e.g. older Intel, AMD, NVIDIA drivers; some Wine/Proton environments) benefit the most
- Client-side only; safe for singleplayer and multiplayer

<details>
<summary>Türkçe (Göster / Gizle)</summary>

# VulkanCompat

`VK_KHR_dynamic_rendering` desteklemeyen eski GPU'lar ve sürücüler için Minecraft'ın Vulkan renderer'ının çalışmasını sağlayan istemci tarafı (client-side) bir Fabric modu.

## Neden?

Minecraft'ın Vulkan renderer'ı çizim işlemleri için `VK_KHR_dynamic_rendering` eklentisine (extension) dayanır. Pek çok eski GPU ve sürücü (özellikle Linux veya eski Windows sürücüleri) bu eklentiyi desteklemez.

Bu mod olmadan oyun yine açılır ama Vulkan renderer'ı şuna benzer bir hatayla başlamaz:

```
[Render thread/WARN]: Device [NVIDIA GeForce GT 740] does not support required extensions, missing: [VK_KHR_dynamic_rendering]
[Render thread/WARN]: Device [NVIDIA GeForce GT 740] does not have required feature [dynamicRendering]
[Render thread/ERROR]: Failed to create backend Vulkan
com.mojang.blaze3d.systems.BackendCreationException: Device missing capabilities
	at knot//com.mojang.blaze3d.vulkan.VulkanBackend.throwForMissingRequrements(VulkanBackend.java:404)
	at knot//com.mojang.blaze3d.vulkan.VulkanBackend.findPhysicalDevice(VulkanBackend.java:261)
	at knot//com.mojang.blaze3d.vulkan.VulkanBackend.createDevice(VulkanBackend.java:149)
	at knot//net.minecraft.client.Minecraft.<init>(Minecraft.java:515)
	at knot//net.minecraft.client.main.Main.main(Main.java:278)
	at net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider.launch(MinecraftGameProvider.java:514)
	at net.fabricmc.loader.impl.launch.knot.Knot.launch(Knot.java:72)
	at net.fabricmc.loader.impl.launch.knot.KnotClient.main(KnotClient.java:23)
	at org.prismlauncher.launcher.impl.StandardLauncher.launch(StandardLauncher.java:115)
	at org.prismlauncher.EntryPoint.listen(EntryPoint.java:129)
	at org.prismlauncher.EntryPoint.main(EntryPoint.java:70)
```

**VulkanCompat** renderer'ın dinamik render çağrılarını her yerde desteklenen klasik render pass API'sine (`vkCmdBeginRenderPass` / `vkCmdEndRenderPass`) çevirir; böylece Vulkan renderer'ı eklentisi olmayan donanımda da çalışır.

## Özellikler

- `vkCmdBeginRenderingKHR` / `vkCmdEndRenderingKHR` çağrılarını eski tip render pass'lerle değiştirir
- `VK_KHR_dynamic_rendering` kullanan `vkCreateGraphicsPipelines` çağrılarını uyumlu render pass tabanlı pipeline'larla değiştirir
- Performans kaybını önlemek için render pass ve framebuffer'ları önbelleğe alır
- Otomatik çalışır — **ayar yapmanız gerekmez**, her zaman aktif
- Dinamik rendering desteklendiğinde doğal `vkCmdBeginRenderPass` / `vkCmdEndRenderPass` kullanır (müdahale etmez)
- İstemci tarafı, hafif

## Nasıl çalışır?

Sürücü `VK_KHR_dynamic_rendering` bildirmediğinde:

- Renderer'ın eklenti olmadan başlatılması sağlanır (`VK_KHR_dynamic_rendering`, gerekli cihaz eklentileri ve özelliklerinden çıkarılır)
- `vkCmdBeginRenderingKHR` ile çizilen her render pass, eski tip render pass + framebuffer'a dönüştürülüp `vkCmdBeginRenderPass` ile kaydedilir
- Dinamik rendering yapılarını kullanan pipeline derlemeleri, eşdeğer klasik render pass düzenine çevrilir

Sürücü dinamik rendering *destekliyorsa* mod hiçbir şey yapmaz ve oyun tamamen vanilya gibi çalışır.

## Uyumluluk

- Vulkan 1.0+ destekleyen **her GPU** ile çalışır (yani Vulkan destekleyen tüm GPU'lar)
- `VK_KHR_dynamic_rendering` eklentisi olmayan sürücüler (eski Intel, AMD, NVIDIA sürücüleri; bazı Wine/Proton ortamları) en çok fayda görür
- İstemci tarafı; tek oyunculu ve çok oyunculuda güvenle kullanılabilir

</details>
