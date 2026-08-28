package com.example

import org.junit.Test
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.MultipleGradientPaint
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin

class LogoGeneratorTest {

    @Test
    fun generateHighResLogos() {
        val assetDir = File("../assets")
        if (!assetDir.exists()) {
            assetDir.mkdirs()
        }
        val targetDir = if (assetDir.exists()) assetDir else File("assets").apply { mkdirs() }

        generateLogo(512, File(targetDir, "echo_logo_512.png"))
        generateLogo(1024, File(targetDir, "echo_logo_1024.png"))
        println("Successfully generated 512x512 and 1024x1024 PNG logos at ${targetDir.absolutePath}")
    }

    private fun generateLogo(size: Int, outputFile: File) {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        // Enable highest quality antialiasing and rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)

        val center = size / 2.0f
        val cornerRadius = size * 0.225f

        // 1. Dark Background Squircle
        val squircle = RoundRectangle2D.Float(0f, 0f, size.toFloat(), size.toFloat(), cornerRadius, cornerRadius)
        val bgFractions = floatArrayOf(0.0f, 0.55f, 1.0f)
        val bgColors = arrayOf(
            Color(15, 23, 42),   // Slate 900
            Color(7, 11, 20),    // Dark Nebula
            Color(2, 4, 8)       // Deep Obsidian
        )
        val bgPaint = RadialGradientPaint(
            Point2D.Float(center, center),
            size * 0.75f,
            bgFractions,
            bgColors
        )
        g2d.paint = bgPaint
        g2d.fill(squircle)

        // 2. Subtle Outer Border Glow
        val borderGradient = GradientPaint(
            0f, 0f, Color(0, 240, 255, 120),
            size.toFloat(), size.toFloat(), Color(139, 92, 246, 120)
        )
        g2d.paint = borderGradient
        g2d.stroke = BasicStroke(size * 0.005f)
        g2d.draw(RoundRectangle2D.Float(size * 0.005f, size * 0.005f, size * 0.99f, size * 0.99f, cornerRadius * 0.98f, cornerRadius * 0.98f))

        // 3. Ambient Radial Nebula Glow
        val auraFractions = floatArrayOf(0.0f, 0.35f, 0.7f, 1.0f)
        val auraColors = arrayOf(
            Color(0, 240, 255, 90),
            Color(16, 185, 129, 50),
            Color(124, 77, 255, 30),
            Color(0, 0, 0, 0)
        )
        val auraPaint = RadialGradientPaint(
            Point2D.Float(center, center),
            size * 0.46f,
            auraFractions,
            auraColors
        )
        g2d.paint = auraPaint
        g2d.fill(Ellipse2D.Float(center - size * 0.46f, center - size * 0.46f, size * 0.92f, size * 0.92f))

        // 4. Outer Sonic Orbit Ring with dash
        val ring1Paint = GradientPaint(
            0f, 0f, Color(0, 240, 255, 160),
            size.toFloat(), size.toFloat(), Color(16, 185, 129, 140)
        )
        g2d.paint = ring1Paint
        val dash1 = floatArrayOf(size * 0.035f, size * 0.02f)
        g2d.stroke = BasicStroke(size * 0.007f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash1, 0f)
        val r1 = size * 0.34f
        g2d.draw(Ellipse2D.Float(center - r1, center - r1, r1 * 2, r1 * 2))

        // 5. Middle Glowing Sonic Ring
        val ring2Paint = GradientPaint(
            size.toFloat(), 0f, Color(236, 72, 153, 220),
            0f, size.toFloat(), Color(0, 240, 255, 220)
        )
        g2d.paint = ring2Paint
        g2d.stroke = BasicStroke(size * 0.011f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val r2 = size * 0.27f
        g2d.draw(Ellipse2D.Float(center - r2, center - r2, r2 * 2, r2 * 2))

        // 6. Inner Energy Halo
        g2d.color = Color(0, 240, 255, 170)
        g2d.stroke = BasicStroke(size * 0.005f)
        val r3 = size * 0.215f
        g2d.draw(Ellipse2D.Float(center - r3, center - r3, r3 * 2, r3 * 2))

        // 7. Glowing Holographic AI Orb Core (3D Shere Lighting)
        val orbRadius = size * 0.175f
        val orbCenter = Point2D.Float(center - orbRadius * 0.25f, center - orbRadius * 0.28f)
        val orbFractions = floatArrayOf(0.0f, 0.25f, 0.52f, 0.78f, 1.0f)
        val orbColors = arrayOf(
            Color(255, 255, 255, 255),
            Color(103, 232, 249, 255),
            Color(6, 182, 212, 255),
            Color(99, 102, 241, 255),
            Color(59, 7, 100, 255)
        )
        val orbPaint = RadialGradientPaint(
            orbCenter,
            orbRadius * 1.5f,
            orbFractions,
            orbColors
        )
        g2d.paint = orbPaint
        g2d.fill(Ellipse2D.Float(center - orbRadius, center - orbRadius, orbRadius * 2, orbRadius * 2))

        // 8. Dynamic Siri/Gemini Sine Acoustic Waveforms inside Core
        val wavePath1 = GeneralPath()
        val waveW = orbRadius * 1.35f
        val startX = center - waveW / 2
        wavePath1.moveTo(startX, center)
        wavePath1.curveTo(
            center - waveW * 0.25f, center - orbRadius * 0.65f,
            center - waveW * 0.05f, center + orbRadius * 0.65f,
            center, center
        )
        wavePath1.curveTo(
            center + waveW * 0.05f, center - orbRadius * 0.65f,
            center + waveW * 0.25f, center + orbRadius * 0.65f,
            center + waveW / 2, center
        )
        g2d.color = Color(255, 255, 255, 245)
        g2d.stroke = BasicStroke(size * 0.012f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2d.draw(wavePath1)

        val wavePath2 = GeneralPath()
        val waveW2 = orbRadius * 1.15f
        val startX2 = center - waveW2 / 2
        wavePath2.moveTo(startX2, center)
        wavePath2.curveTo(
            center - waveW2 * 0.22f, center + orbRadius * 0.45f,
            center - waveW2 * 0.05f, center - orbRadius * 0.45f,
            center, center
        )
        wavePath2.curveTo(
            center + waveW2 * 0.05f, center + orbRadius * 0.45f,
            center + waveW2 * 0.22f, center - orbRadius * 0.45f,
            center + waveW2 / 2, center
        )
        g2d.color = Color(165, 243, 252, 210)
        g2d.stroke = BasicStroke(size * 0.007f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2d.draw(wavePath2)

        g2d.dispose()

        outputFile.parentFile?.mkdirs()
        ImageIO.write(image, "png", outputFile)
    }
}
