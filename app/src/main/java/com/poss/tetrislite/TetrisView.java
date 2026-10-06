package com.poss.tetrislite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

public class TetrisView extends View {
    private static final int COLS = 10;
    private static final int ROWS = 20;
    private static final int DRILL_CHANCE_PERCENT = 9;

    private static final int[][][][] SHAPES = {
            { // I
                    {{0,1},{1,1},{2,1},{3,1}},
                    {{2,0},{2,1},{2,2},{2,3}},
                    {{0,2},{1,2},{2,2},{3,2}},
                    {{1,0},{1,1},{1,2},{1,3}}
            },
            { // O
                    {{1,0},{2,0},{1,1},{2,1}},
                    {{1,0},{2,0},{1,1},{2,1}},
                    {{1,0},{2,0},{1,1},{2,1}},
                    {{1,0},{2,0},{1,1},{2,1}}
            },
            { // T
                    {{1,0},{0,1},{1,1},{2,1}},
                    {{1,0},{1,1},{2,1},{1,2}},
                    {{0,1},{1,1},{2,1},{1,2}},
                    {{1,0},{0,1},{1,1},{1,2}}
            },
            { // S
                    {{1,0},{2,0},{0,1},{1,1}},
                    {{1,0},{1,1},{2,1},{2,2}},
                    {{1,1},{2,1},{0,2},{1,2}},
                    {{0,0},{0,1},{1,1},{1,2}}
            },
            { // Z
                    {{0,0},{1,0},{1,1},{2,1}},
                    {{2,0},{1,1},{2,1},{1,2}},
                    {{0,1},{1,1},{1,2},{2,2}},
                    {{1,0},{0,1},{1,1},{0,2}}
            },
            { // J
                    {{0,0},{0,1},{1,1},{2,1}},
                    {{1,0},{2,0},{1,1},{1,2}},
                    {{0,1},{1,1},{2,1},{2,2}},
                    {{1,0},{1,1},{0,2},{1,2}}
            },
            { // L
                    {{2,0},{0,1},{1,1},{2,1}},
                    {{1,0},{1,1},{1,2},{2,2}},
                    {{0,1},{1,1},{2,1},{0,2}},
                    {{0,0},{1,0},{1,1},{1,2}}
            }
    };

    private static final int[] COLORS = {
            Color.rgb(73, 216, 230),  // I
            Color.rgb(245, 211, 67),  // O
            Color.rgb(181, 92, 230),  // T
            Color.rgb(97, 205, 92),   // S
            Color.rgb(239, 79, 79),   // Z
            Color.rgb(69, 115, 230),  // J
            Color.rgb(241, 145, 53)   // L
    };

    private final int[][] board = new int[ROWS][COLS];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int currentType;
    private int rotation;
    private int pieceX;
    private int pieceY;
    private boolean drillPiece;
    private boolean gameOver;

    private int score;
    private int lines;
    private int level;

    private float cellSize;
    private float boardLeft;
    private float boardTop;
    private float touchDownX;
    private float touchDownY;

    private final RectF leftButton = new RectF();
    private final RectF rotateButton = new RectF();
    private final RectF rightButton = new RectF();
    private final RectF dropButton = new RectF();

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (gameOver) {
                invalidate();
                return;
            }

            if (drillPiece) {
                stepDrill();
            } else if (canPlace(pieceX, pieceY + 1, rotation)) {
                pieceY++;
            } else {
                lockPiece();
            }

