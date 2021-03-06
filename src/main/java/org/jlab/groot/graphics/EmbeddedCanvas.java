/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.groot.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jlab.groot.base.PadMargins;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.FunctionFactory;

/**
 *
 * @author gavalian
 */
public class EmbeddedCanvas extends JPanel implements MouseMotionListener,MouseListener {
    
    private Timer        updateTimer = null;
    private Long numberOfPaints  = (long) 0;
    private Long paintingTime    = (long) 0;
    
    private List<EmbeddedPad>    canvasPads  = new ArrayList<EmbeddedPad>();
    private int                  ec_COLUMNS  = 1;
    private int                  ec_ROWS     = 1;
    private PadMargins           canvasPadding = new PadMargins();
    private int                  activePad     = 0; 
    
    public EmbeddedCanvas(){
        super();
        //this.setSize(500, 400);
        this.setPreferredSize(new Dimension(500,400));        
        canvasPads.add(new EmbeddedPad());
        this.divide(1, 1);
        this.initMouse();
    }
    
    public EmbeddedCanvas(EmbeddedPad pad){
        this.setPreferredSize(new Dimension(500,400));
        
    }
    
    public final void initMouse(){
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
    }
    
    public final void divide(int columns, int rows){
        canvasPads.clear();
        ec_COLUMNS = columns;
        ec_ROWS    = rows;
        for(int i = 0; i < columns*rows; i++){
            canvasPads.add(new EmbeddedPad());
        }
        activePad = 0;
    }
    
    public void cd(int pad){
        if(pad<0){
            activePad = 0;
        } else if (pad>=this.canvasPads.size()) {
            activePad = 0;
        } else {
            activePad = pad;
        }         
    }
    
    
    public void draw(IDataSet ds){
        draw(ds,"");
    }
    
    public void draw(IDataSet ds, String options){
        this.getPad(activePad).draw(ds, options);
    }
    
    private void updateCanvasPads(int w, int h){
        int pcounter = 0;
        int startX   = 5;
        int minY     = 5;
        int rW       = w - startX;
        int rH       = h - minY;
        
        for(int ir = 0; ir < ec_ROWS; ir++){
            for(int ic = 0; ic < ec_COLUMNS; ic++){
                double x   = ic * (rW/((double) ec_COLUMNS ));
                double xe  = (ic+1) * (rW/((double) ec_COLUMNS ));
                double y   = ir * ( rH/((double) ec_ROWS) );
                double ye  = (ir+1) * ( rH/((double) ec_ROWS));
                //System.out.println("PAD " + pcounter + " " + x + " " + xe );
                canvasPads.get(pcounter).setDimension((int) x + startX, (int) y - minY,
                        (int) (xe-x), (int) (ye-y));

                pcounter++;
            }
        }
    }
    
