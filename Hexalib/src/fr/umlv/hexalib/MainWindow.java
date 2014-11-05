package fr.umlv.hexalib;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JComponent;
import javax.swing.JFrame;


/**
 *  This class represents rectangular area onto which an application
 *  can draw simple shapes and can listen mouse events.
 *  
 *  The method {@link #clear(Brush)} can be used to
 *  set a background color.
 *  
 *  The methods {@link #drawLine(int, int, int, int, Brush)},
 *  {@link #drawRectangle(int, int, int, int, Brush)} and 
 *  {@link #drawEllipse(int, int, int, int, Brush)} can be used to draw shapes on the canvas.
 *  
 *  The mouse events can be retrieved using {@link #waitForMouseEvents(MouseCallback)}. 
 *  
 *  All these methods are thread safe.
 */
public class MainWindow {
    private final JComponent area;
    private final BufferedImage buffer;
    final LinkedBlockingQueue<MouseEvent> eventBlockingQueue =
        new LinkedBlockingQueue<>();
    
    /**
     * Create a window of size width x height and a title. 
     * 
     * @param title the title of the window.
     * @param width the width of the window. 
     * @param height the height of the window.
     */
    public MainWindow(String title, int width, int height) {
      // This should be a factory method and not a constructor
      // but given that this library can be used
      // before static factory have been introduced
      // it's simpler from the user point of view
      // to create a canvas area using new.
      
      BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      @SuppressWarnings("serial")
      
      JComponent area = new JComponent() {
        @Override
        protected void paintComponent(Graphics g) {
          g.drawImage(buffer, 0, 0, null);
        }
        
        @Override
        public Dimension getPreferredSize() {
          return new Dimension(width, height);
        }
      };
     
      class MouseManager extends MouseAdapter implements MouseMotionListener {
        @Override
        public void mouseClicked(MouseEvent event) {
          try {
            eventBlockingQueue.put(event);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
        @Override
        public void mouseMoved(MouseEvent e) {
          // do nothing
        }
        @Override
        public void mouseDragged(MouseEvent e) {
          mouseClicked(e);
        }
      }
      MouseManager mouseManager = new MouseManager();
      area.addMouseListener(mouseManager);
      area.addMouseMotionListener(mouseManager);
      try {
        EventQueue.invokeAndWait(() -> {
          JFrame frame = new JFrame(title);
          area.setOpaque(true);
          frame.setContentPane(area);
          frame.setResizable(false);
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          frame.pack();
          frame.setVisible(true);
        });
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch(InvocationTargetException e) {
        throw new IllegalStateException(e.getCause());
      }
      this.buffer = buffer;
      this.area = area;
    }
    
    /**
     * Clear the drawing area with the color of the brush.
     */
    public void clear(Brush brush) {
      post(graphics -> {
        brush.applyColor(graphics);
        graphics.fillRect(0, 0, area.getWidth(), area.getHeight());
      });
    }

    /**
     * Draw a hexagon using the coordinate of a upper point and a width and an height
     * of the rectangle containing the ellipse.
     * 
     * @param x the x coordinate of the upper left of the rectangle.
     * @param y the y coordinate of the upper left of the rectangle.
     * @param width the width of the rectangle.
     * @param height the height of the rectangle.
     * @param brush if the brush is opaque, the ellipse will be filled
     *              by the color of the brush otherwise only the contour will
     *              be drawn.
     */   
    public void drawHexagon(int x, int y, int r, Brush brush) {
        post(graphics -> {
          brush.applyColor(graphics);
          Polygon p = new Polygon();
          for (int i = 0; i < 6; i++) {
        	  p.addPoint((int)(x + r*Math.cos(i*2*Math.PI/6)), (int)(y + r*Math.sin(i*2*Math.PI/6)));
          }
          if (brush.isOpaque()) {
        	  graphics.fillPolygon(p);
          }else {
        	  graphics.drawPolygon(p); 
          }
          
        });
    }
    
    /**
     * A brush is a color and a style of painting (opaque or not).
     * A brush that is opaque will fill the content of the drawing.
     */
    public static class Brush {
      private final Color color;
      // corresponding opaque brush, null if the corresponding opaque brush is not computed yet
      private Brush opaqueBrush;
      
      private Brush(Color color, boolean opaque) {
        this.color = color;
        this.opaqueBrush = (opaque)? this: null;
      }

      private Brush(int red, int green, int blue, boolean opaque) {
        this(new Color(
                checkColorComponent(red),
                checkColorComponent(green),
                checkColorComponent(blue)),
             opaque);
      }

      private static int checkColorComponent(int component) {
        if (component < 0 || component > 255) {
          throw new IllegalArgumentException("bad component value " + component);
        }
        return component;
      }
      
      /**
       * Creates a new Brush representing a color.
       * 
       * @param red red component of the color of the brush.
       *        the value must be between 0 and 255.
       * @param green green component of the color of the brush.
       *        the value must be between 0 and 255.
       * @param blue blue component of the color of the brush.
       *        the value must be between 0 and 255.
       */
      public Brush(int red, int green, int blue) {
        this(red, green, blue, false);
      }
      
      void applyColor(Graphics2D graphics) {
        graphics.setColor(color);
      }
      
      /**
       * Returns true if the current brush is opaque.
       * @return true if the current brush is opaque.
       */
      public boolean isOpaque() {
        return opaqueBrush == this;
      }
      
      /**
       * Returns a brush with the same color components and a style opaque.
       * @return a brush with the same color components and a style opaque.
       */
      public Brush asOpaque() {
        if (opaqueBrush != null) {
          return opaqueBrush;
        }
        return opaqueBrush = new Brush(color, true);
      }

      /**
       * The brush corresponding to the color red.
       */
      public static final Brush RED = new Brush(255, 0, 0);
      
      /**
       * The brush corresponding to the color green.
       */
      public static final Brush GREEN = new Brush(0, 255, 0);
      
      /**
       * The brush corresponding to the color blue.
       */
      public static final Brush BLUE = new Brush(0, 0, 244);
      
      /**
       * The brush corresponding to the color light gray.
       */
      public static final Brush LIGHT_GRAY = new Brush(192, 192, 192);
      
      /**
       * The brush corresponding to the color gray.
       */
      public static final Brush GRAY = new Brush(128, 128, 128);
      
      /**
       * The brush corresponding to the color drak gray.
       */
      public static final Brush DARK_GRAY = new Brush(64, 64, 64);
      
      /**
       * The brush corresponding to the color black.
       */
      public static final Brush BLACK = new Brush(0, 0, 0);
      
      /**
       * The brush corresponding to the color pink.
       */
      public static final Brush PINK = new Brush(255, 175, 175);
      
      /**
       * The brush corresponding to the color orange.
       */
      public static final Brush ORANGE = new Brush(255, 200, 0);
      
      /**
       * The brush corresponding to the color yellow.
       */
      public static final Brush YELLOW = new Brush(255, 255, 0);
      
      /**
       * The brush corresponding to the color magenta.
       */
      public static final Brush MAGENTA = new Brush(255, 0, 255);
      
      /**
       * The brush corresponding to the color cyan.
       */
      public static final Brush CYAN = new Brush(0, 255, 255);
    }
    
    
    @FunctionalInterface
    interface Painter {
      public void paint(Graphics2D graphics);
    }
    
    private void post(Painter painter) {
      EventQueue.invokeLater(() -> {
        Graphics2D graphics = buffer.createGraphics();
        try {
          graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          painter.paint(graphics);
          area.repaint();  // repost a paint event to avoid blinking
        } finally {
          graphics.dispose();
        }
      });
    }

}