            invalidate();
            if (!gameOver) {
                handler.postDelayed(this, dropInterval());
            }
        }
    };

    public TetrisView(Context context) {
        super(context);
        setFocusable(true);
        startNewGame();
    }

    private void startNewGame() {
        handler.removeCallbacks(tick);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = 0;
            }
        }
        score = 0;
        lines = 0;
        level = 1;
        gameOver = false;
        spawnPiece();
        handler.postDelayed(tick, dropInterval());
        invalidate();
    }

    private void spawnPiece() {
        drillPiece = random.nextInt(100) < DRILL_CHANCE_PERCENT;
        rotation = 0;
        pieceX = drillPiece ? COLS / 2 : 3;
        pieceY = drillPiece ? 0 : -1;

        if (drillPiece) {
            currentType = 7;
            if (board[pieceY][pieceX] != 0) {
                board[pieceY][pieceX] = 0;
            }
        } else {
            currentType = random.nextInt(7);
            if (!canPlace(pieceX, pieceY, rotation)) {
                gameOver = true;
                handler.removeCallbacks(tick);
            }
        }
    }

    private long dropInterval() {
        return Math.max(110L, 700L - (long) (level - 1) * 55L);
    }

    private boolean canPlace(int x, int y, int rot) {
        if (drillPiece) {
            return x >= 0 && x < COLS;
        }

        int[][] blocks = SHAPES[currentType][rot];
        for (int[] block : blocks) {
            int bx = x + block[0];
            int by = y + block[1];
            if (bx < 0 || bx >= COLS || by >= ROWS) {
                return false;
            }
            if (by >= 0 && board[by][bx] != 0) {
                return false;
            }
        }
        return true;
    }

    private void moveHorizontal(int delta) {
        if (gameOver) return;
        if (drillPiece) {
            int nx = pieceX + delta;
            if (nx >= 0 && nx < COLS) {
                pieceX = nx;
                if (pieceY >= 0 && pieceY < ROWS && board[pieceY][pieceX] != 0) {
                    board[pieceY][pieceX] = 0;
                }
            }
        } else if (canPlace(pieceX + delta, pieceY, rotation)) {
            pieceX += delta;
        }
        invalidate();
    }

    private void rotatePiece() {
        if (gameOver || drillPiece) return;
        int next = (rotation + 1) % 4;
        if (canPlace(pieceX, pieceY, next)) {
            rotation = next;
        } else if (canPlace(pieceX - 1, pieceY, next)) {
            pieceX--;
            rotation = next;
        } else if (canPlace(pieceX + 1, pieceY, next)) {
            pieceX++;
            rotation = next;
        }
        invalidate();
    }

    private void hardDrop() {
        if (gameOver) return;

        if (drillPiece) {
            int start = Math.max(0, pieceY);
            for (int r = start; r < ROWS; r++) {
                board[r][pieceX] = 0;
            }
            score += 25;
            spawnPiece();
        } else {
            int distance = 0;
            while (canPlace(pieceX, pieceY + 1, rotation)) {
                pieceY++;
                distance++;
            }
            score += distance * 2;
            lockPiece();
        }
        resetTickTimer();
        invalidate();
    }

    private void stepDrill() {
        if (pieceY >= 0 && pieceY < ROWS) {
            board[pieceY][pieceX] = 0;
        }
        pieceY++;
        if (pieceY >= ROWS) {
            score += 25;
            spawnPiece();
        } else if (board[pieceY][pieceX] != 0) {
            board[pieceY][pieceX] = 0;
        }
    }

    private void lockPiece() {
        boolean aboveTop = false;
        int[][] blocks = SHAPES[currentType][rotation];
        for (int[] block : blocks) {
            int bx = pieceX + block[0];
            int by = pieceY + block[1];
            if (by < 0) {
                aboveTop = true;
            } else if (by < ROWS && bx >= 0 && bx < COLS) {
                board[by][bx] = currentType + 1;
            }
        }

        if (aboveTop) {
            gameOver = true;
            handler.removeCallbacks(tick);
            return;
        }

        int cleared = clearCompletedLines();
        if (cleared > 0) {
            lines += cleared;
            int base;
            switch (cleared) {
                case 1: base = 100; break;
                case 2: base = 300; break;
                case 3: base = 500; break;
                default: base = 800; break;
            }
            score += base * level;
            level = 1 + lines / 10;
        }
        spawnPiece();
    }

    private int clearCompletedLines() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cleared++;
                for (int rr = r; rr > 0; rr--) {
                    System.arraycopy(board[rr - 1], 0, board[rr], 0, COLS);
                }
                for (int c = 0; c < COLS; c++) {
                    board[0][c] = 0;
                }
                r++;
            }
        }
        return cleared;
    }

    private void resetTickTimer() {
        handler.removeCallbacks(tick);
        if (!gameOver) {
            handler.postDelayed(tick, dropInterval());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(16, 18, 24));

        float width = getWidth();
        float height = getHeight();
        float margin = dp(14);
        float header = dp(78);
        float controls = dp(104);
        float usableHeight = Math.max(dp(200), height - header - controls - margin * 2);
        cellSize = Math.min((width - margin * 2) / COLS, usableHeight / ROWS);
        boardLeft = (width - cellSize * COLS) / 2f;
        boardTop = header;

        drawHeader(canvas);
        drawBoard(canvas);
        drawCurrentPiece(canvas);
        drawControls(canvas, height);

        if (gameOver) {
            drawGameOver(canvas, width, height);
        }
    }

    private void drawHeader(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(dp(22));
        paint.setFakeBoldText(true);
        canvas.drawText("TETRIS LITE", dp(14), dp(31), paint);

        paint.setFakeBoldText(false);
        paint.setTextSize(dp(14));
        paint.setColor(Color.rgb(190, 197, 214));
        canvas.drawText("PUNTI " + score, dp(14), dp(58), paint);

        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LINEE " + lines, getWidth() / 2f, dp(58), paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("LIV " + level, getWidth() - dp(14), dp(58), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawBoard(Canvas canvas) {
        float boardRight = boardLeft + COLS * cellSize;
        float boardBottom = boardTop + ROWS * cellSize;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(28, 31, 41));
        canvas.drawRect(boardLeft, boardTop, boardRight, boardBottom, paint);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int value = board[r][c];
                if (value != 0) {
                    drawCell(canvas, c, r, COLORS[value - 1]);
                }
            }
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, dp(0.6f)));
        paint.setColor(Color.rgb(48, 52, 66));
        for (int c = 0; c <= COLS; c++) {
            float x = boardLeft + c * cellSize;
            canvas.drawLine(x, boardTop, x, boardBottom, paint);
        }
        for (int r = 0; r <= ROWS; r++) {
            float y = boardTop + r * cellSize;
            canvas.drawLine(boardLeft, y, boardRight, y, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCurrentPiece(Canvas canvas) {
        if (gameOver) return;

        if (drillPiece) {
            if (pieceY >= 0 && pieceY < ROWS) {
                drawCell(canvas, pieceX, pieceY, Color.rgb(255, 213, 74));
                paint.setColor(Color.rgb(35, 35, 35));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(cellSize * 0.65f);
                paint.setFakeBoldText(true);
                float cx = boardLeft + (pieceX + 0.5f) * cellSize;
                float cy = boardTop + (pieceY + 0.72f) * cellSize;
                canvas.drawText("▼", cx, cy, paint);
                paint.setFakeBoldText(false);
                paint.setTextAlign(Paint.Align.LEFT);
            }
            return;
        }

        for (int[] block : SHAPES[currentType][rotation]) {
            int x = pieceX + block[0];
            int y = pieceY + block[1];
            if (y >= 0) {
                drawCell(canvas, x, y, COLORS[currentType]);
            }
        }
    }

    private void drawCell(Canvas canvas, int col, int row, int color) {
        float inset = Math.max(1f, cellSize * 0.055f);
        float l = boardLeft + col * cellSize + inset;
        float t = boardTop + row * cellSize + inset;
        float r = boardLeft + (col + 1) * cellSize - inset;
        float b = boardTop + (row + 1) * cellSize - inset;

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(l, t, r, b), cellSize * 0.12f, cellSize * 0.12f, paint);

        paint.setColor(Color.argb(55, 255, 255, 255));
        canvas.drawRect(l + inset, t + inset, r - inset, t + cellSize * 0.16f, paint);
    }

    private void drawControls(Canvas canvas, float height) {
        float gap = dp(8);
        float side = dp(58);
        float dropWidth = Math.max(dp(82), getWidth() - dp(28) - side * 3 - gap * 3);
        float total = side * 3 + dropWidth + gap * 3;
        float startX = (getWidth() - total) / 2f;
        float top = Math.min(height - dp(78), boardTop + ROWS * cellSize + dp(18));
        float bottom = top + dp(58);

        leftButton.set(startX, top, startX + side, bottom);
        rotateButton.set(leftButton.right + gap, top, leftButton.right + gap + side, bottom);
        rightButton.set(rotateButton.right + gap, top, rotateButton.right + gap + side, bottom);
        dropButton.set(rightButton.right + gap, top, rightButton.right + gap + dropWidth, bottom);

        drawButton(canvas, leftButton, "←");
        drawButton(canvas, rotateButton, "↻");
        drawButton(canvas, rightButton, "→");
        drawButton(canvas, dropButton, "DROP");

        if (drillPiece && !gameOver) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.rgb(255, 213, 74));
            paint.setTextSize(dp(12));
            paint.setFakeBoldText(true);
            canvas.drawText("PERFORATORE", getWidth() / 2f, top - dp(6), paint);
            paint.setFakeBoldText(false);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawButton(Canvas canvas, RectF rect, String label) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(42, 47, 61));
        canvas.drawRoundRect(rect, dp(12), dp(12), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.rgb(91, 102, 128));
        canvas.drawRoundRect(rect, dp(12), dp(12), paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(label.equals("DROP") ? dp(14) : dp(28));
        paint.setFakeBoldText(true);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float baseline = rect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(label, rect.centerX(), baseline, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGameOver(Canvas canvas, float width, float height) {
        paint.setColor(Color.argb(210, 8, 10, 14));
        canvas.drawRect(0, 0, width, height, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);
        paint.setTextSize(dp(34));
        canvas.drawText("GAME OVER", width / 2f, height / 2f - dp(18), paint);

        paint.setFakeBoldText(false);
        paint.setTextSize(dp(17));
        paint.setColor(Color.rgb(205, 211, 226));
        canvas.drawText("Punti: " + score + "   Linee: " + lines, width / 2f, height / 2f + dp(18), paint);
        paint.setTextSize(dp(15));
        canvas.drawText("Tocca per ricominciare", width / 2f, height / 2f + dp(52), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                float x = event.getX();
                float y = event.getY();

                if (gameOver) {
                    startNewGame();
                    return true;
                }

                if (leftButton.contains(x, y)) {
                    moveHorizontal(-1);
                    return true;
                }
                if (rotateButton.contains(x, y)) {
                    rotatePiece();
                    return true;
                }
                if (rightButton.contains(x, y)) {
                    moveHorizontal(1);
                    return true;
                }
                if (dropButton.contains(x, y)) {
                    hardDrop();
                    return true;
                }

                float dx = x - touchDownX;
                float dy = y - touchDownY;
                float threshold = dp(34);

                if (Math.abs(dx) > threshold && Math.abs(dx) > Math.abs(dy)) {
                    moveHorizontal(dx > 0 ? 1 : -1);
                } else if (dy > threshold) {
                    hardDrop();
                } else if (dy < -threshold) {
                    rotatePiece();
                } else {
                    rotatePiece();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }
}
