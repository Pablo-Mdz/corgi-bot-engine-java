package com.corgibot.example;

import com.corgibot.engine.audio.Speaker;
import com.corgibot.engine.control.Keyboard;
import com.corgibot.engine.game.Frame;
import com.corgibot.engine.game.Game;
import com.corgibot.engine.game.GameConfig;
import com.corgibot.engine.game.Position;
import com.corgibot.utils.random.Random;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Queue;

public class Knyacki2 {
    private static Position position;
    private static Direction direction;
    private static Queue<Position> body;
    private static int endCounter;
    private static int score;

    private static boolean isGameOver = true;
    private static final Game game = new Game(new GameConfig(16, 100, Color.black));
    private static final Field[][] world = new Field[Game.config.getWidth()][Game.config.getHeight()];

    private static boolean newGamePressed;

    private enum Field {
        HEAD, BODY, ITEM, WALL
    }

    private enum Direction {
        RIGHT, LEFT, UP, DOWN
    }

    public static void main(String[] args) {
        Keyboard.onKey(KeyEvent.VK_LEFT, Knyacki2::turnLeft);
        Keyboard.onKey(KeyEvent.VK_RIGHT, Knyacki2::turnRight);
        Keyboard.onKey(KeyEvent.VK_SPACE, Knyacki2::onNewGameKey);

        game.onFrame(Knyacki2::onFrame);
        game.start();
    }

    private static void onNewGameKey() {
        newGamePressed = true;
    }

    private static void onFrame(Frame frame) {
        Speaker.loop("Knyacki/Music");
        if (isGameOver) {
            if (newGamePressed) {
                newGame(frame);
            }
        } else {
            move(frame);
            placeItem(frame);
            if (score % 50 == 0) {
                expandWall(frame);
            }
            score++;
        }

        frame.drawHead("Score:" + score);
    }

    private static void turnLeft() {
        switch (direction) {
            case UP:
                direction = Direction.LEFT;
                break;
            case RIGHT:
                direction = Direction.UP;
                break;
            case DOWN:
                direction = Direction.RIGHT;
                break;
            case LEFT:
                direction = Direction.DOWN;
                break;
        }
    }

    private static void turnRight() {
        switch (direction) {
            case UP:
                direction = Direction.RIGHT;
                break;
            case RIGHT:
                direction = Direction.DOWN;
                break;
            case DOWN:
                direction = Direction.LEFT;
                break;
            case LEFT:
                direction = Direction.UP;
                break;
        }
    }

    private static void newGame(Frame frame) {
        Speaker.play("Knyacki/NewGame");

        resetWorld(frame);

        body = new ArrayDeque<>();
        endCounter = 0;
        direction = Direction.UP;
        position = new Position(Game.config.getWidth() / 2, Game.config.getHeight() / 2);
        score = 0;
        isGameOver = false;
        newGamePressed = false;
    }

    private static void move(Frame frame) {
        int newPosX = position.x;
        int newPosY = position.y;

        switch (direction) {
            case UP:
                newPosY--;
                break;
            case RIGHT:
                newPosX++;
                break;
            case DOWN:
                newPosY++;
                break;
            case LEFT:
                newPosX--;
                break;
        }

        if ((newPosX < 0 || newPosX >= Game.config.getWidth() || newPosY < 0 || newPosY >= Game.config.getHeight()) ||
                world[newPosX][newPosY] == Field.BODY || world[newPosX][newPosY] == Field.WALL) {
            if (score % 10 == 0) {
                endCounter++;
                switch (endCounter) {
                    case 1:
                        Speaker.play("Knyacki/Ichi", 6);
                        break;
                    case 2:
                        Speaker.play("Knyacki/Ni", 6);
                        break;
                    case 3:
                        Speaker.play("Knyacki/San", 6);
                        break;
                    case 4:
                        Speaker.play("Knyacki/GameOver", 6);
                        isGameOver = true;

                }
            }

            return;
        } else {
            endCounter = 0;
        }

        if (world[newPosX][newPosY] == Field.ITEM) {
            for (int counter = 0; counter < 10 && body.size() > 0; counter++) {
                Position tail = body.poll();
                if (world[tail.x][tail.y] == Field.BODY) {
                    frame.erase(tail);
                }
                world[tail.x][tail.y] = null;
            }

            Speaker.play("Knyacki/KnyackiChan");
        }

        if ((world[newPosX][newPosY] == null || world[newPosX][newPosY] == Field.ITEM)) {
            frame.erase(position);
            frame.drawImage(position, "Knyacki/Körper");
            world[position.x][position.y] = Field.BODY;
            body.add(new Position(position.x, position.y));

            position.x = newPosX;
            position.y = newPosY;
            frame.drawImage(position, "Knyacki/Kopf");
            world[position.x][position.y] = Field.HEAD;
        }
    }

    private static void placeItem(Frame frame) {
        int x = Random.number(0, Game.config.getWidth() - 1);
        int y = Random.number(0, Game.config.getHeight() - 1);
        if (world[x][y] == null) {
            frame.drawImage(new Position(x, y), "Knyacki/Item");
            world[x][y] = Field.ITEM;
        }
    }

    private static void resetWorld(Frame frame) {
        for (int columnCounter = 0; columnCounter < Game.config.getWidth(); columnCounter++) {
            for (int rowCounter = 0; rowCounter < Game.config.getHeight(); rowCounter++) {
                if (columnCounter == 0 || rowCounter == 0 || columnCounter == Game.config.getWidth() - 1 || rowCounter == Game.config.getHeight() - 1) {
                    frame.drawImage(new Position(columnCounter, rowCounter), "Knyacki/Wand");
                    world[columnCounter][rowCounter] = Field.WALL;
                } else {
                    frame.erase(new Position(columnCounter, rowCounter));
                    world[columnCounter][rowCounter] = null;
                }
            }
        }
    }

    private static void expandWall(Frame frame) {
        int verticalWallThickness = (score / 50) + 1;
        int horizontalWallThickness = (int) (((double) Game.config.getWidth() / (double) Game.config.getHeight()) * verticalWallThickness + 1);
        for (int columnCounter = 0; columnCounter < Game.config.getWidth(); columnCounter++) {
            for (int rowCounter = 0; rowCounter < Game.config.getHeight(); rowCounter++) {
                if (columnCounter < horizontalWallThickness || rowCounter < verticalWallThickness || columnCounter > Game.config.getWidth() - horizontalWallThickness - 1 || rowCounter > Game.config.getHeight() - verticalWallThickness - 1) {
                    if (world[columnCounter][rowCounter] == Field.HEAD) {
                        Speaker.play("Knyacki/Aitz");
                        Speaker.play("Knyacki/GameOver", 6);
                        isGameOver = true;
                    }
                    if (world[columnCounter][rowCounter] != Field.WALL) {
                        world[columnCounter][rowCounter] = Field.WALL;
                        frame.drawImage(new Position(columnCounter, rowCounter), "Knyacki/Wand");
                    }
                }
            }
        }
    }
}
