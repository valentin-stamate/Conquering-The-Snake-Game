package board.snake;

import board.CellType;
import neural_network.NeuralNetwork;
import observer.Observer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Snake {

    private final List<Observer> snakeCollisionObserverList;

    private final int[] direction = new int[]{0, 1};

    private final List<SnakeCell> snakeCells;
    private final HashSet<SnakeCell> snakeCellsHashSet;

    private final int[][] boardMatrix;
    private final int rows;
    private final int columns;
    private final int width;
    private final int height;

    private boolean moveConsumed = true;
    private boolean snakeIncreaseConsumed = true;
    private boolean snakeFinished = false;

    private final int[] foodPosition;

    private int score;

    private final NeuralNetwork snakeBrain;

    public Snake(int[][] boardMatrix, int rows, int columns, int width, int height) {
        this.boardMatrix = boardMatrix;
        this.rows = rows;
        this.columns = columns;
        this.snakeCells = new ArrayList<>();
        this.snakeCellsHashSet = new HashSet<>();
        this.snakeCollisionObserverList = new ArrayList<>();
        this.score = 0;
        this.foodPosition = new int[2];
        this.snakeBrain = new NeuralNetwork(new int[]{24, 16, 16, 4});
        this.width = width;
        this.height = height;
        initialize();
    }

    private void initialize() {
        deleteSnake();

        int i = generateRandom(5, rows - 5);
        int j = generateRandom(5, columns - 5);

        int startingLength = 4;

        for (int l = 0; l < startingLength; l++) {
            SnakeCell lastSnakeCell = new SnakeCell(i, j);
            boardMatrix[lastSnakeCell.getI()][lastSnakeCell.getJ()] = CellType.SNAKE_CELL;

            List<SnakeCell> neighbours = getSnakeCellNeighbours(lastSnakeCell);
            snakeCells.add(lastSnakeCell);
            snakeCellsHashSet.add(lastSnakeCell);

            SnakeCell randomCell = neighbours.get(generateRandom(0, neighbours.size()));

            i = randomCell.getI();
            j = randomCell.getJ();

            if (l == 0) {
                direction[0] = lastSnakeCell.getI() - randomCell.getI();
                direction[1] = lastSnakeCell.getJ() - randomCell.getJ();
            }
        }

        moveConsumed = true;
        snakeIncreaseConsumed = true;
    }

    private List<SnakeCell> getSnakeCellNeighbours(SnakeCell snakeCell) {
        /* TODO handle snake intersection */
        List<SnakeCell> neighbours = new ArrayList<>();

        int i = snakeCell.getI();
        int j = snakeCell.getJ();

        if (i > 0) {
            if (boardMatrix[i - 1][j] == CellType.EMPTY_CELL) {
                neighbours.add(new SnakeCell(i - 1, j));
            }
        }

        if (j > 0) {
            if (boardMatrix[i][j - 1] == CellType.EMPTY_CELL) {
                neighbours.add(new SnakeCell(i, j - 1));
            }
        }

        if (i + 1 < rows) {
            if (boardMatrix[i + 1][j] == CellType.EMPTY_CELL) {
                neighbours.add(new SnakeCell(i + 1, j));
            }
        }

        if (j + 1 < columns) {
            if (boardMatrix[i][j + 1] == CellType.EMPTY_CELL) {
                neighbours.add(new SnakeCell(i, j + 1));
            }
        }

        return neighbours;
    }

    public void setFoodPosition(int i, int j) {
        foodPosition[0] = i;
        foodPosition[1] = j;
    }

    public void deleteSnake() {
        /* TODO handle snake intersection */
        snakeCells.forEach(snakeCell -> {
            int i = snakeCell.getI();
            int j = snakeCell.getJ();
            boardMatrix[i][j] = CellType.EMPTY_CELL;
        });

        snakeCells.clear();
    }

    public void setSnakeFinished() {
        snakeFinished = true;
    }

    public void onCrashListener(Observer observer) {
        snakeCollisionObserverList.add(observer);
    }

    public void makeStep() {
        if (snakeFinished) {
            return;
        }

        int newI = getNextI();
        int newJ = getNextJ();

        if (newI >= rows || newJ >= columns || newI < 0 || newJ < 0) {
            onSnakeCollide();
            return;
        }

        if (checkTailCollision(newI, newJ)) {
            onSnakeCollide();
            return;
        }

        SnakeCell head = snakeCells.get(0);
        boardMatrix[head.getI()][head.getJ()] = CellType.SNAKE_CELL;

        SnakeCell endTail = snakeCells.get(snakeCells.size() - 1);
        SnakeCell copyEndTail = endTail.getCopy();

        snakeCells.remove(endTail);
        snakeCellsHashSet.remove(endTail);

        endTail = new SnakeCell(newI, newJ);

        snakeCells.add(0, endTail);
        snakeCellsHashSet.add(endTail);

        boardMatrix[getHeadI()][getHeadJ()] = CellType.SNAKE_HEAD_CELL;

        if (!snakeIncreaseConsumed) {
            snakeCells.add(copyEndTail);
            snakeCellsHashSet.add(copyEndTail);
            snakeIncreaseConsumed = true;
        } else {
            boardMatrix[copyEndTail.getI()][copyEndTail.getJ()] = CellType.EMPTY_CELL;
        }

        moveConsumed = true;
        updateSnakeTail();

        /* Neural Network Prediction */
        predictNextMove();
    }

    private void updateSnakeTail() {
        for (int i = 0; i < snakeCells.size(); i++) {
            SnakeCell snakeCell = snakeCells.get(i);
            boardMatrix[snakeCell.getI()][snakeCell.getJ()] = i == 0 ? CellType.SNAKE_HEAD_CELL : CellType.SNAKE_CELL;
        }
    }

    private void onSnakeCollide() {
        snakeCollisionObserverList.forEach(Observer::update);
    }

    public boolean haveMoveConsumed() {
        return moveConsumed;
    }

    public void moveUp() {
        if (direction[0] == 1) {
            return;
        }

        direction[0] = -1;
        direction[1] = 0;
        moveConsumed = false;
    }

    public void moveRight() {
        if (direction[1] == -1) {
            return;
        }

        direction[0] = 0;
        direction[1] = 1;
        moveConsumed = false;
    }

    public void moveLeft() {
        if (direction[1] == 1) {
            return;
        }

        direction[0] = 0;
        direction[1] = -1;
        moveConsumed = false;
    }

    public void moveDown() {
        if (direction[0] == -1) {
            return;
        }

        direction[0] = 1;
        direction[1] = 0;
        moveConsumed = false;
    }

    public int getNextI() {
        return snakeCells.get(0).getI() + direction[0];
    }

    public int getNextJ() {
        return snakeCells.get(0).getJ() + direction[1];
    }

    public int getHeadI() {
        return snakeCells.get(0).getI();
    }

    public int getHeadJ() {
        return snakeCells.get(0).getJ();
    }

    public void setIncrease() {
        snakeIncreaseConsumed = false;
        score += 3;
    }

    public int getScore() {
        return score;
    }

    private int generateRandom(int start, int end) {
           return (((int)(Math.random() * 10000)) % (end - start)) + start;
    }

    public boolean isFinished() {
        return snakeFinished;
    }

    /* Neural Network */
    /** 8 directions and each one sees the distance between:
     * 1. wall
     * 2. tail
     * 3. food
     * 8 * 3 = 24 input size */
    private void predictNextMove() {
        double[] input = new double[24];

        int snakeI = getHeadI();
        int snakeJ = getHeadJ();

        getDistancePack(snakeI, snakeJ, 1, 0, input, 0);
        getDistancePack(snakeI, snakeJ, 1, 1, input, 3);
        getDistancePack(snakeI, snakeJ, 0, 1, input, 6);
        getDistancePack(snakeI, snakeJ, -1, 1, input, 9);
        getDistancePack(snakeI, snakeJ, -1, 0, input, 12);
        getDistancePack(snakeI, snakeJ, -1, -1, input, 15);
        getDistancePack(snakeI, snakeJ, 0, -1, input, 18);
        getDistancePack(snakeI, snakeJ, 1, -1, input, 21);

        System.out.println("Input: " + Arrays.toString(input));
        double[] output = snakeBrain.getOutput(input);

        if (output != null) {
            makeMoveFromOutput(output);
        } else {
            System.out.println("Output is null");
        }

    }

    private void makeMoveFromOutput(double[] output) {
        int n = output.length;

        int iMax = -1;
        double maxOutput = 0;

        for (int i = 0; i < n; i++) {
            if (output[i] > maxOutput) {
                iMax = i;
                maxOutput = output[i];
            }
        }

        System.out.println("Output: " + Arrays.toString(output));

        if (maxOutput > 0.6D) {
            System.out.println("Moving " + iMax);
            if (iMax == 0) {
                moveUp();
                System.out.println("UP");
            } else if (iMax == 1) {
                moveRight();
                System.out.println("Right");
            } else if (iMax == 2) {
                moveDown();
                System.out.println("Down");
            } else if (iMax == 3) {
                moveLeft();
                System.out.println("Left");
            }
        }
        System.out.println("");
    }

    private void getDistancePack(int headI, int headJ, int dirX, int dirY, double[] buffer, int offset) {

        boolean wallDistanceSet = false;
        boolean tailDistanceSet = false;
        boolean foodDistanceSet = false;

        for (int i = headI, j = headJ; (i >= 0 && i < rows) && (j >= 0 && j < columns); i += dirY, j += dirX) {
            if (!wallDistanceSet && (i == 0 || j == 0 || i == rows - 1 || j == columns - 1)) {
                buffer[offset] = distanceBetween(i, j, headI, headJ);
                wallDistanceSet = true;
            }

            if (!tailDistanceSet && checkTailCollision(i, j)) {
                buffer[offset + 1] = distanceBetween(i, j, headI, headJ);
                tailDistanceSet = true;
            }

            if (!foodDistanceSet && (dirX == 0 || dirY == 0) && foodPosition[0] == i && foodPosition[1] == j) {
                buffer[offset + 2] = distanceBetween(i, j, headI, headJ);
                foodDistanceSet = true;
            }
        }
    }

    private double distanceBetween(double aX, double aY, double bX, double bY) {
        double distanceX = Math.abs(aX - bX);
        double distanceY = Math.abs(aY - bY);

        /* Normalizing the distance to be between 0 - 1 */
        distanceX /= (1.0 * height);
        distanceY /= (1.0 * width);

        return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
    }

    private boolean checkTailCollision(int headI, int headJ) {
        return snakeCellsHashSet.contains(new SnakeCell(headI, headJ));
    }

}
