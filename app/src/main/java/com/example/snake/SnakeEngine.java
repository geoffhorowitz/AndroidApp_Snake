package com.example.snake;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.view.GestureDetectorCompat;

import java.util.ArrayList;
import java.util.Random;

class SnakeEngine extends SurfaceView implements Runnable {

    // Our game thread for the main game loop
    private Thread thread = null;

    // To hold a reference to the Activity
    private Context context;

    // For tracking movement Heading
    public enum Heading {UP, RIGHT, DOWN, LEFT}
    // Start by heading to the right
    private Heading heading = Heading.RIGHT;

    // To hold the screen size in pixels
    private int screenX;
    private int screenY;

    // The snake body locations
    private ArrayList<Point> snakeBody;

    // Where is Bob hiding?
    private int bobX;
    private int bobY;

    // The size in pixels of a snake segment
    private int blockSize;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private int numBlocksHigh;

    // Control pausing between updates
    private long nextFrameTime;
    // Update the game 10 times per second
    private int FPS = 1;
    // There are 1000 milliseconds in a second
    private final long MILLIS_PER_SECOND = 1000;

    // How many points does the player have
    private int score;

    // Everything we need for drawing
    // Is the game currently playing?
    private volatile boolean isPlaying;

    // A canvas for our paint
    private Canvas canvas;

    // Required to use canvas
    private SurfaceHolder surfaceHolder;

    // Some paint for our canvas
    private Paint paint;
    
    // For swipe input detection
    private GestureDetectorCompat gestureDetector;

    public SnakeEngine(Context context, Point size, int speed) {
        super(context);

        this.context = context;

        screenX = size.x;
        screenY = size.y;

        // Work out how many pixels each block is
        blockSize = screenX / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        numBlocksHigh = screenY / blockSize;

        // Ensure minimum speed of 1 to avoid divide-by-zero
        FPS = Math.max(1, speed);

        // Initialize the drawing objects
        surfaceHolder = getHolder();
        paint = new Paint();

        // Unlimited snake length using dynamic array
        snakeBody = new ArrayList<>();
        
        setupGestureDetector();

        // Start the game
        newGame();
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true; // Must return true for onFling to be recognized natively
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                
                float deltaX = e2.getX() - e1.getX();
                float deltaY = e2.getY() - e1.getY();
                
                // Determine whether the swipe is predominantly horizontal or vertical
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    // Horizontal swipe
                    if (deltaX > 0 && heading != Heading.LEFT) {
                        heading = Heading.RIGHT;
                    } else if (deltaX < 0 && heading != Heading.RIGHT) {
                        heading = Heading.LEFT;
                    }
                } else {
                    // Vertical swipe
                    if (deltaY > 0 && heading != Heading.UP) {
                        heading = Heading.DOWN;
                    } else if (deltaY < 0 && heading != Heading.DOWN) {
                        heading = Heading.UP;
                    }
                }
                return true;
            }
        });
    }


    @Override
    public void run() {

        while (isPlaying) {

            // Update 10 times a second
            if(updateRequired()) {
                update();
                draw();
            }

        }
    }

    public void update() {
        // Safe check for body
        if (snakeBody.isEmpty()) return;
        
        Point head = snakeBody.get(0);
        
        // Did the head of the snake eat Bob?
        if (head.x == bobX && head.y == bobY) {
            eatBob();
        }

        moveSnake();

        if (detectDeath()) {
            newGame();
        }
    }

    public void pause() {
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public boolean updateRequired() {

        // Are we due to update the frame
        if(nextFrameTime <= System.currentTimeMillis()){
            // Setup when the next update will be triggered
            nextFrameTime = System.currentTimeMillis() + MILLIS_PER_SECOND / FPS;
            // Return true so that the update and draw functions are executed
            return true;
        }

        return false;
    }

    public void newGame() {
        snakeBody.clear();
        
        // Start with a single snake segment
        Point head = new Point();
        head.x = NUM_BLOCKS_WIDE / 2;
        head.y = numBlocksHigh / 2;
        snakeBody.add(head);

        heading = Heading.RIGHT;

        // Get Bob ready for dinner
        spawnBob();

        // Reset the score
        score = 0;

        // Setup nextFrameTime so an update is triggered
        nextFrameTime = System.currentTimeMillis();
    }

    public void spawnBob() {
        Random random = new Random();
        bobX = random.nextInt(NUM_BLOCKS_WIDE - 1) + 1;
        bobY = random.nextInt(numBlocksHigh - 1) + 1;
    }

    private void eatBob(){
        //  Got him!
        // Increase the size of the snake
        Point tail = snakeBody.get(snakeBody.size() - 1);
        snakeBody.add(new Point(tail.x, tail.y));
        
        //replace Bob
        spawnBob();
        //add to the score
        score = score + 1;
    }

    private void moveSnake(){
        // Move the body
        for (int i = snakeBody.size() - 1; i > 0; i--) {
            // Start at the back and move it
            // to the position of the segment in front of it
            Point current = snakeBody.get(i);
            Point previous = snakeBody.get(i - 1);
            current.x = previous.x;
            current.y = previous.y;
        }

        // Move the head in the appropriate heading
        Point head = snakeBody.get(0);
        switch (heading) {
            case UP:
                head.y--;
                break;

            case RIGHT:
                head.x++;
                break;

            case DOWN:
                head.y++;
                break;

            case LEFT:
                head.x--;
                break;
        }
    }

    private boolean detectDeath(){
        // Has the snake died?
        boolean dead = false;

        Point head = snakeBody.get(0);

        // Hit the screen edge
        if (head.x < 0) dead = true;
        if (head.x >= NUM_BLOCKS_WIDE) dead = true;
        if (head.y < 0) dead = true;
        if (head.y >= numBlocksHigh) dead = true;

        // Eaten itself?
        for (int i = snakeBody.size() - 1; i > 0; i--) {
            Point part = snakeBody.get(i);
            if (head.x == part.x && head.y == part.y) {
                dead = true;
            }
        }

        return dead;
    }

    public void draw() {
        // Get a lock on the canvas
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            // Fill the screen with Game Code School blue
            canvas.drawColor(Color.argb(255, 26, 128, 182));

            // Set the color of the paint to draw the snake white
            paint.setColor(Color.argb(255, 255, 255, 255));

            // Scale the HUD text relative to screen dimension
            paint.setTextSize(screenY / 15f);
            canvas.drawText("Score: " + score, 20, screenY / 10f, paint);

            // Draw the snake one block at a time
            for (Point p : snakeBody) {
                canvas.drawRect(p.x * blockSize,
                        (p.y * blockSize),
                        (p.x * blockSize) + blockSize,
                        (p.y * blockSize) + blockSize,
                        paint);
            }

            // Set the color of the paint to draw Bob red
            paint.setColor(Color.argb(255, 255, 0, 0));

            // Draw Bob
            canvas.drawRect(bobX * blockSize,
                    (bobY * blockSize),
                    (bobX * blockSize) + blockSize,
                    (bobY * blockSize) + blockSize,
                    paint);

            // Unlock the canvas and reveal the graphics for this frame
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // Forward touch events to the gesture detector
        if (gestureDetector.onTouchEvent(motionEvent)) {
            return true;
        }
        return super.onTouchEvent(motionEvent);
    }
}