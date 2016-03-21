/*
 * EditorView.java
 */

package editor;

/*import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;*/
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
/*import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
*/

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

//////////////////////////////////////////////////////////////////////

class Point{
    public int x = 0, y = 0;
    public Point(){}
    public Point(int x, int y){ this.x = x; this.y = y; }
};

class Vector{
    public float x, y;
    public Vector(float x, float y){ this.x = x; this.y = y; }
    public Vector(Point a, Point b){ this.x = b.x-a.x; this.y = b.y-a.y; }
    public Vector(Point pt){ this.x = pt.x; this.y = pt.y; }

    public float length(){ return (float)Math.sqrt(x*x + y*y); }
    public float dot(Vector v){ return x*v.x + y*v.y; }

    public Vector normalized(){
        float inv = 1.0f/(length() + 1e-6f); // no division by zero!
        return new Vector(x*inv, y*inv);
    }
};

class Line{
    public Point a = new Point();
    public Point b = new Point();
    
    public Line(){ }
    public Line(Point p0, Point p1){ a = p0; b = p1; }
    
    void Draw(Graphics2D g){
        g.setStroke(new BasicStroke(3));
        g.drawLine(a.x, a.y, b.x, b.y);
        g.setStroke(new BasicStroke(1));
    }
};

class Pickup{
    private String m_type = "";
    private int m_amount = 0; // like bullets and etc.
    private Point m_pos = new Point();

    public Pickup(Point pos, String type, int count){ m_pos = pos; m_type = type; m_amount = count; }

    public String GetType(){ return m_type; }
    public int GetAmount(){ return m_amount; }
    public Point GetPos(){ return m_pos; }
};

//////////////////////////////////////////////////////////////////////

//
// custom panel to repaint the panel
//
class MyPanel extends JPanel{

    private EditorView m_editor;

    public MyPanel(){ super(); }

    // Set callback
    public void SetParent(EditorView editor){
        m_editor = editor;
    }

    // Repaint the editor window
    public void paintComponent(Graphics g)
    {
       super.paintComponent(g);
       m_editor.DrawMap((Graphics2D)g);
    }
};

/**
 * The application's main frame.
 */
public class EditorView extends FrameView {

    final int editorVersion = 1000;
    final int grid_step = 16; // grid size, TODO: can be made change'able
    final int snapThreshold = 8;  // in pixels
    final int pickupSize = 12; // health/ammo/etc. rectangle
    final float minEraseDist = 10; // in pixels
    private String savePath;

    public EditorView(SingleFrameApplication app) {
        super(app);
        initComponents();

        this.getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.m_mainWindow = app.getMainFrame();
        this.m_app = app;

        // set default text to resize label
        statusMessageLabel.setText("Select operation from toolbar");
        
        //
        //      ??? Net Beans fails to find resource
        //                  declared name manualy
        optionsMenu.setText("Options");
        jButton6.setText("Node");

        node = new Point( -1, -1 );

        this.SelectMapSize();
    }

