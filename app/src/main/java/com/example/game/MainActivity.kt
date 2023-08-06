package com.example.game
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.example.game.GameView.Companion.screenHeight
import com.example.game.GameView.Companion.screenWidth

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = GameView(this)
        setContentView(gameView)
    }

    override fun onResume() {
    super.onResume()
    gameView.resume()
}

override fun onPause() {
    super.onPause()
    gameView.pause()
}
}

class GameView(context: Context) : SurfaceView(context), Runnable {
    private var playing = false
    private var gameThread: Thread? = null
    private var lastFrameTime: Long = 0
    private var player: Player
    private var chaser: Chaser
    private var obstacles: ArrayList<Obstacle>
    private var score: Int = 0
    private var highScore: Int = 0
    private val canvasBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.bitmap)
    private val canvas: Canvas = Canvas()
    private val surfaceHolder: SurfaceHolder = holder
    private val paint: Paint = Paint()

    init {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        player = Player(BitmapFactory.decodeResource(resources, R.drawable.player), screenWidth,
            screenHeight)
        chaser = Chaser(BitmapFactory.decodeResource(resources, R.drawable.chaser))
        obstacles = ArrayList()

        for (i in 0 until MAX_OBSTACLES) {
            val obstacle = Obstacle(BitmapFactory.decodeResource(resources, R.drawable.obstacle))
            obstacle.x = (screenWidth + (i * screenWidth / MAX_OBSTACLES)).toFloat()
            obstacles.add(obstacle)
        }
    }

    private fun update() {
        player.update()

        chaser.update()


        for (obstacle in obstacles) {
            obstacle.update()


            if (Rect.intersects(player.getCollisionRect(), obstacle.getCollisionRect())) {
                if (!player.isSlowedDown()) {
                    player.slowDown()
                } else {
                    gameOver()
                }
            }
        }


        if (Rect.intersects(player.getCollisionRect(), chaser.getCollisionRect())) {
            gameOver()
        }


        if (obstacles[0].x < -obstacles[0].width) {
            obstacles.removeAt(0)
            val lastObstacle = obstacles[obstacles.size - 1]
            val obstacle = Obstacle(BitmapFactory.decodeResource(resources, R.drawable.obstacle))
            obstacle.x = lastObstacle.x + screenWidth / MAX_OBSTACLES
            obstacles.add(obstacle)
            score++
        }
    }

    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            canvas.drawColor(Color.WHITE)
            paint.textSize = 30f
            paint.color = Color.BLACK
            canvas.drawText("Score: $score", 50f, 50f, paint)
            canvas.drawText("High Score: $highScore", 50f, 100f, paint)

            player.draw(canvas)
            chaser.draw(canvas)

            for (obstacle in obstacles) {
                obstacle.draw(canvas)
            }

            surfaceHolder.lockCanvas()?.apply {

                drawBitmap(canvasBitmap, 0f, 0f, null)
                surfaceHolder.unlockCanvasAndPost(this)
            }
        }
    }

    private fun gameOver() {
        if (score > highScore) {
            highScore = score
        }
        score = 0

        player.reset()
        obstacles.clear()

        for (i in 0 until MAX_OBSTACLES) {
            val obstacle = Obstacle(BitmapFactory.decodeResource(resources, R.drawable.obstacle))
            obstacle.x = (screenWidth + (i * screenWidth / MAX_OBSTACLES)).toFloat()
            obstacles.add(obstacle)
        }
    }

    override fun run() {
        while (playing) {
            update()
            draw()
            control()
        }
    }

    private fun control() {
        val frameTime = System.currentTimeMillis() - lastFrameTime

        val desiredFrameTime = 1000L / 60


        if (frameTime < desiredFrameTime) {
            try {

                Thread.sleep(desiredFrameTime - frameTime)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        lastFrameTime = System.currentTimeMillis()
    }

    fun pause() {
        playing = false
        gameThread?.join()
    }

    fun resume() {
        playing = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                player.jump()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val MAX_OBSTACLES = 5
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
    }
}

class Player(bitmap: Bitmap, private val screenWidth: Int, private val screenHeight: Int) {
    private var x: Float = 0f
    private var y: Float = 0f
    private var yVelocity: Float = 0f
    private var jumpPower: Float = -30f
    private var gravity: Float = 1.5f
    private var playerBitmap: Bitmap = bitmap
    private var frame: Int = 0
    private var frameCount: Int = 5
    private var frameWidth: Int = playerBitmap.width / frameCount
    private var frameHeight: Int = playerBitmap.height
    private var slowedDown: Boolean = false

    private var collisionRect: Rect = Rect()

    init {
        x = screenWidth / 2f - frameWidth / 2f
        y = screenHeight / 2f - frameHeight / 2f
    }

    fun update() {
        if (y < screenHeight - frameHeight || yVelocity < 0) {
            yVelocity += gravity
            y += yVelocity
        }
        frame = (frame + 1) % frameCount

        collisionRect.left = x.toInt()
        collisionRect.top = y.toInt()
        collisionRect.right = (x + frameWidth).toInt()
        collisionRect.bottom = (y + frameHeight).toInt()
    }

    fun jump() {
        if (y >= screenHeight - frameHeight) {
            yVelocity = jumpPower
        }
    }

    fun slowDown() {
        slowedDown = true
        gravity = 0.5f
    }

    fun reset() {
        slowedDown = false
        gravity = 1.5f
    }

    fun isSlowedDown(): Boolean {
        return slowedDown
    }

    fun getCollisionRect(): Rect {
        return collisionRect
    }

    fun draw(canvas: Canvas) {
        val srcRect = Rect(frame * frameWidth, 0, (frame + 1) * frameWidth, frameHeight)
        val dstRect = Rect(x.toInt(), y.toInt(), (x + frameWidth).toInt(), (y + frameHeight).toInt())
        canvas.drawBitmap(playerBitmap, srcRect, dstRect, null)
    }
}

class Chaser(bitmap: Bitmap) {
    private var x: Float = 0f
    private var y: Float = 0f
    private var chaserBitmap: Bitmap = bitmap

    private var collisionRect: Rect = Rect()
    init {
        x = screenWidth / 2f
        y = screenHeight / 2f
    }

    fun update() {

        x -= CHASER_SPEED
        collisionRect.left = x.toInt()
        collisionRect.top = y.toInt()
        collisionRect.right = (x + chaserBitmap.width).toInt()
        collisionRect.bottom = (y + chaserBitmap.height).toInt()
    }

    fun getCollisionRect(): Rect {
        return collisionRect
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(chaserBitmap, x, y, null)
    }

    companion object {
        private const val CHASER_SPEED = 10f
    }
}

class Obstacle(bitmap: Bitmap) {
    var x: Float = 0f
    private var obstacleBitmap: Bitmap = bitmap
    var width: Int = obstacleBitmap.width
    private var y: Float = 0f
    private var collisionRect: Rect = Rect()

    fun update() {
        x -= OBSTACLE_SPEED

        collisionRect.left = x.toInt()
        collisionRect.top = y.toInt()
        collisionRect.right = (x + obstacleBitmap.width).toInt()
        collisionRect.bottom = (y + obstacleBitmap.height).toInt()
    }

    fun getCollisionRect(): Rect {
        return collisionRect
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(obstacleBitmap, x, y, null)
    }

    companion object {
        private const val OBSTACLE_SPEED = 10f
    }
}