    /**
     * painting all components on the Graphics2D object.
     * @param g 
     */
    @Override
    public void paint(Graphics g){ 
        try {
            Long st = System.currentTimeMillis();
            Graphics2D g2d = (Graphics2D) g;
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = this.getSize().width;
            int h = this.getSize().height;
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, w, h);
            updateCanvasPads(w,h);
            
            PadMargins  margins = new PadMargins();
            
            for(int i = 0; i < canvasPads.size(); i ++){
                EmbeddedPad pad = canvasPads.get(i);
                pad.getAxisFrame().updateMargins(g2d);
                //pad.getAxisFrame().setAxisMargins(pad.getAxisFrame().getFrameMargins());
                margins.marginFit(pad.getAxisFrame().getFrameMargins());
            }
            
            for(int i = 0; i < canvasPads.size(); i ++){
                EmbeddedPad pad = canvasPads.get(i);            
                //pad.setDimension(0, 0, w, h);                        
                //System.out.println("PAD " + i + " " + pad.getAxisFrame().getFrameMargins());
                //pad.getAxisFrame().setAxisMargins(pad.getAxisFrame().getFrameMargins());                
                //System.out.println(pad.getAxisFrame().getFrameMargins());
                pad.getAxisFrame().setAxisMargins(margins);
                pad.setMargins(margins);
                pad.draw(g2d);
            }
            
            Long et = System.currentTimeMillis();
            paintingTime += (et-st);
            numberOfPaints++;
        } catch(Exception e){
            System.out.println("[EmbeddedCanvas] ---> ooops");
        }
    }
        public EmbeddedPad  getPad(int index){
        return this.canvasPads.get(index);
    }
        
    public void update(){
        this.repaint();       
        //System.out.println(this.getBenchmarkString());
    }
    
    public String getBenchmarkString(){
        StringBuilder str = new StringBuilder();
        double time = (double) paintingTime;
        
        double ms =  (time/numberOfPaints);
        if(numberOfPaints==0) ms = 1000.0;
        
        str.append(String.format("Time = %.2f ms Total Time = %d , Events = %d",
                ms,paintingTime, numberOfPaints));
        return str.toString();
    }
    
    public void setAxisFontSize(int size){
        for(EmbeddedPad pad : canvasPads){
            pad.setAxisFontSize(size);
        }
    }
    
    public void  initTimer(int interval){
        System.out.println("[EmbeddedCanvas] ---->  starting an update timer.");
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                update();
                /*for(int i = 0; i < canvasPads.size();i++){
                    System.out.println("PAD = " + i);
                    canvasPads.get(i).show();
                }*/
            }
        };
        updateTimer = new Timer("EmbeddeCanvasTimer");
        updateTimer.scheduleAtFixedRate(timerTask, 30, interval);
        this.paintingTime   = 0L;
        this.numberOfPaints = 0L;
    }
    
    public int getPadByXY(int x, int y){
        int  rowSize = (int) this.getHeight()/this.ec_ROWS;
        int  row = (int) (y/rowSize);
        int  colSize = (int) this.getWidth()/this.ec_COLUMNS;
        int  col = (int) (x/colSize);
        return row*ec_ROWS + col;
    }

    public void draw(DataGroup group){
        int nrows = group.getRows();
        int ncols = group.getColumns();
        this.divide(ncols, nrows);
        
        int nds   = nrows*ncols;
        for(int i = 0; i < nds; i++){
            List<IDataSet> dsList = group.getData(i);
            this.cd(i);
            for(IDataSet ds : dsList){
                this.draw(ds, "same");
            }
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int pad = this.getPadByXY(e.getX(),e.getY());
        //System.out.println("you're hovering over pad = " + pad);
    }
    @Override
    public void mouseClicked(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        if(e.getClickCount()==2){
            int pad = this.getPadByXY(e.getX(),e.getY());
            System.out.println("you double clicked on " + pad);
            JDialog  dialogWin = new JDialog();            
            dialogWin.setContentPane(new EmbeddedCanvas());
            dialogWin.setSize(400, 400);
            dialogWin.setVisible(true);
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public static void main(String[] args){
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        canvas.divide(2, 2);
        canvas.setAxisFontSize(14);
        //canvas.getPad(0).getAxisFrame().getAxisX().setAxisFontSize(18);
        //canvas.getPad(1).getAxisFrame().getAxisY().setAxisFontSize(18);
        //canvas.getPad(0).getAxisFrame().setDrawAxisZ(true);
        
        H1F h1  = FunctionFactory.createDebugH1F(6);
        H1F h2  = FunctionFactory.randomGausian(100, 0.4, 5.6, 200000, 2.3, 0.8);
        H1F h2b = FunctionFactory.randomGausian(100, 0.4, 5.6, 80000, 4.0, 0.8);
        H2F h2d = FunctionFactory.randomGausian2D(40, 0.4, 5.6, 800000, 2.3, 0.8);
        
        DataGroup group = new DataGroup(2,1);
        h2b.setName("h2b");
        GraphErrors hprofile = h2d.getProfileX();
        group.addDataSet(h2d, 0);
        group.addDataSet(hprofile, 1);
        canvas.draw(group);
        /*for(int i =0; i < 4; i++){
            canvas.cd(i);
            canvas.draw(h2);
        }*/
        frame.add(canvas);
        frame.pack();
        frame.setVisible(true);
        
    }

   
}
