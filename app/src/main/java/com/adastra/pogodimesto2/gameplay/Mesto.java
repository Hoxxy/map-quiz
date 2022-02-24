package com.adastra.pogodimesto2.gameplay;


public class Mesto {
    public static int mapWidth;
    public static int mapHeight;

    public int     x;
    public int     y;
    public String  name;

    public Mesto(String name, double x_cof, double y_cof) {
        this.name  = name;

        this.x = (int) (x_cof*Mesto.mapWidth);
        this.y = (int) (y_cof*Mesto.mapHeight);
    }

    public Mesto(int w, int h) {
        Mesto.mapWidth  = w;
        Mesto.mapHeight = h;
    }
}