    //
    // shows a selection box to select map size, exits if the user cancels
    //
    private void SelectMapSize()
    {
        String[] opt_list = { // option list
            new String("600x500"),
            new String("400x400")
        };

        String ret = (String)JOptionPane.showInputDialog(
                this.m_mainWindow, new String("Select map size:"),
                "Map size",
                JOptionPane.INFORMATION_MESSAGE,
                null,
                opt_list,
                opt_list[0]); // default opt

        if(ret == null)
            this.m_app.exit();

        // resize the map
        String[] dims = ret.split("x");
        this.SetMapDims(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
    }

    //
    // Resizes the panel to fit the map size
    //
    private void SetMapDims(int width, int height)
    {
        System.out.println("setting map dims to "+width+"x"+height);
        this.jPanel1.setPreferredSize(new Dimension(width, height));
        this.mainPanel.setBackground(Color.LIGHT_GRAY);
        this.jPanel1.setBackground(Color.WHITE);
        this.redraw_graphics();

        n_mapWidth = width;
        n_mapHeight = height;
    }

    //
    // Finds the closest object to mouse pointer & erases it
    //
    private boolean EraseObject(int mouse_x, int mouse_y)
    {
        if(m_items.isEmpty() && m_lines.isEmpty()) 
            return false;
        
        // find closest pickup
        Pickup pickUp = null;
        float dist = 0;

        for(int i=0; i<m_items.size(); i++){
            int dx = mouse_x - m_items.get(i).GetPos().x;
            int dy = mouse_y - m_items.get(i).GetPos().y;
            float d = (float)Math.sqrt(dx*dx + dy*dy);
            if(d < dist || pickUp == null){
                if(d > minEraseDist) continue;
                pickUp = m_items.get(i);
                dist = d;
            }
        }

        // find closest line
        Point mouse = new Point(mouse_x, mouse_y);
        Line kill_line = null;
        float line_dist = 0;

        for(int i=0; i<m_lines.size(); i++){
            Line p = m_lines.get(i);

            // get normalized line vector
            Vector line_v = new Vector(p.a, p.b);
            float len = line_v.length();
            line_v = line_v.normalized();

            // vector to mouse point
            Vector mouse_v = new Vector(p.a, mouse);
            Vector pt = null; // point on line

            // find closest point on line from mouse point(project)
            float proj = line_v.dot(mouse_v);

            if(proj <= 0.0f)
                pt = new Vector(p.a);
            else if(proj >= len)
                pt = new Vector(p.b);
            else 
                pt = new Vector(
                        p.a.x + line_v.x * proj,
                        p.a.y + line_v.y * proj);

            Vector dir = new Vector(pt.x - mouse.x, pt.y - mouse.y);
            float d = dir.length();

            if(d < line_dist || kill_line == null)
            {
                if(d > minEraseDist) continue;
                kill_line = p;
                line_dist = d;
            }
        }

        // now erase the object
        if(kill_line != null && pickUp != null) // found both
        {
            if(dist < line_dist) // pickup is closer
                m_items.remove(pickUp);
            else // line is closer
                m_lines.remove(kill_line);
            return true;
        }
        else if(kill_line != null)
        {
             m_lines.remove(kill_line);
             return true;
        }
        else if(pickUp != null)
        {
            m_items.remove(pickUp);
            return true;
        }

        return false; // nothing erased
    }

    //
    // initiates a draw call to repaint the map
    //
    public void redraw_graphics()
    {
        MyPanel mp = (MyPanel)this.jPanel1;
        mp.SetParent(this);
        mp.repaint();
    }

    //
    // Panel calls this to repaint itself. DO NO CALL DIRECTLY.
    //
    public void DrawMap(Graphics2D g)
    {
        g.setRenderingHint (RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON);

        int nPaneWidth = this.jPanel1.getWidth();
        int nPaneHeight = this.jPanel1.getHeight();

        // clear screen
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 2000, 2000); // max out clear
        g.setColor(Color.BLACK);

        // draw grid
        g.setColor(Color.LIGHT_GRAY);
        for(int i=0; i<nPaneHeight; i += grid_step)
            g.drawLine(0, i, nPaneWidth, i); // horizontal
        for(int j=0; j<nPaneWidth; j += grid_step)
            g.drawLine(j, 0, j, nPaneHeight); // vertical
        g.setColor(Color.BLACK);

        // draw all lines
        for(int i=0; i<m_lines.size(); i++)
            m_lines.get(i).Draw(g);

        // draw Pickup's
        for ( int l = 0; l < m_items.size(); l++ )  {
            int x = m_items.get(l).GetPos().x;
            int y = m_items.get(l).GetPos().y;
            g.drawRect(x-pickupSize/2, y-pickupSize/2,
                    pickupSize, pickupSize);

            String puType = m_items.get(l).GetType();
            if ( puType.compareTo("Health") == 0 ){
                g.setColor(Color.RED);
                g.drawLine(x, y-4, x, y+4 );
                g.drawLine(x-4, y, x+4, y );
                g.drawString( Integer.toString(m_items.get(l).GetAmount()),
                        x+pickupSize/2+2, y+pickupSize/2+2);
                g.setColor(Color.BLACK);
            }else if( puType.compareTo("Ammo") == 0 ){
                g.drawOval(x-pickupSize/4, y-pickupSize/4, pickupSize/2, pickupSize/2);
                g.setColor(Color.RED);
                g.drawString( Integer.toString(m_items.get(l).GetAmount()),
                        x+pickupSize/2+2, y+pickupSize/2+2);
                g.setColor(Color.BLACK);
            }

        }
        // draw node
        if (  b_node_is )  {
            g.setColor(Color.RED);
            int node_width = 10;
            int node_height = 10;
            g.drawOval( node.x - node_width/2 , node.y - node_height/2 , node_width, node_height );
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jPanel1 = new MyPanel();
        jToolBar1 = new javax.swing.JToolBar();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        jButton6 = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        optionsMenu = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                mainPanelMouseWheelMoved(evt);
            }
        });

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(editor.EditorApp.class).getContext().getResourceMap(EditorView.class);
        jPanel1.setBackground(resourceMap.getColor("canvas.background")); // NOI18N
        jPanel1.setName("canvas"); // NOI18N
        jPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jPanel1MousePressed(evt);
            }
        });
        jPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanel1MouseMoved(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 139, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 118, Short.MAX_VALUE)
        );

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jToolBar1.setName("Toolbar"); // NOI18N

        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setName("jButton2"); // NOI18N
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton2);

        jButton3.setText(resourceMap.getString("jButton3.text")); // NOI18N
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setName("jButton3"); // NOI18N
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton3);

        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setName("jButton1"); // NOI18N
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        jSeparator1.setName("jSeparator1"); // NOI18N
        jToolBar1.add(jSeparator1);

        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
        jButton4.setFocusable(false);
        jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton4.setName("jButton4"); // NOI18N
        jButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton4);

        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N
        jButton5.setFocusable(false);
        jButton5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton5.setName("jButton5"); // NOI18N
        jButton5.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton5);

        jSeparator3.setName("jSeparator3"); // NOI18N
        jToolBar1.add(jSeparator3);

        jButton6.setText(resourceMap.getString("jButton6.text")); // NOI18N
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton6.setName("jButton6"); // NOI18N
        jButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton6);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 510, Short.MAX_VALUE)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(231, Short.MAX_VALUE))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem2);

        jSeparator2.setName("jSeparator2"); // NOI18N
        fileMenu.add(jSeparator2);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(editor.EditorApp.class).getContext().getActionMap(EditorView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        optionsMenu.setText(resourceMap.getString("optionsMenu.text")); // NOI18N
        optionsMenu.setName("optionsMenu"); // NOI18N
        optionsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionsMenuActionPerformed(evt);
            }
        });

        jMenuItem3.setText(resourceMap.getString("jMenuItem3.text")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        optionsMenu.add(jMenuItem3);

        menuBar.add(optionsMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 510, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 490, Short.MAX_VALUE)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    //
    // select to draw a wall
    //
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        this.ResetEdit();
        
        m_wallDrawing = true;
        statusMessageLabel.setText("Wall drawing ON, RMB - cancel, Shift - to snap");
        this.redraw_graphics();
        
    }//GEN-LAST:event_jButton2ActionPerformed


    //
    // mouse click - drawing ability control
    //
    private void jPanel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel1MousePressed


        if ( m_wallDrawing )
        {
            if(evt.getButton() == MouseEvent.BUTTON3) // disable wall drawing
            {
                m_wallDrawing = false;
                m_lineStarted = false;
                statusMessageLabel.setText("Wall drawing OFF");

                this.redraw_graphics(); // clears old "ghost" line
            }
            else if(evt.getButton() == MouseEvent.BUTTON1 && m_wallDrawing) // start or end the line
            {
                if(m_lineStarted)// draw line - endpoint clicked
                {
                    Point p =  new Point(evt.getX(), evt.getY());
                    m_lines.add(new Line(m_lineStart, p));
                    m_lineStart = p; // new line begins with other line's end
                    this.redraw_graphics();

                }
                else // start a new line
                {
                    m_lineStart = new Point(evt.getX(), evt.getY());
                    m_lineStarted = true;
                }
            }
        }

        // cancel erase of objects
        if(evt.getButton() == MouseEvent.BUTTON3){
            m_bEraseOn = false;
        }

        // erase the selected object
        if(evt.getButton() == MouseEvent.BUTTON1 && m_bEraseOn){
           if( this.EraseObject(evt.getX(), evt.getY()) == true )
               this.redraw_graphics();
        }
        
        //  cancel item put
        if(evt.getButton() == MouseEvent.BUTTON3){  // disable items drawing
            b_itemDrawing = false;
            statusMessageLabel.setText("Select operation from toolbar");
        }

        // put healt item
        if ( b_itemDrawing && evt.getButton() == MouseEvent.BUTTON1)  {
           // b_healthDrawing = false;
            this.n_prev_items = n_items;
            m_items.add( new Pickup(new Point( evt.getX(),evt.getY() ), s_itemType, n_items ));
            this.redraw_graphics();
            //statusMessageLabel.setText("Select operation from toolbar");
        }

        if ( b_node )  {
            node.x = evt.getX();
            node.y = evt.getY();
            b_node = false;
            b_node_is = true;
            this.redraw_graphics();
            this.statusMessageLabel.setText("Select option from toolbar ");
        }


    }//GEN-LAST:event_jPanel1MousePressed

    //
    // rapainting when mouse moves
    //
    private void jPanel1MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel1MouseMoved
     
        if ( evt.isShiftDown() )
        {
            // grid snap
            try{
                Robot robot = new Robot();

                // getting coordinates for first time or if they was reseted
                if ( local_X == 0 || local_Y == 0 )  {
                    temp_X = evt.getXOnScreen();
                    temp_Y = evt.getYOnScreen();
                    local_X = Math.round( (float) evt.getX() / grid_step ) * grid_step;
                    local_Y = Math.round( (float) evt.getY() / grid_step ) * grid_step;
                    int dx = local_X - evt.getX();
                    int dy = local_Y - evt.getY();
                    temp_X += dx;
                    temp_Y += dy;
                   // robot.mouseMove(temp_X, temp_Y);
                }
                robot.mouseMove(temp_X, temp_Y);
                dif_X += local_X - evt.getX();
                dif_Y += local_Y - evt.getY();


                int snap_iter = snapThreshold*2;


                if ( dif_X >=snap_iter )  { 
                    temp_X -= grid_step;
                    robot.mouseMove(temp_X, temp_Y);
                    local_X -= grid_step;
                    dif_X = 0;
                } else if ( dif_Y >=snap_iter )  {  
                    temp_Y -= grid_step;
                    robot.mouseMove(temp_X, temp_Y);
                    local_Y -= grid_step;
                    dif_Y = 0;
                } else if ( dif_X <=snap_iter*-1 )  { 
                    temp_X += grid_step;
                    robot.mouseMove(temp_X, temp_Y);
                    local_X += grid_step;
                    dif_X = 0;
                } else if ( dif_Y <=snap_iter*-1 )  { 
                    temp_Y += grid_step;
                    robot.mouseMove(temp_X, temp_Y);
                    local_Y += grid_step;
                    dif_Y = 0;
                }

                }
                catch  (Exception e )  {
                    ShowMessage("ShowMessage handling mouse input.");
                }
        }
        if ( !evt.isShiftDown() )
            local_X = 0;


        if(m_lineStarted)
        {
            this.redraw_graphics();
            
            // draw current line
            Line current = new Line(m_lineStart, new Point(evt.getX(), evt.getY()));
            current.Draw((Graphics2D)this.jPanel1.getGraphics());
        }


    }//GEN-LAST:event_jPanel1MouseMoved

    //
    // Turns off all editing (placing walls, items, erasing, etc.)
    //
    private void ResetEdit()
    {
        m_wallDrawing = false;
        b_itemDrawing = false;
        m_lineStarted = false;
        m_bEraseOn = false;
    }

    //
    // Resets the editor state
    //
    private void Reset()
    {
        m_lines.clear();
        m_items.clear();
        b_node_is = false;
        this.ResetEdit();
        n_prev_items = 0;
        this.redraw_graphics();
    }

    //
    // Quick ShowMessage message implementation
    //
    private void ShowMessage(String msg)
    {
        JOptionPane.showMessageDialog(this.getComponent(), msg);
    }

    //
    // Clear all - button pressed
    //
    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        this.Reset();
    }//GEN-LAST:event_jButton4ActionPerformed

    //
    // start Drawing health
    //
    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed

        this.ResetEdit();
        
        b_itemDrawing = true;
        s_itemType = "Health";
        n_items = n_prev_items;
        statusMessageLabel.setText("LMB to put "+s_itemType+" item | Mouse Wheel +- | Current "+
                Integer.toString(n_items)+" | RMB - cancel");

    }//GEN-LAST:event_jButton3ActionPerformed

    //
    // Updates pickup item count when scrolling the mouse wheel
    //
    private void mainPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_mainPanelMouseWheelMoved
        int n_healthIncrease = 5;

        if ( b_itemDrawing )  {
            int rotation = evt.getWheelRotation();
            if ( rotation < 0 )
                n_items += n_healthIncrease;
            else
                n_items -= n_healthIncrease;

            // check if it is not over limits
            //               0 <= x <= 100
            if ( n_items < 0 )
                n_items = 0;
            if ( n_items > 100 )
                n_items = 100;
            // print Status Message
            statusMessageLabel.setText("LMB to put "+s_itemType+" item | Mouse Wheel +-"+
                    Integer.toString(n_healthIncrease)+" | Current "+
                    Integer.toString(n_items)+" | RMB - cancel");
        }
    }//GEN-LAST:event_mainPanelMouseWheelMoved

    //
    // Erase button pressed
    //
    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed

        this.ResetEdit();
        m_bEraseOn = true;
        statusMessageLabel.setText("Click on object to erase");

    }//GEN-LAST:event_jButton5ActionPerformed

    private void optionsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionsMenuActionPerformed
        //TODO:
    }//GEN-LAST:event_optionsMenuActionPerformed

    //
    //  Save map
    //
    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        try {
            if ( !b_node_is )  { /* ensure map seed node is placed */
                ShowMessage("Please place map navigation node before saving!");
            }  else  {
                if (savePath == null) {
                    JFileChooser fileChooser = new JFileChooser();
                    if (fileChooser.showSaveDialog(this.m_mainWindow) != JFileChooser.APPROVE_OPTION) {
                        return;
                    }
                    File file = fileChooser.getSelectedFile();
                    savePath = file.getAbsolutePath();
                }

                System.out.println("Saving to file: " + savePath);
                RandomAccessFile raf = new RandomAccessFile(savePath, "rw");

                raf.writeInt(0xC0FFEE); // write magic
                raf.writeInt(editorVersion);
                raf.writeInt(n_mapWidth);
                raf.writeInt(n_mapHeight);

                // node cords
                raf.writeInt( node.x );
                raf.writeInt( node.y );

                raf.writeInt(m_lines.size());
                raf.writeInt(m_items.size());


                for ( int i = 0; i < m_lines.size(); i++ )  {  /* writing lines */
                    raf.writeInt(m_lines.get(i).a.x);
                    raf.writeInt(m_lines.get(i).a.y);
                    raf.writeInt(m_lines.get(i).b.x);
                    raf.writeInt(m_lines.get(i).b.y);
                }

                for ( int i = 0; i< m_items.size(); i++ ) {    /* writing items */
                    Pickup pu = m_items.get(i);
                    raf.writeInt(pu.GetPos().x);
                    raf.writeInt(pu.GetPos().y);
                    int ilgis = pu.GetType().length();
                    char[] tipas = pu.GetType().toCharArray();
                    raf.writeInt(ilgis);
                    for  ( int j =0 ; j < ilgis; j++ )
                        raf.writeChar(tipas[j]);
                    raf.writeInt(pu.GetAmount());
                }

                raf.close();
                ShowMessage("Map saved to file: " + savePath);
            }

        } catch( Exception e) {
            ShowMessage("Unable to save map: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this.m_mainWindow) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            
            File file = fileChooser.getSelectedFile();
            String openPath = file.getAbsolutePath();
            System.out.println("Opening file: " + openPath);

            RandomAccessFile raf = new RandomAccessFile(openPath, "rw");

            int magic = raf.readInt();
            int ver = raf.readInt();

            if ( magic == 0xC0FFEE && ver >= editorVersion)  { // check magic

                this.Reset();

                int n_width = raf.readInt();
                int n_height = raf.readInt();
                node.x = raf.readInt();
                node.y = raf.readInt();
                b_node_is = true;
                int lines = raf.readInt();
                int items = raf.readInt();

                for (int i = 0; i <  lines ; i++ )  { /* reading lines */
                    m_lines.add(new Line( new Point(raf.readInt(), raf.readInt()), new Point(raf.readInt(), raf.readInt())  ));
                }
                for ( int i = 0; i< items; i++ )  { /* reading items */
                    int x = raf.readInt();
                    int y = raf.readInt();
                    int n_t = raf.readInt();
                    char[] tipas = new char[n_t];
                    for ( int j = 0; j < n_t; j++ )
                        tipas[j] = raf.readChar();
                    int ammount = raf.readInt();

                    String eil = "";
                    for ( int h = 0; h< tipas.length; h++ )
                        eil +=tipas[h];

                    m_items.add(new Pickup(new Point(x, y), eil, ammount ));
                    redraw_graphics();
                }

                this.SetMapDims(n_width, n_height);
            }else{
                ShowMessage("Invalid map file or unsupported version.");
            }
            
            raf.close();
        } catch (Exception e) {
            ShowMessage("Unable to open file: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    //
    // Start to place some ammo...
    //
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        this.ResetEdit();

        b_itemDrawing = true;
        s_itemType = "Ammo";
        n_items = n_prev_items;
        statusMessageLabel.setText("LMB to put "+s_itemType+" item | Mouse Wheel +- | Current "+
                Integer.toString(n_items)+" | RMB - cancel");
    }//GEN-LAST:event_jButton1ActionPerformed

    //
    //   place node
    //
    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        statusMessageLabel.setText("Click on map, to (re)place node");
        b_node = true;
    }//GEN-LAST:event_jButton6ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu optionsMenu;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

 
    private JFrame m_mainWindow = null;
    private SingleFrameApplication m_app = null;
    private boolean m_wallDrawing = false;
    private boolean m_lineStarted = false;
    private boolean b_itemDrawing = false;
    private boolean b_node = false;
    private boolean b_node_is = false;
    private String s_itemType = "none";
    private boolean m_bEraseOn = false;
    private Point m_lineStart = new Point();
    private int n_mapWidth = 0, n_mapHeight = 0;
    private int n_items = 0;
    private int n_prev_items = 0;
    private Point node;
    private int local_X = 0, local_Y = 0, // local for keeping on grid spot ( used in snap )
                temp_X = 0, temp_Y = 0, // temp for invisible coords ( used in snap )
                dif_X = 0, dif_Y = 0;   // difference between coordinates ( used in snap )

    // objects in map
    private ArrayList<Line> m_lines = new ArrayList<Line>();
    private ArrayList<Pickup> m_items = new ArrayList<Pickup>();

}
