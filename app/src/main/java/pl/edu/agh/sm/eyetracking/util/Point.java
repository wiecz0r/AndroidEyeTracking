package pl.edu.agh.sm.eyetracking.util;

public class Point {
    public final int x;
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof org.opencv.core.Point)) return false;
        org.opencv.core.Point it = (org.opencv.core.Point) obj;
        return x == it.x && y == it.y;
    }

    @Override
    public String toString() {
        return "{" + x + ", " + y + "}";
    }
}
