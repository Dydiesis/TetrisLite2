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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TetrisView extends View {
    private static final int COLS = 10;
    private static final int ROWS = 20;

    private static final int SPECIAL_NORMAL = 0;
    private static final int SPECIAL_DRILL = 1;
    private static final int SPECIAL_BOMB = 2;
    private static final int SPECIAL_COLOR_BOMB = 3;
    private static final int SPECIAL_ATOMIC = 4;

    private static final int DRILL_CHANCE_PERCENT = 9;
    private static final int BOMB_CHANCE_PERCENT = 6;
    private static final int COLOR_BOMB_CHANCE_PERCENT = 6;
    private static final int ATOMIC_CHANCE_PERCENT = 5;

    private static final int[][][][] SHAPES = {
            {
                    {{0,1},{1,1},{2,1},{3,1}},
                    {{2,0},{2,1},{2,2},{2,3}},
                    {{0,2},{1,2},{2,2},{3,2}},
                    {{1,0},{1,1},{1,2},{1,3}}
            },
            {
                    {{1,0},{2,0},{1,1},{2,1}},
                    {{1,0},{2,0},{1,1},{2,1}},
                    {{1,0},{2,0},{1,1},{2,1}},
                    {{1,0},{2,0},{1,1},{2,1}}
            },
            {
                    {{1,0},{0,1},{1,1},{2,1}},
                    {{1,0},{1,1},{2,1},{1,2}},
                    {{0,1},{1,1},{2,1},{1,2}},
                    {{1,0},{0,1},{1,1},{1,2}}
            },
            {
                    {{1,0},{2,0},{0,1},{1,1}},
                    {{1,0},{1,1},{2,1},{2,2}},
                    {{1,1},{2,1},{0,2},{1,2}},
                    {{0,0},{0,1},{1,1},{1,2}}
            },
            {
                    {{0,0},{1,0},{1,1},{2,1}},
                    {{2,0},{1,1},{2,1},{1,2}},
                    {{0,1},{1,1},{1,2},{2,2}},
                    {{1,0},{0,1},{1,1},{0,2}}
            },
            {
                    {{0,0},{0,1},{1,1},{2,1}},
                    {{1,0},{2,0},{1,1},{1,2}},
                    {{0,1},{1,1},{2,1},{2,2}},
                    {{1,0},{1,1},{0,2},{1,2}}
            },
            {
                    {{2,0},{0,1},{1,1},{2,1}},
                    {{1,0},{1,1},{1,2},{2,2}},
                    {{0,1},{1,1},{2,1},{0,2}},
                    {{0,0},{1,0},{1,1},{1,2}}
            }
    };

    private static final int[] COLORS = {
            Color.rgb(54, 220, 238),
            Color.rgb(255, 213, 55),
            Color.rgb(181, 82, 235),
            Color.rgb(82, 211, 88),
            Color.rgb(244, 70, 70),
            Color.rgb(61, 111, 239),
            Color.rgb(255, 145, 43)
    };

    private static final int DRILL_COLOR = Color.rgb(255, 221, 71);
    private static final int BOMB_COLOR = Color.rgb(255, 67, 54);
    private static final int COLOR_BOMB_COLOR = Color.rgb(115, 210, 255);
    private static final int ATOMIC_COLOR = Color.rgb(104, 255, 128);

    // board = colore 1..7; pieceIds = identita del tetramino classico.
    private final int[][] board = new int[ROWS][COLS];
    private final int[][] pieceIds = new int[ROWS][COLS];

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int currentType;
    private int rotation;
    private int pieceX;
    private int pieceY;
    private int specialKind;
    private boolean gameOver;

    private boolean drillHasHit;
    private int nextPieceId = 1;

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

            if (specialKind == SPECIAL_DRILL) {
                stepDrill();
            } else if (specialKind == SPECIAL_BOMB) {
                stepBottomBomb(false);
            } else if (specialKind == SPECIAL_COLOR_BOMB) {
                stepBottomBomb(true);
            } else if (specialKind == SPECIAL_ATOMIC) {
                stepAtomic();
            } else if (canPlaceNormal(pieceX, pieceY + 1, rotation)) {
                pieceY++;
            } else {
                lockPiece();
            }

            invalidate();
            if (!gameOver) handler.postDelayed(this, dropInterval());
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
                pieceIds[r][c] = 0;
            }
        }
        nextPieceId = 1;
        score = 0;
        lines = 0;
        level = 1;
        gameOver = false;
        spawnPiece();
        handler.postDelayed(tick, dropInterval());
        invalidate();
    }

    private void spawnPiece() {
        int roll = random.nextInt(100);
        int limitDrill = DRILL_CHANCE_PERCENT;
        int limitBomb = limitDrill + BOMB_CHANCE_PERCENT;
        int limitColor = limitBomb + COLOR_BOMB_CHANCE_PERCENT;
        int limitAtomic = limitColor + ATOMIC_CHANCE_PERCENT;

        if (roll < limitDrill) specialKind = SPECIAL_DRILL;
        else if (roll < limitBomb) specialKind = SPECIAL_BOMB;
        else if (roll < limitColor) specialKind = SPECIAL_COLOR_BOMB;
        else if (roll < limitAtomic) specialKind = SPECIAL_ATOMIC;
        else specialKind = SPECIAL_NORMAL;

        rotation = 0;
        drillHasHit = false;

        if (specialKind != SPECIAL_NORMAL) {
            currentType = 0;
            pieceX = COLS / 2;
            pieceY = -1;
            return;
        }

        currentType = random.nextInt(7);
        pieceX = 3;
        pieceY = -1;
        if (!canPlaceNormal(pieceX, pieceY, rotation)) {
            gameOver = true;
            handler.removeCallbacks(tick);
        }
    }

    private long dropInterval() {
        return Math.max(110L, 700L - (long) (level - 1) * 55L);
    }

    private boolean canPlaceNormal(int x, int y, int rot) {
        for (int[] block : SHAPES[currentType][rot]) {
            int bx = x + block[0];
            int by = y + block[1];
            if (bx < 0 || bx >= COLS || by >= ROWS) return false;
            if (by >= 0 && board[by][bx] != 0) return false;
        }
        return true;
    }

    private boolean specialDestinationFree(int x, int y) {
        if (x < 0 || x >= COLS || y >= ROWS) return false;
        return y < 0 || board[y][x] == 0;
    }

    private void moveHorizontal(int delta) {
        if (gameOver) return;

        if (specialKind == SPECIAL_NORMAL) {
            if (canPlaceNormal(pieceX + delta, pieceY, rotation)) pieceX += delta;
        } else if (specialKind == SPECIAL_DRILL) {
            // Il perforatore e controllabile solo prima del primo contatto.
            if (!drillHasHit) {
                int nx = pieceX + delta;
                if (nx >= 0 && nx < COLS) {
                    pieceX = nx;
                    if (pieceY >= 0 && board[pieceY][pieceX] != 0) {
                        clearBoardCell(pieceY, pieceX);
                        drillHasHit = true;
                    }
                }
            }
        } else if (specialKind == SPECIAL_ATOMIC) {
            // ATOMIC resta sempre sulla sua colonna: perforazione perpendicolare pura.
        } else {
            // BOMBA e BOMBA COLORE possono scorrere accanto ai blocchi senza esplodere.
            int nx = pieceX + delta;
            if (specialDestinationFree(nx, pieceY)) pieceX = nx;
        }
        invalidate();
    }

    private void rotatePiece() {
        if (gameOver || specialKind != SPECIAL_NORMAL) return;
        int next = (rotation + 1) % 4;
        if (canPlaceNormal(pieceX, pieceY, next)) {
            rotation = next;
        } else if (canPlaceNormal(pieceX - 1, pieceY, next)) {
            pieceX--;
            rotation = next;
        } else if (canPlaceNormal(pieceX + 1, pieceY, next)) {
            pieceX++;
            rotation = next;
        }
        invalidate();
    }

    private void hardDrop() {
        if (gameOver) return;

        if (specialKind == SPECIAL_NORMAL) {
            int distance = 0;
            while (canPlaceNormal(pieceX, pieceY + 1, rotation)) {
                pieceY++;
                distance++;
            }
            score += distance * 2;
            lockPiece();
        } else if (specialKind == SPECIAL_DRILL) {
            int safety = ROWS * COLS * 10;
            while (specialKind == SPECIAL_DRILL && safety-- > 0) {
                if (advanceDrillOneStep()) break;
            }
        } else if (specialKind == SPECIAL_BOMB) {
            hardDropBottomBomb(false);
        } else if (specialKind == SPECIAL_COLOR_BOMB) {
            hardDropBottomBomb(true);
        } else if (specialKind == SPECIAL_ATOMIC) {
            while (specialKind == SPECIAL_ATOMIC) {
                if (advanceAtomicOneStep()) break;
            }
        }

        resetTickTimer();
        invalidate();
    }

    // ---------------- PERFORATORE ----------------

    private void stepDrill() {
        advanceDrillOneStep();
    }

    // Dopo il primo impatto, OGNI passo sceglie casualmente GIU, SINISTRA o DESTRA.
    private boolean advanceDrillOneStep() {
        if (pieceY >= 0 && pieceY < ROWS && board[pieceY][pieceX] != 0) {
            clearBoardCell(pieceY, pieceX);
            drillHasHit = true;
        }

        int nx = pieceX;
        int ny = pieceY;

        if (!drillHasHit) {
            ny = pieceY + 1;
        } else {
            int direction = random.nextInt(3); // 0=giu, 1=sinistra, 2=destra
            if (direction == 0) {
                ny = pieceY + 1;
            } else if (direction == 1) {
                nx = pieceX - 1;
                if (nx < 0) {
                    nx = pieceX;
                    ny = pieceY + 1;
                }
            } else {
                nx = pieceX + 1;
                if (nx >= COLS) {
                    nx = pieceX;
                    ny = pieceY + 1;
                }
            }
        }

        if (ny >= ROWS) {
            score += 25;
            spawnPiece();
            return true;
        }

        pieceX = nx;
        pieceY = ny;

        if (pieceY >= 0 && board[pieceY][pieceX] != 0) {
            clearBoardCell(pieceY, pieceX);
            drillHasHit = true;
        }
        return false;
    }

    // ---------------- BOMBA / BOMBA COLORE ----------------

    private void stepBottomBomb(boolean colorBomb) {
        // Unica condizione di innesco: cella OCCUPATA immediatamente sotto.
        int[] contact = findBottomContact();
        if (contact != null) {
            if (colorBomb) explodeColor6x6(contact[0], contact[1]);
            else explodeArea3x3(contact[0], contact[1]);
            spawnPiece();
            return;
        }

        int nextY = pieceY + 1;
        if (nextY >= ROWS) {
            score += 5;
            spawnPiece();
            return;
        }

        // Se sotto e libero, scende. I blocchi laterali sono completamente ignorati.
        if (nextY < 0 || board[nextY][pieceX] == 0) {
            pieceY = nextY;
        } else {
            // Caso teoricamente equivalente al contatto sotto: gestito qui per sicurezza.
            if (colorBomb) explodeColor6x6(pieceX, nextY);
            else explodeArea3x3(pieceX, nextY);
            spawnPiece();
        }
    }

    private void hardDropBottomBomb(boolean colorBomb) {
        while (specialKind == (colorBomb ? SPECIAL_COLOR_BOMB : SPECIAL_BOMB)) {
            int[] contact = findBottomContact();
            if (contact != null) {
                if (colorBomb) explodeColor6x6(contact[0], contact[1]);
                else explodeArea3x3(contact[0], contact[1]);
                spawnPiece();
                return;
            }

            int nextY = pieceY + 1;
            if (nextY >= ROWS) {
                score += 5;
                spawnPiece();
                return;
            }
            pieceY = nextY;
        }
    }

    private int[] findBottomContact() {
        int ny = pieceY + 1;
        if (pieceX >= 0 && pieceX < COLS && ny >= 0 && ny < ROWS && board[ny][pieceX] != 0) {
            return new int[]{pieceX, ny};
        }
        return null;
    }

    private void explodeArea3x3(int centerCol, int centerRow) {
        int removed = 0;
        for (int r = centerRow - 1; r <= centerRow + 1; r++) {
            for (int c = centerCol - 1; c <= centerCol + 1; c++) {
                if (inside(c, r) && board[r][c] != 0) {
                    clearBoardCell(r, c);
                    removed++;
                }
            }
        }
        score += 40 + removed * 12;
    }

    private void explodeColor6x6(int contactCol, int contactRow) {
        int targetColor = board[contactRow][contactCol];
        if (targetColor == 0) return;

        int startCol = clampWindowStart(contactCol - 2, COLS, 6);
        int startRow = clampWindowStart(contactRow - 2, ROWS, 6);
        int endCol = startCol + 5;
        int endRow = startRow + 5;

        Set<Integer> idsToRemove = new HashSet<>();
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (board[r][c] == targetColor && pieceIds[r][c] != 0) {
                    idsToRemove.add(pieceIds[r][c]);
                }
            }
        }

        int removedCells = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (idsToRemove.contains(pieceIds[r][c])) {
                    clearBoardCell(r, c);
                    removedCells++;
                }
            }
        }

        applyRigidTetrominoGravity();
        score += 60 + removedCells * 15;
        processCompletedLines();
    }

    // ---------------- ATOMIC ----------------

    private void stepAtomic() {
        advanceAtomicOneStep();
    }

    // ATOMIC resta sulla stessa colonna, cancella tutto sul percorso e scoppia solo alla base.
    private boolean advanceAtomicOneStep() {
        if (pieceY >= 0 && pieceY < ROWS && board[pieceY][pieceX] != 0) {
            clearBoardCell(pieceY, pieceX);
        }

        int nextY = pieceY + 1;
        if (nextY >= ROWS) {
            explodeAtomic6x6(pieceX);
            score += 100;
            spawnPiece();
            return true;
        }

        pieceY = nextY;
        if (board[pieceY][pieceX] != 0) clearBoardCell(pieceY, pieceX);
        return false;
    }

    private void explodeAtomic6x6(int centerCol) {
        int startCol = clampWindowStart(centerCol - 2, COLS, 6);
        int startRow = ROWS - 6;
        int removed = 0;

        for (int r = startRow; r < ROWS; r++) {
            for (int c = startCol; c < startCol + 6; c++) {
                if (board[r][c] != 0) {
                    clearBoardCell(r, c);
                    removed++;
                }
            }
        }
        score += removed * 12;
    }

    private int clampWindowStart(int desired, int total, int window) {
        return Math.max(0, Math.min(total - window, desired));
    }

    private boolean inside(int col, int row) {
        return col >= 0 && col < COLS && row >= 0 && row < ROWS;
    }

    // ---------------- GRAVITA A TETRAMINI INTERI ----------------

    private void applyRigidTetrominoGravity() {
        int safety = ROWS * ROWS;
        boolean moved;
        do {
            moved = false;
            ArrayList<Integer> ids = collectPieceIds();
            Collections.sort(ids, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Integer.compare(maxRowForPiece(b), maxRowForPiece(a));
                }
            });

            for (int id : ids) {
                if (canPieceMoveDown(id)) {
                    movePieceDownOne(id);
                    moved = true;
                }
            }
            safety--;
        } while (moved && safety > 0);
    }

    private ArrayList<Integer> collectPieceIds() {
        Set<Integer> unique = new HashSet<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (pieceIds[r][c] != 0) unique.add(pieceIds[r][c]);
            }
        }
        return new ArrayList<>(unique);
    }

    private int maxRowForPiece(int id) {
        int max = -1;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (pieceIds[r][c] == id) max = Math.max(max, r);
            }
        }
        return max;
    }

    private boolean canPieceMoveDown(int id) {
        boolean found = false;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (pieceIds[r][c] != id) continue;
                found = true;
                int nr = r + 1;
                if (nr >= ROWS) return false;
                if (board[nr][c] != 0 && pieceIds[nr][c] != id) return false;
            }
        }
        return found;
    }

    private void movePieceDownOne(int id) {
        for (int r = ROWS - 1; r >= 0; r--) {
            for (int c = 0; c < COLS; c++) {
                if (pieceIds[r][c] == id) {
                    int color = board[r][c];
                    board[r][c] = 0;
                    pieceIds[r][c] = 0;
                    board[r + 1][c] = color;
                    pieceIds[r + 1][c] = id;
                }
            }
        }
    }

    private void clearBoardCell(int row, int col) {
        board[row][col] = 0;
        pieceIds[row][col] = 0;
    }

    // ---------------- TETRIS CLASSICO ----------------

    private void lockPiece() {
        boolean aboveTop = false;
        int id = nextPieceId++;

        for (int[] block : SHAPES[currentType][rotation]) {
            int bx = pieceX + block[0];
            int by = pieceY + block[1];
            if (by < 0) {
                aboveTop = true;
            } else if (inside(bx, by)) {
                board[by][bx] = currentType + 1;
                pieceIds[by][bx] = id;
            }
        }

        if (aboveTop) {
            gameOver = true;
            handler.removeCallbacks(tick);
            return;
        }

        processCompletedLines();
        spawnPiece();
    }

    private void processCompletedLines() {
        int cleared = clearCompletedLines();
        if (cleared <= 0) return;

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
                    System.arraycopy(pieceIds[rr - 1], 0, pieceIds[rr], 0, COLS);
                }
                for (int c = 0; c < COLS; c++) {
                    board[0][c] = 0;
                    pieceIds[0][c] = 0;
                }
                r++;
            }
        }
        return cleared;
    }

    private void resetTickTimer() {
        handler.removeCallbacks(tick);
        if (!gameOver) handler.postDelayed(tick, dropInterval());
    }

    // ---------------- DISEGNO ----------------

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

        float originalCellSize = Math.min((width - margin * 2) / COLS, usableHeight / ROWS);
        cellSize = originalCellSize * 0.90f;
        boardLeft = (width - cellSize * COLS) / 2f;
        boardTop = header;

        drawHeader(canvas);
        drawBoard(canvas);
        drawCurrentPiece(canvas);
        drawControls(canvas, height);

        if (gameOver) drawGameOver(canvas, width, height);
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
                if (value != 0) drawCell(canvas, c, r, COLORS[value - 1]);
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

        if (specialKind == SPECIAL_NORMAL) {
            for (int[] block : SHAPES[currentType][rotation]) {
                int x = pieceX + block[0];
                int y = pieceY + block[1];
                if (y >= 0) drawCell(canvas, x, y, COLORS[currentType]);
            }
            return;
        }

        if (pieceY < 0 || pieceY >= ROWS) return;

        if (specialKind == SPECIAL_DRILL) {
            drawCell(canvas, pieceX, pieceY, DRILL_COLOR);
            drawSymbol(canvas, pieceX, pieceY, drillHasHit ? "?" : "V", Color.rgb(35, 35, 35));
        } else if (specialKind == SPECIAL_BOMB) {
            drawCell(canvas, pieceX, pieceY, BOMB_COLOR);
            drawSymbol(canvas, pieceX, pieceY, "B", Color.WHITE);
        } else if (specialKind == SPECIAL_COLOR_BOMB) {
            drawCell(canvas, pieceX, pieceY, COLOR_BOMB_COLOR);
            drawSymbol(canvas, pieceX, pieceY, "C", Color.WHITE);
        } else if (specialKind == SPECIAL_ATOMIC) {
            drawCell(canvas, pieceX, pieceY, ATOMIC_COLOR);
            drawSymbol(canvas, pieceX, pieceY, "A", Color.rgb(20, 35, 20));
        }
    }

    private void drawSymbol(Canvas canvas, int col, int row, String symbol, int color) {
        float cx = boardLeft + (col + 0.5f) * cellSize;
        float cy = boardTop + (row + 0.69f) * cellSize;
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(cellSize * 0.48f);
        paint.setFakeBoldText(true);
        canvas.drawText(symbol, cx, cy, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
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

        paint.setColor(Color.argb(70, 255, 255, 255));
        canvas.drawRect(l + inset, t + inset, r - inset, t + cellSize * 0.16f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, cellSize * 0.035f));
        paint.setColor(Color.argb(70, 0, 0, 0));
        canvas.drawRoundRect(new RectF(l, t, r, b), cellSize * 0.12f, cellSize * 0.12f, paint);
        paint.setStyle(Paint.Style.FILL);
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

        if (!gameOver && specialKind != SPECIAL_NORMAL) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(12));
            paint.setFakeBoldText(true);

            if (specialKind == SPECIAL_DRILL) {
                paint.setColor(DRILL_COLOR);
                canvas.drawText("PERFORATORE 1x1 - MOVIMENTO CASUALE", getWidth() / 2f, top - dp(6), paint);
            } else if (specialKind == SPECIAL_BOMB) {
                paint.setColor(BOMB_COLOR);
                canvas.drawText("BOMBA 1x1 - CONTATTO SOLO SOTTO", getWidth() / 2f, top - dp(6), paint);
            } else if (specialKind == SPECIAL_COLOR_BOMB) {
                paint.setColor(COLOR_BOMB_COLOR);
                canvas.drawText("BOMBA COLORE 1x1 - AREA 6x6", getWidth() / 2f, top - dp(6), paint);
            } else {
                paint.setColor(ATOMIC_COLOR);
                canvas.drawText("ATOMIC 1x1 - PERFORA + ESPLOSIONE 6x6", getWidth() / 2f, top - dp(6), paint);
            }

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
                } else if (specialKind == SPECIAL_NORMAL) {
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
