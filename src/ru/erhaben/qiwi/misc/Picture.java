package ru.erhaben.qiwi.misc;

import java.awt.Graphics;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.awt.Image;

public class Picture
{
    public static ImageIcon resizeIcon(ImageIcon icon, int height, int width)
    {
        Image img = icon.getImage();
        
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics g = bi.createGraphics();
        g.drawImage(img, 0, 0, width, height, null);
        
        return new ImageIcon(bi);
    }
    
    public static ImageIcon makeTransparent(ImageIcon icon)
    {   
        Image img = icon.getImage();
        
        BufferedImage bi = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);
        
        Graphics g = bi.createGraphics();
        g.drawImage(img, 0, 0, 25, 25, null);
        
        return new ImageIcon(bi);
    }
    
    public static Image resizeImage(Image image, int height, int width){
        BufferedImage bi = new BufferedImage(height, width, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        g.drawImage(image, 0, 0, 25, 25, null);
        
        ImageIcon icon = new ImageIcon(bi);
        return icon.getImage();
    }
}
