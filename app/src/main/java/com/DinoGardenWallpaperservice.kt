package com.example.dinogardenwallpaper

import android.service.wallpaper.WallpaperService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.HandlerThread
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.example.dinogardenwallpaper.R

enum class WallpaperMode { ONLINE_PEACEFUL, OFFLINE_GAME }

class DinoGardenWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DinoGardenEngine()

    private inner class DinoGardenEngine : Engine() {

        private val frameDelayMs = 16L
        private val gravity = 2600f
        private val jumpVelocity = -1500f
        private val groundRatio = 0.78f
        private val obstacleSpeed = 480f
        private val dinoFrameSwitchMs = 120L
        private val dinoHeightRatio = 0.16f
        private val obstacleHeightRatio = 0.12f
        private val dinoXRatio = 0.10f

        private var mode = WallpaperMode.ONLINE_PEACEFUL

        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var groundY = 0f
        private var isSurfaceReady = false

        @Volatile
        private var isVisible = false

        private var dinoX = 0f
        private var dinoY = 0f
        private var dinoVelocityY = 0f
        private var isJumping = false
        private var dinoWidth = 0
        private var dinoHeight = 0
        private var dinoFrameIndex = 0
        private var lastFrameSwitchAt = 0L
        private var obstacleX = 0f
        private var obstacleWidth = 0
        private var obstacleHeight = 0
        private var lastUpdateAt = 0L

        private val handlerThread = HandlerThread("DinoGardenRenderThread").apply { start() }
        private val renderHandler = Handler(handlerThread.looper)

        private val drawRunnable = object : Runnable {
            override fun run() {
                update()
                draw()
                if (isVisible) {
                    renderHandler.postDelayed(this, frameDelayMs)
                }
            }
        }

        private lateinit var bgBitmap: Bitmap
        private lateinit var dinoRun1: Bitmap
        private lateinit var dinoRun2: Bitmap
        private lateinit var obstacleBitmap: Bitmap

        private lateinit var connectivityManager: ConnectivityManager

        private val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                renderHandler.post { switchToOffline() }
            }

            override fun onAvailable(network: Network) {
                renderHandler.post { switchToOnline() }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val usable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                renderHandler.post { if (usable) switchToOnline() else switchToOffline() }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            loadBitmaps()
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            registerNetworkCallback()
            checkInitialConnectivity()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            groundY = height * groundRatio
            isSurfaceReady = true
            renderHandler.post {
                setupGameDimensions()
                resetGame()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            isSurfaceReady = false
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                renderHandler.removeCallbacks(drawRunnable)
                renderHandler.post {
                    lastUpdateAt = System.currentTimeMillis()
                    drawRunnable.run()
                }
            } else {
                renderHandler.removeCallbacks(drawRunnable)
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                renderHandler.post { if (mode == WallpaperMode.OFFLINE_GAME) jump() }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: IllegalArgumentException) {
                // Safe to ignore
            }
            renderHandler.removeCallbacksAndMessages(null)
            renderHandler.post { recycleBitmaps() }
            handlerThread.quitSafely()
        }

        private fun loadBitmaps() {
            bgBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg_garden)
            dinoRun1 = BitmapFactory.decodeResource(resources, R.drawable.dino_run1)
            dinoRun2 = BitmapFactory.decodeResource(resources, R.drawable.dino_run2)
            obstacleBitmap = BitmapFactory.decodeResource(resources, R.drawable.obstacle_bush)
        }

        private fun recycleBitmaps() {
            if (!bgBitmap.isRecycled) bgBitmap.recycle()
            if (!dinoRun1.isRecycled) dinoRun1.recycle()
            if (!dinoRun2.isRecycled) dinoRun2.recycle()
            if (!obstacleBitmap.isRecycled) obstacleBitmap.recycle()
        }

        private fun registerNetworkCallback() {
            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } catch (e: Exception) {
                mode = WallpaperMode.ONLINE_PEACEFUL
            }
        }

        private fun checkInitialConnectivity() {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            val hasInternet = capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            renderHandler.post { if (hasInternet) switchToOnline() else switchToOffline() }
        }

        private fun setupGameDimensions() {
            dinoHeight = (surfaceHeight * dinoHeightRatio).toInt().coerceAtLeast(1)
            val dinoAspect = dinoRun1.width.toFloat() / dinoRun1.height.toFloat()
            dinoWidth = (dinoHeight * dinoAspect).toInt().coerceAtLeast(1)
            dinoX = surfaceWidth * dinoXRatio

            obstacleHeight = (surfaceHeight * obstacleHeightRatio).toInt().coerceAtLeast(1)
            val obstacleAspect = obstacleBitmap.width.toFloat() / obstacleBitmap.height.toFloat()
            obstacleWidth = (obstacleHeight * obstacleAspect).toInt().coerceAtLeast(1)
        }

        private fun switchToOffline() {
            if (mode == WallpaperMode.OFFLINE_GAME) return
            mode = WallpaperMode.OFFLINE_GAME
            resetGame()
        }

        private fun switchToOnline() {
            mode = WallpaperMode.ONLINE_PEACEFUL
        }

        private fun resetGame() {
            if (surfaceWidth == 0 || surfaceHeight == 0) return
            dinoY = groundY - dinoHeight
            dinoVelocityY = 0f
            isJumping = false
            dinoFrameIndex = 0
            lastFrameSwitchAt = System.currentTimeMillis()
            obstacleX = surfaceWidth.toFloat() + obstacleWidth
            lastUpdateAt = System.currentTimeMillis()
        }

        private fun jump() {
            if (!isJumping) {
                isJumping = true
                dinoVelocityY = jumpVelocity
            }
        }

        private fun update() {
            if (!isSurfaceReady) return
            val now = System.currentTimeMillis()
            val deltaSeconds = (now - lastUpdateAt).coerceIn(0, 100) / 1000f
            lastUpdateAt = now

            if (mode == WallpaperMode.OFFLINE_GAME) {
                updateGame(deltaSeconds, now)
            }
        }

        private fun updateGame(deltaSeconds: Float, now: Long) {
            dinoVelocityY += gravity * deltaSeconds
            dinoY += dinoVelocityY * deltaSeconds
            val groundLevel = groundY - dinoHeight
            if (dinoY >= groundLevel) {
                dinoY = groundLevel
                dinoVelocityY = 0f
                isJumping = false
            }

            if (!isJumping && now - lastFrameSwitchAt > dinoFrameSwitchMs) {
                dinoFrameIndex = 1 - dinoFrameIndex
                lastFrameSwitchAt = now
            }

            obstacleX -= obstacleSpeed * deltaSeconds
            if (obstacleX + obstacleWidth < 0) {
                obstacleX = surfaceWidth.toFloat() + obstacleWidth
            }

            val dinoRect = RectF(dinoX, dinoY, dinoX + dinoWidth, dinoY + dinoHeight)
            val obstacleRect = RectF(obstacleX, groundY - obstacleHeight, obstacleX + obstacleWidth, groundY)
            if (RectF.intersects(dinoRect, obstacleRect)) {
                obstacleX = surfaceWidth.toFloat() + obstacleWidth
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    when (mode) {
                        WallpaperMode.ONLINE_PEACEFUL -> drawPeacefulGarden(canvas)
                        WallpaperMode.OFFLINE_GAME -> drawGame(canvas)
                    }
                }
            } catch (e: Exception) {
                // Surface torn down safely
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // Surface gone
                    }
                }
            }
        }

        private fun drawPeacefulGarden(canvas: Canvas) {
            drawScaledBackground(canvas)
        }

        private fun drawGame(canvas: Canvas) {
            drawScaledBackground(canvas)

            val dinoBitmap = if (dinoFrameIndex == 0) dinoRun1 else dinoRun2
            val dinoSrc = Rect(0, 0, dinoBitmap.width, dinoBitmap.height)
            val dinoDst = Rect(dinoX.toInt(), dinoY.toInt(), (dinoX + dinoWidth).toInt(), (dinoY + dinoHeight).toInt())
            canvas.drawBitmap(dinoBitmap, dinoSrc, dinoDst, null)

            val obstacleSrc = Rect(0, 0, obstacleBitmap.width, obstacleBitmap.height)
            val obstacleDst = Rect(obstacleX.toInt(), (groundY - obstacleHeight).toInt(), (obstacleX + obstacleWidth).toInt(), groundY.toInt())
            canvas.drawBitmap(obstacleBitmap, obstacleSrc, obstacleDst, null)
        }

        private fun drawScaledBackground(canvas: Canvas) {
            if (surfaceWidth <= 0 || surfaceHeight <= 0) return
            val src = Rect(0, 0, bgBitmap.width, bgBitmap.height)
            val dst = Rect(0, 0, surfaceWidth, surfaceHeight)
            canvas.drawBitmap(bgBitmap, src, dst, null)
        }
    }
}
