/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawRect
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.FontValue
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.roundToInt

@ModuleInfo(name = "NameTags", spacedName = "Name Tags", description = "Changes the scale of the nametags so you can always read them.", category = ModuleCategory.RENDER)
class NameTags : Module() {
    private val healthValue = BoolValue("Health", true)
    private val healthBarValue = BoolValue("Bar", true)
    private val pingValue = BoolValue("Ping", true)
    private val distanceValue = BoolValue("Distance", false)
    private val armorValue = BoolValue("Armor", true)
    private val clearNamesValue = BoolValue("ClearNames", false)
    private val fontValue = FontValue("Font", Fonts.font40)
    private val borderValue = BoolValue("Border", true)
    private val backgroundColorRedValue = IntegerValue("Background-R", 0, 0, 255)
    private val backgroundColorGreenValue = IntegerValue("Background-G", 0, 0, 255)
    private val backgroundColorBlueValue = IntegerValue("Background-B", 0, 0, 255)
    private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 0, 0, 255)
    private val borderColorRedValue = IntegerValue("Border-R", 0, 0, 255)
    private val borderColorGreenValue = IntegerValue("Border-G", 0, 0, 255)
    private val borderColorBlueValue = IntegerValue("Border-B", 0, 0, 255)
    private val borderColorAlphaValue = IntegerValue("Border-Alpha", 0, 0, 255)
    private val scaleValue = FloatValue("Scale", 1F, 1F, 4F)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        glPushAttrib(GL_ENABLE_BIT)
        glPushMatrix()

        // Disable lightning and depth test
        glDisable(GL_LIGHTING)
        glDisable(GL_DEPTH_TEST)

        glEnable(GL_LINE_SMOOTH)

        // Enable blend
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        for (entity in mc.theWorld.loadedEntityList) {
            if (!EntityUtils.isSelected(entity, false))
                continue

            renderNameTag(entity as EntityLivingBase,
                    if (clearNamesValue.get())
                        ColorUtils.stripColor(entity.getDisplayName().unformattedText) ?: continue
                    else
                        entity.getDisplayName().unformattedText
            )
        }

        glPopMatrix()
        glPopAttrib()

        // Reset color
        resetColor()
        glColor4f(1F, 1F, 1F, 1F)
    }

    private fun renderNameTag(entity: EntityLivingBase, tag: String) {
        val fontRenderer = fontValue.get()

        // Modify tag
        val bot = AntiBot.isBot(entity)
        val nameColor = if (bot) "§3" else if (entity.isInvisible) "§6" else if (entity.isSneaking) "§4" else "§7"
        val ping = if (entity is EntityPlayer) EntityUtils.getPing(entity) else 0

        val distanceText = if (distanceValue.get()) "§7 [§a${mc.thePlayer.getDistanceToEntity(entity).roundToInt()}§7]" else ""
        val pingText = if (pingValue.get() && entity is EntityPlayer) " §7[" + (if (ping > 200) "§c" else if (ping > 100) "§e" else "§a") + ping + "ms§7]" else ""
        val healthText = if (healthValue.get()) "§7 [§f" + entity.health.toInt() + "§c❤§7]" else ""
        val botText = if (bot) " §7[§6§lBot§7]" else ""

        val text = "$nameColor$tag$healthText$distanceText$pingText$botText"

        // Push
        glPushMatrix()

        // Translate to player position
        val timer = mc.timer
        val renderManager = mc.renderManager


        glTranslated( // Translate to player position with render pos and interpolate it
                entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY + entity.eyeHeight.toDouble() + 0.55,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
        )

        glRotatef(-mc.renderManager.playerViewY, 0F, 1F, 0F)
        glRotatef(mc.renderManager.playerViewX, 1F, 0F, 0F)


        // Scale
        var distance = mc.thePlayer.getDistanceToEntity(entity) * 0.25f

        if (distance < 1F)
            distance = 1F

        val scale = distance / 100f * scaleValue.get()

        glScalef(-scale, -scale, scale)

        AWTFontRenderer.assumeNonVolatile = true

        // Draw NameTag
        val width = fontRenderer.getStringWidth(text) * 0.5f

        val dist = width + 4F - (-width - 2F)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)

        val bgColor = Color(backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get(), backgroundColorAlphaValue.get())
        val borderColor = Color(borderColorRedValue.get(), borderColorGreenValue.get(), borderColorBlueValue.get(), borderColorAlphaValue.get())

        if (borderValue.get())
            quickDrawBorderedRect(-width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F + if (healthBarValue.get()) 2F else 0F, 2F, borderColor.rgb, bgColor.rgb)
        else
            quickDrawRect(-width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F + if (healthBarValue.get()) 2F else 0F, bgColor.rgb)

        if (healthBarValue.get())
            quickDrawRect(-width - 2F, fontRenderer.FONT_HEIGHT + 3F, -width - 2F + (dist * (entity.health.toFloat() / entity.maxHealth.toFloat()).coerceIn(0F, entity.maxHealth.toFloat())), fontRenderer.FONT_HEIGHT + 4F, Color(10, 255, 10).rgb)

        glEnable(GL_TEXTURE_2D)

        fontRenderer.drawString(text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F,
                0xFFFFFF, true)

        AWTFontRenderer.assumeNonVolatile = false

        if (armorValue.get() && entity is EntityPlayer) {
            for (index in 0..4) {
                if (entity.getEquipmentInSlot(index) == null)
                    continue

                mc.renderItem.zLevel = -147F
                mc.renderItem.renderItemAndEffectIntoGUI(entity.getEquipmentInSlot(index), -50 + index * 20, -22)
            }

            enableAlpha()
            disableBlend()
            enableTexture2D()
        }

        // Pop
        glPopMatrix()
    }
}
