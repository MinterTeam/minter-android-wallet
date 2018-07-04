package network.minter.bipwallet.internal.helpers.data;

import java.io.Serializable;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class Vec2 implements Serializable {
    public float x = 0.0f;
    public float y = 0.0f;
    public int width = 0;
    public int height = 0;

    public Vec2() {
    }

    public Vec2(float x, float y) {
        this(x, y, 0, 0);
    }

    public Vec2(int width, int height) {
        this(0.0f, 0.0f, width, height);
    }

    public Vec2(float x, float y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public Vec2 setX(float x) {
        this.x = x;
        return this;
    }

    public float getY() {
        return y;
    }

    public Vec2 setY(float y) {
        this.y = y;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public Vec2 setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public Vec2 setHeight(int height) {
        this.height = height;
        return this;
    }

    public float getLeft() {
        return getX();
    }

    public float getTop() {
        return getY();
    }

    public float getRight() {
        return getLeft() + getWidth();
    }

    public float getBottom() {
        return getTop() + getHeight();
    }
}
