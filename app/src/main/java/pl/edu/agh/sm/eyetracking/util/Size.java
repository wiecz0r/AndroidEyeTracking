package pl.edu.agh.sm.eyetracking.util;

public class Size {
    public final int width;
    public final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public org.opencv.core.Size toOpenCV() {
        return new org.opencv.core.Size(width, height);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(height);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(width);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof org.opencv.core.Size)) return false;
        org.opencv.core.Size it = (org.opencv.core.Size) obj;
        return width == it.width && height == it.height;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }
}
