package fr.umlv.hexalib;

import fr.umlv.hexalib.MainWindow.Brush;

public class MainTest {
	public static void main(String[] args) {
		MainWindow window = new MainWindow("Hexalib",600,600);
		// drawing a hexagon
		window.drawHexagon(300,300,50, Brush.BLUE.asOpaque());	
	}
}